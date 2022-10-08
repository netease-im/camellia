package com.netease.nim.camellia.redis.proxy.cluster;


import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/9/29
 */
public class ProxyClusterModeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyClusterModeProcessor.class);

    private static final ThreadPoolExecutor heartbeatExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("proxy-heartbeat-receiver"), new ThreadPoolExecutor.AbortPolicy());

    private final Object lock = new Object();

    private final ProxyClusterModeProvider provider;

    private List<ProxyNode> onlineNodes = new ArrayList<>();
    private ProxyNode currentNode;
    private int slotStart;
    private int slotEnd;

    private final ConcurrentHashMap<Integer, ProxyNode> slotMap = new ConcurrentHashMap<>();

    private Reply clusterInfo;
    private Reply clusterSlots;
    private Reply clusterNodes;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private boolean clusterModeCommandMoveEnable;
    private int clusterModeCommandIntervalSeconds;

    private boolean init = false;

    private boolean currentNodeOnline = true;

    public ProxyClusterModeProcessor(ProxyClusterModeProvider provider) {
        this.provider = provider;
        GlobalRedisProxyEnv.addStartOkCallback(this::init);
    }

    private synchronized void init() {
        if (init) return;
        provider.init();
        refresh();
        provider.addNodeChangeListener(new ProxyNodeChangeListener() {
            @Override
            public void addNode(ProxyNode node) {
                refresh();
            }

            @Override
            public void removeNode(ProxyNode node) {
                refresh();
            }
        });
        reloadConf();
        ProxyDynamicConf.registerCallback(this::reloadConf);
        int seconds = ProxyDynamicConf.getInt("proxy.cluster.mode.refresh.nodes.interval.seconds", 60);
        ExecutorUtils.scheduleAtFixedRate(this::refresh, seconds, seconds, TimeUnit.SECONDS);
        init = true;
    }

    private void reloadConf() {
        clusterModeCommandMoveEnable = ProxyDynamicConf.getBoolean("proxy.cluster.mode.command.move.enable", true);
        clusterModeCommandIntervalSeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.command.move.interval.seconds", 30);
    }

    private void refresh() {
        synchronized (lock) {
            if (refreshing.compareAndSet(false, true)) {
                try {
                    List<ProxyNode> list = new ArrayList<>(this.provider.discovery());
                    Collections.sort(list);
                    onlineNodes = list;
                    if (onlineNodes.isEmpty()) {
                        logger.warn("refresh proxy cluster mode nodes done, onlineNodes is empty");
                        return;
                    }
                    currentNode = this.provider.current();
                    clusterInfo = initClusterInfo();
                    clusterSlots = initClusterSlots();
                    clusterNodes = initClusterNodes();
                    currentNodeOnline = onlineNodes.contains(currentNode);
                    logger.info("refresh proxy cluster mode nodes success, onlineNodes = {}, currentNodeOnline = {}", onlineNodes, currentNodeOnline);
                } finally {
                    refreshing.compareAndSet(true, false);
                }
            }
        }
    }

    /**
     * 命令是否要重定向
     */
    public Reply isCommandMove(Command command) {
        if (!init) return null;
        if (!clusterModeCommandMoveEnable) return null;//不开启move，则直接返回null
        if (onlineNodes.isEmpty()) return null;
        if (refreshing.get()) return null;//正在更新slot信息，则别move了
        ChannelInfo channelInfo = command.getChannelInfo();
        long lastCommandMoveTime = channelInfo.getLastCommandMoveTime();
        if (TimeCache.currentMillis - lastCommandMoveTime <= clusterModeCommandIntervalSeconds*1000L) {
            //30s内只move一次
            return null;
        }
        if (command.isBlocking()) return null;//blocking的command也别move了吧
        List<byte[]> keys = command.getKeys();
        if (keys.isEmpty()) return null;
        byte[] key = keys.get(0);
        int slot = RedisClusterCRC16Utils.getSlot(key);
        if (!currentNodeOnline || slot < slotStart || slot > slotEnd) {
            ProxyNode node = slotMap.get(slot);
            if (node.equals(currentNode)) return null;
            channelInfo.setLastCommandMoveTime(TimeCache.currentMillis);//记录一下上一次move的时间
            return new ErrorReply("MOVED " + slot + " " + node.getHost() + ":" + node.getPort());
        }
        return null;
    }

    /**
     * cluster相关命令
     */
    public CompletableFuture<Reply> clusterCommands(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand != RedisCommand.CLUSTER) {
            return wrapper(ErrorReply.NOT_SUPPORT);
        }
        byte[][] objects = command.getObjects();
        if (objects.length <= 1) {
            return wrapper(ErrorReply.argNumWrong(redisCommand));
        }
        if (!init) {
            return wrapper(ErrorReply.NOT_AVAILABLE);
        }
        String arg = Utils.bytesToString(objects[1]);
        if (arg.equalsIgnoreCase(RedisKeyword.INFO.name())) {
            return wrapper(clusterInfo == null ? ErrorReply.NOT_AVAILABLE : clusterInfo);
        } else if (arg.equalsIgnoreCase(RedisKeyword.NODES.name())) {
            return wrapper(clusterNodes == null ? ErrorReply.NOT_AVAILABLE : clusterNodes);
        } else if (arg.equalsIgnoreCase(RedisKeyword.SLOTS.name())) {
            return wrapper(clusterSlots == null ? ErrorReply.NOT_AVAILABLE : clusterSlots);
        } else if (arg.equalsIgnoreCase(RedisKeyword.PROXY_HEARTBEAT.name())) {//camellia定义的proxy间心跳
            if (objects.length >= 4) {
                ProxyNode node = ProxyNode.parseString(Utils.bytesToString(objects[2]));
                if (node == null) {
                    return wrapper(ErrorReply.argNumWrong(redisCommand));
                }
                ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue((int) Utils.bytesToNum(objects[3]));
                if (status == null) {
                    return wrapper(ErrorReply.argNumWrong(redisCommand));
                }
                ProxyHeartbeatRequest request = new ProxyHeartbeatRequest();
                request.setNode(node);
                request.setStatus(status);
                CompletableFuture<Reply> future = new CompletableFuture<>();
                try {
                    heartbeatExecutor.submit(() -> {
                        try {
                            Reply reply = provider.proxyHeartbeat(request);
                            future.complete(reply);
                        } catch (Exception e) {
                            ErrorLogCollector.collect(ProxyClusterModeProcessor.class, "proxyHeartbeat error", e);
                            future.complete(ErrorReply.NOT_AVAILABLE);
                        }
                    });
                } catch (Exception e) {
                    ErrorLogCollector.collect(ProxyClusterModeProcessor.class, "submit proxyHeartbeat task error", e);
                    future.complete(ErrorReply.NOT_AVAILABLE);
                }
                return future;
            } else {
                return wrapper(ErrorReply.argNumWrong(redisCommand));
            }
        } else {
            return wrapper(ErrorReply.NOT_SUPPORT);
        }
    }

    private CompletableFuture<Reply> wrapper(Reply reply) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        future.complete(reply);
        return future;
    }

    private Reply initClusterInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("cluster_state:ok").append("\r\n");
        builder.append("cluster_slots_assigned:16384").append("\r\n");
        builder.append("cluster_slots_ok:16384").append("\r\n");
        builder.append("cluster_slots_pfail:0").append("\r\n");
        builder.append("cluster_slots_fail:0").append("\r\n");
        int size = onlineNodes.size();
        builder.append("cluster_known_nodes:").append(size).append("\r\n");
        builder.append("cluster_size:").append(size).append("\r\n");
        builder.append("cluster_current_epoch:").append(size).append("\r\n");
        int myEpoch = 1;
        int i=0;
        for (ProxyNode proxyNode : onlineNodes) {
            i ++;
            if (proxyNode.equals(currentNode)) {
                myEpoch = i;
                break;
            }
        }
        builder.append("cluster_my_epoch:").append(myEpoch).append("\r\n");
        builder.append("cluster_stats_messages_sent:0").append("\r\n");
        builder.append("cluster_stats_messages_received:0").append("\r\n");
        builder.append("total_cluster_links_buffer_limit_exceeded:0").append("\r\n");
        String str = builder.toString();
        logger.info("cluster info refresh, cluster_info: \r\n {}", str);
        return new BulkReply(Utils.stringToBytes(str));
    }

    private Reply initClusterNodes() {
        StringBuilder builder = new StringBuilder();
        int i=0;
        int size = onlineNodes.size();
        int slotsPerNode = 16384 / size;
        long slotCurrent = 0;
        for (ProxyNode proxyNode : onlineNodes) {
            i++;
            builder.append(MD5Util.md5(proxyNode.toString())).append(" ");
            builder.append(proxyNode.getHost()).append(":").append(proxyNode.getPort()).append("@").append(proxyNode.getPort() + 1000).append(" ");
            if (proxyNode.equals(currentNode)) {
                builder.append("myself,master").append(" ");
            } else {
                builder.append("master").append(" ");
            }
            builder.append("-").append(" ");
            builder.append("0").append(" ");
            builder.append(System.currentTimeMillis()).append(" ");
            builder.append(i).append(" ");
            builder.append("connected").append(" ");
            if (i == size) {
                builder.append(slotCurrent).append("-").append(16383);
            } else {
                builder.append(slotCurrent).append("-").append(slotCurrent + slotsPerNode);
            }
            slotCurrent += slotsPerNode;
            slotCurrent ++;
            builder.append("\r\n");
        }
        String str = builder.toString();
        logger.info("cluster nodes refresh, cluster_nodes: \r\n {}", str);
        return new BulkReply(Utils.stringToBytes(str));
    }

    private Reply initClusterSlots() {
        int i=0;
        int size = onlineNodes.size();
        int slotsPerNode = 16384 / size;
        long slotCurrent = 0;
        List<MultiBulkReply> replies = new ArrayList<>();
        for (ProxyNode proxyNode : onlineNodes) {
            i++;
            IntegerReply slotStart = new IntegerReply(slotCurrent);
            IntegerReply slotEnd;
            if (i == size) {
                slotEnd = new IntegerReply(16383L);
            } else {
                slotEnd = new IntegerReply(slotCurrent + slotsPerNode);
            }
            BulkReply host = new BulkReply(Utils.stringToBytes(proxyNode.getHost()));
            IntegerReply port = new IntegerReply((long) proxyNode.getPort());
            BulkReply id = new BulkReply(Utils.stringToBytes(MD5Util.md5(proxyNode.toString())));
            MultiBulkReply master = new MultiBulkReply(new Reply[]{host, port, id});
            replies.add(new MultiBulkReply(new Reply[]{slotStart, slotEnd, master}));
            slotCurrent += slotsPerNode;
            slotCurrent ++;
            if (proxyNode.equals(currentNode)) {
                this.slotStart = Math.toIntExact(slotStart.getInteger());
                this.slotEnd = Math.toIntExact(slotEnd.getInteger());
            }
            for (long slotIndex = slotStart.getInteger(); slotIndex <= slotEnd.getInteger(); slotIndex ++) {
                slotMap.put((int) slotIndex, proxyNode);
            }
        }
        return new MultiBulkReply(replies.toArray(new MultiBulkReply[0]));
    }
}
