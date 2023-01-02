package com.netease.nim.camellia.redis.proxy.cluster;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.util.ConcurrentHashSet;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2022/9/30
 */
public class DefaultProxyClusterModeProvider implements ProxyClusterModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyClusterModeProvider.class);

    private static final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new DefaultThreadFactory("proxy-cluster-mode-schedule"));

    private static final ThreadPoolExecutor heartbeatExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("proxy-heartbeat-sender"), new ThreadPoolExecutor.AbortPolicy());

    private boolean init = false;

    private ProxyNode current;

    private final ConcurrentHashSet<ProxyNode> initNodes = new ConcurrentHashSet<>();

    private final List<ProxyNodeChangeListener> listenerList = new ArrayList<>();

    private final ConcurrentHashSet<ProxyNode> onlineNodes = new ConcurrentHashSet<>();
    private final ConcurrentHashSet<ProxyNode> pendingNodes = new ConcurrentHashSet<>();
    private final ConcurrentHashMap<ProxyNode, AtomicLong> heartbeatTargetNodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ProxyNode, Long> heartbeatMap = new ConcurrentHashMap<>();

    @Override
    public synchronized void init() {
        if (init) return;
        //初始化
        initConf();
        //定时给所有节点发送心跳
        int intervalSeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.interval.seconds", 5);
        schedule.scheduleAtFixedRate(this::sendHeartbeat, 0, intervalSeconds, TimeUnit.SECONDS);
        //定时校验心跳超时的节点列表，并移除
        schedule.scheduleAtFixedRate(this::checkOnlineNodes, 0, intervalSeconds, TimeUnit.SECONDS);
        init = true;
    }

    private void initConf() {
        String string = ProxyDynamicConf.getString("proxy.cluster.mode.nodes", null);
        if (string == null) {
            throw new IllegalArgumentException("missing 'proxy.cluster.mode.nodes' in camellia-redis-proxy.properties");
        }
        String[] split = string.split(",");
        List<ProxyNode> initNodes = new ArrayList<>();
        for (String str : split) {
            ProxyNode node = ProxyNode.parseString(str);
            if (node == null) continue;
            initNodes.add(node);
        }
        if (initNodes.isEmpty()) {
            throw new IllegalArgumentException("parse 'proxy.cluster.mode.nodes' error");
        }
        this.initNodes.addAll(initNodes);
        //把proxy设置为PENDING状态
        ClusterModeStatus.setStatus(ClusterModeStatus.Status.PENDING);

        for (ProxyNode initNode : initNodes) {
            addHeartbeatTarget(initNode);
        }
        //检查是否所有心跳成功的对象都有回包了
        checkHeartbeatReply();
    }

    private void checkHeartbeatReply() {
        if (ClusterModeStatus.getStatus() != ClusterModeStatus.Status.PENDING) return;
        for (Map.Entry<ProxyNode, AtomicLong> entry : heartbeatTargetNodes.entrySet()) {
            ProxyNode node = entry.getKey();
            AtomicLong failCount = entry.getValue();
            if (failCount.get() == 0) {
                if (!heartbeatMap.containsKey(node)) {
                    logger.warn("proxy check heartbeat reply fail, node = {}", node);
                    //检查失败，则再等一下
                    int delaySeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.init.delay.check.seconds", 10);
                    schedule.schedule(this::checkHeartbeatReply, delaySeconds, TimeUnit.SECONDS);
                    return;
                }
            }
        }
        //如果所有心跳目标对象都有回包了，则把proxy设置为在线状态
        if (ClusterModeStatus.getStatus() == ClusterModeStatus.Status.PENDING) {
            triggerNodeAdd(current());
            ClusterModeStatus.setStatus(ClusterModeStatus.Status.ONLINE);
            logger.info("proxy cluster mode status upgrade {} -> {}", ClusterModeStatus.Status.PENDING, ClusterModeStatus.Status.ONLINE);
        }
    }

    private void checkOnlineNodes() {
        Set<ProxyNode> offlineNodes = new HashSet<>();
        for (ProxyNode node : onlineNodes) {
            if (node.equals(current())) {
                if (ClusterModeStatus.getStatus() != ClusterModeStatus.Status.ONLINE) {
                    offlineNodes.add(node);
                }
                continue;
            }
            Long lastHeartbeatTime = heartbeatMap.get(node);
            int timeoutSeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.timeout.seconds", 20);
            if (lastHeartbeatTime == null || System.currentTimeMillis() - lastHeartbeatTime > timeoutSeconds*1000L) {
                offlineNodes.add(node);
            }
        }
        for (ProxyNode node : offlineNodes) {
            heartbeatMap.remove(node);
            pendingNodes.remove(node);
            triggerNodeRemove(node);
        }
        if (!onlineNodes.contains(current())) {
            if (ClusterModeStatus.getStatus() == ClusterModeStatus.Status.ONLINE) {
                triggerNodeAdd(current());
            }
        }
    }

    private void sendHeartbeat() {
        try {
            Set<ProxyNode> heartbeatNodes = new HashSet<>(onlineNodes);
            heartbeatNodes.addAll(pendingNodes);
            heartbeatNodes.addAll(initNodes);
            int times = ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.request.fail.times", 3);
            heartbeatTargetNodes.entrySet().removeIf(entry -> entry.getValue().get() > times);//心跳连续三次失败，则剔除出去
            heartbeatNodes.addAll(heartbeatTargetNodes.keySet());
            for (ProxyNode node : heartbeatNodes) {
                if (node.equals(current())) continue;//不需要发给自己心跳
                try {
                    heartbeatExecutor.submit(() -> heartbeat(node, ClusterModeStatus.getStatus()));
                } catch (Exception e) {
                    logger.error("submit heartbeat task error, node = {}", node, e);
                }
            }
        } catch (Exception e) {
            logger.error("sendHeartbeat error", e);
        }
    }

    private void heartbeat(ProxyNode node, ClusterModeStatus.Status status) {
        try {
            RedisClient client = RedisClientHub.get(node.getHost(), node.getCport(), null, null);
            if (client != null) {
                CompletableFuture<Reply> future = client.sendCommand(RedisCommand.CLUSTER.raw(),
                        Utils.stringToBytes(RedisKeyword.PROXY_HEARTBEAT.name()), Utils.stringToBytes(current().toString()),
                        Utils.stringToBytes(String.valueOf(status.getValue())));
                int timeoutSeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.request.timeout.seconds", 10);
                Reply reply = future.get(timeoutSeconds, TimeUnit.SECONDS);
                if (reply instanceof BulkReply) {
                    String heartbeatResp = Utils.bytesToString(((BulkReply) reply).getRaw());
                    ProxyClusterModeHeartbeatResp resp = parseHeartbeatResp(heartbeatResp);
                    for (ProxyNode proxyNode : resp.getOnlineNodes()) {
                        addHeartbeatTarget(proxyNode);
                    }
                    for (ProxyNode proxyNode : resp.getPendingNodes()) {
                        addHeartbeatTarget(proxyNode);
                    }
                    addHeartbeatTarget(node);//心跳成功
                    if (logger.isDebugEnabled()) {
                        logger.debug("proxy cluster mode heartbeat success, node = {}, resp = {}", node, heartbeatResp);
                    }
                    return;
                }
            }
            logger.warn("proxy cluster mode heartbeat fail, node = {}", node);
            heartbeatTargetFail(node);//心跳失败
        } catch (Exception e) {
            logger.warn("proxy cluster mode heartbeat error, node = {}, ex = {}", node, e.toString());
            heartbeatTargetFail(node);//心跳失败
        }
    }

    private void addHeartbeatTarget(ProxyNode node) {
        if (node.equals(current())) return;//不需要发给自己心跳
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(heartbeatTargetNodes, node, k -> new AtomicLong(0));
        failCount.set(0);
    }

    private void heartbeatTargetFail(ProxyNode node) {
        if (node.equals(current())) return;
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(heartbeatTargetNodes, node, k -> new AtomicLong(0));
        failCount.incrementAndGet();
    }

    @Override
    public List<ProxyNode> discovery() {
        return new ArrayList<>(onlineNodes);
    }

    @Override
    public ProxyNode current() {
        if (current != null) return current;
        String host = ProxyDynamicConf.getString("proxy.cluster.mode.current.node.host", null);
        if (host != null) {
            ProxyNode current = new ProxyNode();
            current.setHost(host);
            int port = GlobalRedisProxyEnv.port;
            int cport = GlobalRedisProxyEnv.cport;
            if (port == 0 || cport == 0) {
                throw new IllegalStateException("redis proxy not start");
            }
            current.setPort(port);
            current.setCport(cport);
            this.current = current;
        } else {
            this.current = ProxyClusterModeProvider.super.current();
        }
        logger.info("current proxy node = {}", current.toString());
        return current;
    }

    @Override
    public void addNodeChangeListener(ProxyNodeChangeListener listener) {
        synchronized (listenerList) {
            listenerList.add(listener);
        }
    }

    @Override
    public Reply proxyHeartbeat(ProxyHeartbeatRequest request) {
        ProxyNode node = request.getNode();
        ClusterModeStatus.Status status = request.getStatus();
        if (status == ClusterModeStatus.Status.ONLINE || status == ClusterModeStatus.Status.PENDING) {
            heartbeatMap.put(node, System.currentTimeMillis());
        }
        if (status == ClusterModeStatus.Status.OFFLINE) {
            triggerNodeRemove(node);
        }
        if (status == ClusterModeStatus.Status.ONLINE) {
            triggerNodeAdd(node);
        }
        if (status == ClusterModeStatus.Status.PENDING) {
            pendingNodes.add(node);
        }
        return new BulkReply(Utils.stringToBytes(heartbeatResp()));
    }

    private String heartbeatResp() {
        ProxyClusterModeHeartbeatResp resp = new ProxyClusterModeHeartbeatResp();
        resp.setOnlineNodes(new ArrayList<>(onlineNodes));
        resp.setPendingNodes(new ArrayList<>(pendingNodes));
        return JSONObject.toJSONString(resp);
    }

    private ProxyClusterModeHeartbeatResp parseHeartbeatResp(String heartbeatResp) {
        return JSONObject.parseObject(heartbeatResp, ProxyClusterModeHeartbeatResp.class);
    }

    private void triggerNodeRemove(ProxyNode node) {
        if (onlineNodes.contains(node)) {
            logger.info("onlineNodes remove = {}", node);
            onlineNodes.remove(node);
            for (ProxyNodeChangeListener listener : listenerList) {
                try {
                    listener.removeNode(node);
                } catch (Exception e) {
                    logger.error("removeNode callback error, node = {}", node, e);
                }
            }
        }
    }

    private void triggerNodeAdd(ProxyNode node) {
        if (!onlineNodes.contains(node)) {
            onlineNodes.add(node);
            logger.info("onlineNodes add = {}", node);
            for (ProxyNodeChangeListener listener : listenerList) {
                try {
                    listener.addNode(node);
                } catch (Exception e) {
                    logger.error("addNode callback error, node = {}", node, e);
                }
            }
        }
    }
}
