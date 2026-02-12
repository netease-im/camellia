package com.netease.nim.camellia.redis.proxy.cluster;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/12/23
 */
public class ClusterModeCommandMoveInvoker {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModeCommandMoveInvoker.class);

    private static final int executorSize;
    static {
        executorSize = Math.max(4, Math.min(SysUtils.getCpuNum(), 8));
    }

    private final ScheduledExecutorService commandMoveExecutor;

    private final Map<ProxyNode, SlotCache> slotCacheMap = new ConcurrentLinkedHashMap.Builder<ProxyNode, SlotCache>()
            .initialCapacity(1024)
            .maximumWeightedCapacity(1024)
            .build();
    private final ReentrantLock[] lockArray = new ReentrantLock[32];

    private long delayMillis;
    private int maxRetry;
    private int cacheMillis;
    private final ClusterModeProcessor processor;

    public ClusterModeCommandMoveInvoker(ClusterModeProcessor processor) {
        this.processor = processor;
        int poolSize = ProxyDynamicConf.getInt("cluster.mode.command.move.graceful.execute.pool.size", executorSize);
        this.commandMoveExecutor = Executors.newScheduledThreadPool(poolSize, new DefaultThreadFactory("command-move-task"));
        for (int i=0; i<32; i++) {
            lockArray[i] = new ReentrantLock();
        }
        updateConf();
        ProxyDynamicConf.registerCallback(this::updateConf);
    }

    private void updateConf() {
        delayMillis = ClusterModeConfig.clusterModeCommandMoveDelayMillis();
        maxRetry = ClusterModeConfig.clusterModeCommandMoveMaxRetry();
        cacheMillis = ClusterModeConfig.clusterModeCommandMoveCacheMillis();
    }

    /**
     * graceful command move
     * @param slot slot
     * @return move reply
     */
    public CompletableFuture<Reply> gracefulCommandMove(int slot) {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        CommandMoveTask task = new CommandMoveTask(processor, delayMillis, maxRetry, slot, future);
        commandMoveExecutor.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        return future;
    }

    private class CommandMoveTask implements Runnable {

        private final ClusterModeProcessor processor;
        private final long delayMillis;
        private final int maxRetry;
        private final CompletableFuture<Reply> future;
        private final int slot;

        private int retry;

        public CommandMoveTask(ClusterModeProcessor processor, long delayMillis, int maxRetry, int slot, CompletableFuture<Reply> future) {
            this.processor = processor;
            this.delayMillis = delayMillis;
            this.maxRetry = maxRetry;
            this.slot = slot;
            this.future = future;
        }

        @Override
        public void run() {
            if (!execute()) {
                commandMoveExecutor.schedule(this, delayMillis, TimeUnit.MILLISECONDS);
            }
        }

        private boolean execute() {
            retry ++;
            ProxyClusterSlotMap clusterSlotMap = processor.getSlotMap();
            ProxyNode node = clusterSlotMap.getBySlot(slot);
            if (retry > maxRetry || checkSlotInProxyNode(clusterSlotMap, node, slot)) {
                Reply reply = new ErrorReply("MOVED " + slot + " " + node.getHost() + ":" + node.getPort());
                future.complete(reply);
                return true;
            } else {
                return false;
            }
        }
    }

    private static class SlotCache {
        long updateTime;
        Set<Integer> slots;

        public SlotCache(long updateTime, Set<Integer> slots) {
            this.updateTime = updateTime;
            this.slots = slots;
        }
    }

    private boolean checkSlotInProxyNode(ProxyClusterSlotMap clusterSlotMap, ProxyNode node, int slot) {
        try {
            if (clusterSlotMap.getCurrentNode().equals(node)) {
                return true;
            }
            SlotCache slotCache = slotCacheMap.get(node);
            if (slotCache != null && System.currentTimeMillis() - slotCache.updateTime <= cacheMillis) {
                return slotCache.slots.contains(slot);
            }
            int lockIndex = Math.abs(node.toString().hashCode()) % 32;
            ReentrantLock lock = lockArray[lockIndex];
            lock.lock();
            try {
                slotCache = slotCacheMap.get(node);
                if (slotCache != null && System.currentTimeMillis() - slotCache.updateTime <= cacheMillis) {
                    return slotCache.slots.contains(slot);
                }
                RedisConnectionAddr target = new RedisConnectionAddr(node.getHost(), node.getCport(), null, ServerConf.cportPassword());
                RedisConnection connection = RedisConnectionHub.getInstance().get(target);
                if (connection == null) {
                    logger.error("checkSlotInProxyNode error, proxyNode = {}, slot = {}, connection null", node, slot);
                    return false;
                }
                CompletableFuture<Reply> future = connection.sendCommand(RedisCommand.CLUSTER.raw(), RedisKeyword.NODES.getRaw());
                Reply reply = future.get(1000, TimeUnit.MILLISECONDS);
                if (reply instanceof ErrorReply) {
                    logger.error("checkSlotInProxyNode error, proxyNode = {}, slot = {}, error-reply = {}", node, slot, reply);
                    return false;
                }
                if (reply instanceof BulkReply) {
                    String clusterNodes = Utils.bytesToString(((BulkReply) reply).getRaw());
                    String[] split = clusterNodes.split("\n");
                    String targetLine = null;
                    for (String line : split) {
                        if (line.contains("myself")) {
                            targetLine = line;
                            break;
                        }
                    }
                    if (targetLine == null) {
                        logger.warn("checkSlotInProxyNode error, proxyNode = {}, slot = {}, clusterNodes:\n{}", node, slot, clusterNodes);
                        return false;
                    }
                    String slotStr = (targetLine.split("connected")[1]).trim();
                    String[] slotArray = slotStr.split(" ");
                    Set<Integer> slots = new HashSet<>();
                    for (String str : slotArray) {
                        if (str.contains("-")) {
                            String[] startEnd = str.split("-");
                            for (int i = Integer.parseInt(startEnd[0]); i <= Integer.parseInt(startEnd[1]); i++) {
                                slots.add(i);
                            }
                        } else {
                            slots.add(Integer.parseInt(str));
                        }
                    }
                    slotCacheMap.put(node, new SlotCache(System.currentTimeMillis(), slots));
                    boolean result = slots.contains(slot);
                    logger.info("checkSlotInProxyNode success, proxyNode = {}, slot = {}, slot.size = {}, result = {}", node, slot, slots.size(), result);
                    return result;
                }
                logger.error("checkSlotInProxyNode error, proxyNode = {}, slot = {}, reply = {}", node, slot, reply);
                return false;
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            logger.error("checkSlotInProxyNode error, proxyNode = {}, slot = {}", node, slot, e);
            return false;
        }
    }
}
