package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClusterSlotInfo {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterSlotInfo.class);

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    //slot -> master redis node
    private Map<Integer, Node> slotMap = new HashMap<>();
    private Set<Node> nodeSet = new HashSet<>();

    private RedisClusterResource redisClusterResource;
    private String password;
    public RedisClusterSlotInfo(RedisClusterResource redisClusterResource) {
        if (redisClusterResource == null) {
            throw new CamelliaRedisException("redisClusterResource is null");
        }
        this.redisClusterResource = redisClusterResource;
        this.password = redisClusterResource.getPassword();
    }

    /**
     * 根据slot获取client
     * @param slot slot
     * @return client
     */
    public RedisClient getClient(int slot) {
        r.lock();
        try {
            Node node = slotMap.get(slot);
            return RedisClientHub.get(node.getHost(), node.getPort(), redisClusterResource.getPassword());
        } finally {
            r.unlock();
        }
    }

    /**
     * 刷新slot信息
     */
    private long lastRenewTimestamp = 0L;
    private AtomicBoolean renew = new AtomicBoolean(false);
    public boolean renew() {
        //限制1s内最多renew一次
        if (ServerStatus.getCurrentTimeMillis() - lastRenewTimestamp < 1000) {
            return false;
        }
        if (renew.compareAndSet(false, true)) {
            try {
                w.lock();
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
                    logger.info("renew success");
                } else {
                    ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew fail");
                }
                lastRenewTimestamp = ServerStatus.getCurrentTimeMillis();
                return success;
            } finally {
                w.unlock();
                renew.set(false);
            }
        }
        return true;
    }

    private boolean tryRenew(String host, int port, String password) {
        try {
            RedisClient client = RedisClientHub.get(host, port, password);
            if (client == null || !client.isValid()) return false;
            CompletableFuture<Reply> future = client.sendCommand(RedisCommand.CLUSTER.raw(), SafeEncoder.encode("slots"));
            logger.info("tryRenew, client=" + client.getClientName());
            Reply reply = future.get(10000, TimeUnit.MILLISECONDS);
            return clusterNodes(reply);
        } catch (Exception e) {
            logger.error("tryRenew error", e);
            return false;
        }
    }

    private boolean clusterNodes(Reply reply) {
        try {
            Map<Integer, Node> slotMap = new HashMap<>();
            Set<Node> nodeSet = new HashSet<>();

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
                    Node node = new Node(SafeEncoder.encode(host.getRaw()), port.getInteger().intValue());
                    nodeSet.add(node);
                    for (long i=slotStart.getInteger(); i<=slotEnd.getInteger(); i++) {
                        slotMap.put((int) i, node);
                    }
                }
            } else if (reply instanceof ErrorReply) {
                throw new CamelliaRedisException(((ErrorReply) reply).getError());
            } else {
                throw new CamelliaRedisException("decode clusterNodes error");
            }
            boolean success = true;
            for (Node node : nodeSet) {
                RedisClient client = RedisClientHub.get(node.getHost(), node.getPort(), password);
                if (client == null) {
                    success = false;
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("node.size = {}, slotMap.size = {}", nodeSet.size(), slotMap.size());
            }
            this.nodeSet = nodeSet;
            this.slotMap = slotMap;
            return success;
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    private static class Node {
        private String host;
        private int port;

        public Node(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            if (port != node.port) return false;
            return host != null ? host.equals(node.host) : node.host == null;
        }

        @Override
        public int hashCode() {
            int result = host != null ? host.hashCode() : 0;
            result = 31 * result + port;
            return result;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
