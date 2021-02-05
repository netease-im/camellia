package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClusterSlotInfo {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterSlotInfo.class);

    private static final ExecutorService redisClusterRenewExec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), new DefaultThreadFactory("redis-cluster-renew"), new ThreadPoolExecutor.AbortPolicy());

    public static final int SLOT_SIZE = 16384;

    //slot -> master redis node
    private Node[] slotArray = new Node[SLOT_SIZE];
    private Set<Node> nodeSet = new HashSet<>();

    private final RedisClusterResource redisClusterResource;
    private final String password;
    public RedisClusterSlotInfo(RedisClusterResource redisClusterResource) {
        if (redisClusterResource == null) {
            throw new CamelliaRedisException("redisClusterResource is null");
        }
        this.redisClusterResource = redisClusterResource;
        this.password = redisClusterResource.getPassword();
    }

    /**
     * get client by slot
     * @param slot slot
     * @return client
     */
    public RedisClient getClient(int slot) {
        Node node = slotArray[slot];
        if (node == null) return null;
        return RedisClientHub.get(node.getAddr());
    }

    /**
     * get node by slot
     * @param slot slot
     * @return node
     */
    public Node getNode(int slot) {
        return slotArray[slot];
    }

    /**
     * renew slot info
     */
    private long lastRenewTimestamp = 0L;
    private final AtomicBoolean renew = new AtomicBoolean(false);
    public Future<Boolean> renew() {
        //限制1s内最多renew一次
        if (TimeCache.currentMillis - lastRenewTimestamp < 1000) {
            return null;
        }
        if (renew.compareAndSet(false, true)) {
            try {
                return redisClusterRenewExec.submit(() -> {
                    try {
                        boolean success = false;
                        for (Node node : nodeSet) {
                            success = tryRenew(node.getHost(), node.getPort(), password);
                            if (success) break;
                        }
                        if (!success) {
                            for (RedisClusterResource.Node node : redisClusterResource.getNodes()) {
                                success = tryRenew(node.getHost(), node.getPort(), password);
                                if (success) break;
                            }
                        }
                        if (success) {
                            logger.info("renew success, url = {}", redisClusterResource.getUrl());
                        } else {
                            ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew fail, url = " + redisClusterResource.getUrl());
                        }
                        lastRenewTimestamp = TimeCache.currentMillis;
                        return success;
                    } catch (Exception e) {
                        ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew error, url = " + redisClusterResource.getUrl(), e);
                        return false;
                    } finally {
                        renew.set(false);
                    }
                });
            } catch (Exception e) {
                ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew error, url = " + redisClusterResource.getUrl(), e);
                renew.set(false);
            }
        }
        return null;
    }

    private boolean tryRenew(String host, int port, String password) {
        RedisClient client = null;
        try {
            client = RedisClientHub.newClient(host, port, password);
            if (client == null || !client.isValid()) return false;
            CompletableFuture<Reply> future = client.sendCommand(RedisCommand.CLUSTER.raw(), SafeEncoder.encode("slots"));
            logger.info("tryRenew, client = {}, url = {}", client.getClientName(), redisClusterResource.getUrl());
            Reply reply = future.get(10000, TimeUnit.MILLISECONDS);
            return clusterNodes(reply);
        } catch (Exception e) {
            logger.error("tryRenew error, host = {}, port = {}, url = {}", host, port, redisClusterResource.getUrl(), e);
            return false;
        } finally {
            if (client != null) {
                client.stop(true);
            }
        }
    }

    private boolean clusterNodes(Reply reply) {
        try {
            Node[] slotArray = new Node[SLOT_SIZE];
            Set<Node> nodeSet = new HashSet<>();

            int size = 0;
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                for (Reply reply1 : replies) {

                    MultiBulkReply reply2 = (MultiBulkReply) reply1;
                    Reply[] replies1 = reply2.getReplies();
                    IntegerReply slotStart = (IntegerReply)replies1[0];
                    IntegerReply slotEnd = (IntegerReply)replies1[1];
                    MultiBulkReply master = (MultiBulkReply) replies1[2];
                    Reply[] replies2 = master.getReplies();
                    BulkReply host = (BulkReply) replies2[0];
                    IntegerReply port = (IntegerReply)replies2[1];
                    Node node = new Node(SafeEncoder.encode(host.getRaw()), port.getInteger().intValue(), password);
                    nodeSet.add(node);
                    for (long i=slotStart.getInteger(); i<=slotEnd.getInteger(); i++) {
                        slotArray[(int)i] = node;
                        size ++;
                    }
                }
            } else if (reply instanceof ErrorReply) {
                throw new CamelliaRedisException(((ErrorReply) reply).getError());
            } else {
                throw new CamelliaRedisException("decode clusterNodes error");
            }
            boolean success = size == SLOT_SIZE;
            if (logger.isDebugEnabled()) {
                logger.debug("node.size = {}, url = {}", nodeSet.size(), redisClusterResource.getUrl());
            }
            if (!nodeSet.isEmpty()) {
                this.nodeSet = nodeSet;
            }
            if (size > 0) {
                this.slotArray = slotArray;
            }
            if (!success) {
                logger.error("slot size is {}, not {}, url = {}", size, SLOT_SIZE, redisClusterResource.getUrl());
            }
            return success;
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public static class Node {
        private final String host;
        private final int port;
        private final String password;
        private final RedisClientAddr addr;

        public Node(String host, int port, String password) {
            this.host = host;
            this.port = port;
            this.password = password;
            this.addr = new RedisClientAddr(host, port, password);
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getPassword() {
            return password;
        }

        public RedisClientAddr getAddr() {
            return addr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(addr, node.addr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(addr);
        }

        @Override
        public String toString() {
            return addr.getUrl();
        }
    }
}
