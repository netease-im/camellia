package com.netease.nim.camellia.redis.proxy.cluster;


import com.netease.nim.camellia.redis.proxy.cluster.provider.ProxyClusterModeProvider;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2022/9/29
 */
public class DefaultProxyClusterModeProcessor implements ProxyClusterModeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyClusterModeProcessor.class);

    private static final int executorSize;
    static {
        executorSize = Math.max(4, Math.min(SysUtils.getCpuNum(), 8));
    }

    private static final ThreadPoolExecutor heartbeatExecutor = new ThreadPoolExecutor(executorSize, executorSize,
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("proxy-heartbeat-receiver"), new ThreadPoolExecutor.AbortPolicy());

    private final ReentrantLock initLock = new ReentrantLock();
    private final ReentrantLock refreshLock = new ReentrantLock();

    private final ProxyClusterModeProvider provider;

    private ProxyClusterSlotMap clusterSlotMap;

    private Reply clusterInfo;
    private Reply clusterSlots;
    private Reply clusterNodes;

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private boolean clusterModeCommandMoveEnable;
    private int clusterModeCommandMoveIntervalSeconds;
    private boolean clusterModeCommandMoveAlways;
    private boolean init = false;

    public DefaultProxyClusterModeProcessor(ProxyClusterModeProvider provider) {
        this.provider = provider;
        GlobalRedisProxyEnv.addAfterStartCallback(this::init);
    }

    private void init() {
        initLock.lock();
        try {
            if (init) return;
            provider.init();
            refresh();
            provider.addNodeChangeListener(this::refresh);
            reloadConf();
            ProxyDynamicConf.registerCallback(this::reloadConf);
            int seconds = ProxyDynamicConf.getInt("proxy.cluster.mode.refresh.nodes.interval.seconds", 60);
            ExecutorUtils.scheduleAtFixedRate(this::refresh, seconds, seconds, TimeUnit.SECONDS);
            init = true;
        } finally {
            initLock.unlock();
        }
    }

    private void reloadConf() {
        clusterModeCommandMoveEnable = ProxyDynamicConf.getBoolean("proxy.cluster.mode.command.move.enable", true);
        clusterModeCommandMoveIntervalSeconds = ProxyDynamicConf.getInt("proxy.cluster.mode.command.move.interval.seconds", 30);
        clusterModeCommandMoveAlways = ProxyDynamicConf.getBoolean("proxy.cluster.mode.command.move.always", false);
    }

    private void refresh() {
        refreshLock.lock();
        try {
            if (refreshing.compareAndSet(false, true)) {
                try {
                    ProxyClusterSlotMap clusterSlotMap = this.provider.load();
                    if (clusterSlotMap == null) {
                        logger.error("cluster slot map load null");
                        return;
                    }
                    if (clusterSlotMap.equals(this.clusterSlotMap)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("cluster slot map not modify, skip refresh");
                        }
                        return;
                    }
                    clusterInfo = clusterSlotMap.clusterInfo();
                    clusterSlots = clusterSlotMap.clusterSlots();
                    clusterNodes = clusterSlotMap.clusterNodes();
                    ProxyClusterSlotMap oldSlotMap = this.clusterSlotMap;
                    this.clusterSlotMap = clusterSlotMap;
                    logger.info("refresh proxy cluster mode slot map success, onlineNodes = {}, currentNodeOnline = {}",
                            clusterSlotMap.getOnlineNodes(), clusterSlotMap.isCurrentNodeOnline());
                    logger.info("cluster info refresh, cluster_info: \r\n{}", clusterInfo);
                    logger.info("cluster nodes refresh, cluster_nodes: \r\n{}", clusterNodes);
                    ClusterModeStatus.invokeClusterModeSlotMapChangeCallback(oldSlotMap, clusterSlotMap);
                } finally {
                    refreshing.compareAndSet(true, false);
                }
            }
        } catch (Exception e) {
            logger.error("refresh proxy cluster mode nodes error", e);
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * 命令是否要重定向
     * @param command Command
     * @return reply
     */
    @Override
    public Reply isCommandMove(Command command) {
        try {
            if (!init) return null;
            if (!clusterModeCommandMoveEnable) return null;//不开启move，则直接返回null
            if (clusterModeCommandMoveAlways) {
                List<byte[]> keys = command.getKeys();
                if (keys.isEmpty()) {
                    return null;
                }
                for (byte[] key : keys) {
                    int slot = RedisClusterCRC16Utils.getSlot(key);
                    if (!clusterSlotMap.isSlotInCurrentNode(slot)) {
                        ProxyNode node = clusterSlotMap.getBySlot(slot);
                        return new ErrorReply("MOVED " + slot + " " + node.getHost() + ":" + node.getPort());
                    }
                }
                return null;
            }

            if (clusterSlotMap.isOnlineNodesEmpty()) return null;
            if (refreshing.get()) return null;//正在更新slot信息，则别move了
            ChannelInfo channelInfo = command.getChannelInfo();
            long lastCommandMoveTime = channelInfo.getLastCommandMoveTime();
            if (TimeCache.currentMillis - lastCommandMoveTime <= clusterModeCommandMoveIntervalSeconds * 1000L) {
                //30s内只move一次
                return null;
            }
            if (command.isBlocking()) return null;//blocking的command也别move了吧
            List<byte[]> keys = command.getKeys();
            if (keys.isEmpty()) return null;
            byte[] key = keys.get(0);
            int slot = RedisClusterCRC16Utils.getSlot(key);
            if (!clusterSlotMap.isSlotInCurrentNode(slot)) {
                ProxyNode node = clusterSlotMap.getBySlot(slot);
                channelInfo.setLastCommandMoveTime(TimeCache.currentMillis);//记录一下上一次move的时间
                return new ErrorReply("MOVED " + slot + " " + node.getHost() + ":" + node.getPort());
            }
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(DefaultProxyClusterModeProcessor.class, "is command move error", e);
            return null;
        }
    }

    /**
     * cluster相关命令
     * @param command Command
     * @return reply
     */
    @Override
    public CompletableFuture<Reply> clusterCommands(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand != RedisCommand.CLUSTER) {
            return CompletableFuture.completedFuture(ErrorReply.NOT_SUPPORT);
        }
        byte[][] objects = command.getObjects();
        if (objects.length <= 1) {
            return CompletableFuture.completedFuture(ErrorReply.argNumWrong(redisCommand));
        }
        if (!init) {
            return CompletableFuture.completedFuture(ErrorReply.NOT_AVAILABLE);
        }
        String arg = Utils.bytesToString(objects[1]);
        if (arg.equalsIgnoreCase(RedisKeyword.INFO.name())) {
            return CompletableFuture.completedFuture(clusterInfo == null ? ErrorReply.NOT_AVAILABLE : clusterInfo);
        } else if (arg.equalsIgnoreCase(RedisKeyword.NODES.name())) {
            return CompletableFuture.completedFuture(clusterNodes == null ? ErrorReply.NOT_AVAILABLE : clusterNodes);
        } else if (arg.equalsIgnoreCase(RedisKeyword.SLOTS.name())) {
            return CompletableFuture.completedFuture(clusterSlots == null ? ErrorReply.NOT_AVAILABLE : clusterSlots);
        } else if (arg.equalsIgnoreCase(RedisKeyword.KEYSLOT.name())) {
            if (objects.length != 3) {
                return CompletableFuture.completedFuture(ErrorReply.argNumWrong(RedisCommand.CLUSTER));
            }
            IntegerReply reply = new IntegerReply((long) RedisClusterCRC16Utils.getSlot(objects[2]));
            return CompletableFuture.completedFuture(reply);
        } else if (arg.equalsIgnoreCase(RedisKeyword.PROXY_HEARTBEAT.name())) {//camellia定义的proxy间心跳
            CompletableFuture<Reply> future = new CompletableFuture<>();
            try {
                heartbeatExecutor.submit(() -> {
                    try {
                        Reply reply = provider.proxyHeartbeat(command);
                        future.complete(reply);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(DefaultProxyClusterModeProcessor.class, "proxyHeartbeat error", e);
                        future.complete(ErrorReply.NOT_AVAILABLE);
                    }
                });
            } catch (Exception e) {
                ErrorLogCollector.collect(DefaultProxyClusterModeProcessor.class, "submit proxyHeartbeat task error", e);
                future.complete(ErrorReply.NOT_AVAILABLE);
            }
            return future;
        } else {
            ErrorLogCollector.collect(DefaultProxyClusterModeProcessor.class, "not support cluster command, arg = " + arg);
            return CompletableFuture.completedFuture(ErrorReply.NOT_SUPPORT);
        }
    }

    /**
     * 获取当前节点
     * @return 当前节点
     */
    @Override
    public ProxyNode getCurrentNode() {
        return clusterSlotMap.getCurrentNode();
    }


    /**
     * 获取在线节点列表
     * @return 节点列表
     */
    @Override
    public List<ProxyNode> getOnlineNodes() {
        return clusterSlotMap.getOnlineNodes();
    }
}
