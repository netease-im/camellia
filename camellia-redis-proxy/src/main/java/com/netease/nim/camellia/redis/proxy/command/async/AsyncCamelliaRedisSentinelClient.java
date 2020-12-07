package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Created by caojiajun on 2020/08/07.
 */
public class AsyncCamelliaRedisSentinelClient extends AsyncCamelliaSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisSentinelClient.class);
    private static final byte[] SENTINEL_GET_MASTER_ADDR_BY_NAME = SafeEncoder.encode("get-master-addr-by-name");
    private static final byte[] MASTER_SWITCH = SafeEncoder.encode("+switch-master");
    private static final AtomicInteger id = new AtomicInteger(0);

    private final RedisSentinelResource redisSentinelResource;
    private volatile RedisClientAddr redisClientAddr;
    private final Object lock = new Object();

    public AsyncCamelliaRedisSentinelClient(RedisSentinelResource redisSentinelResource) {
        this.redisSentinelResource = redisSentinelResource;
        String master = redisSentinelResource.getMaster();
        boolean sentinelAvailable = false;
        for (RedisSentinelResource.Node node : redisSentinelResource.getNodes()) {
            RedisClient redisClient = null;
            try {
                String host = node.getHost();
                int port = node.getPort();
                redisClient = new RedisClient(host, port, null, RedisClientHub.eventLoopGroup,
                        -1, -1, RedisClientHub.connectTimeoutMillis);
                redisClient.start();
                if (redisClient.isValid()) {
                    sentinelAvailable = true;
                    CompletableFuture<Reply> future = redisClient.sendCommand(RedisCommand.SENTINEL.raw(), SENTINEL_GET_MASTER_ADDR_BY_NAME, SafeEncoder.encode(master));
                    Reply reply = future.get(10, TimeUnit.SECONDS);
                    boolean success = processMasterGet(reply);
                    if (success) break;
                }
            } catch (Exception e) {
                logger.error("Can not get master addr, master name = {}, sentinel = {}", master, node.getHost() + ":" + node.getPort(), e);
            } finally {
                if (redisClient != null) {
                    redisClient.stop(true);
                }
            }
        }
        if (redisClientAddr == null) {
            if (sentinelAvailable) {
                throw new CamelliaRedisException("Can connect to sentinel, but " + master + " seems to be not monitored...");
            } else {
                throw new CamelliaRedisException("All sentinels down, cannot determine where is " + master + " master is running...");
            }
        }
        for (RedisSentinelResource.Node node : redisSentinelResource.getNodes()) {
            MasterListener masterListener = new MasterListener(this, master, node);
            masterListener.start();
        }
    }

    @Override
    public RedisClientAddr getAddr() {
        return redisClientAddr;
    }

    @Override
    public Resource getResource() {
        return redisSentinelResource;
    }

    private class MasterListener extends Thread {

        private final AsyncCamelliaRedisSentinelClient redisSentinelClient;
        private final RedisSentinelResource.Node node;
        private final String master;
        private boolean running = true;

        public MasterListener(AsyncCamelliaRedisSentinelClient redisSentinelClient, String master, RedisSentinelResource.Node node) {
            this.redisSentinelClient = redisSentinelClient;
            this.master = master;
            this.node = node;
            setName("sentinel-master-listener[" + redisSentinelResource.getUrl() + "][" + node.getHost() + ":" + node.getPort() + "][" + id.incrementAndGet() + "]");
        }

        @Override
        public void run() {
            RedisClient redisClient = null;
            while (running) {
                try {
                    if (redisClient == null || !redisClient.isValid()) {
                        if (redisClient != null && !redisClient.isValid()) {
                            redisClient.stop();
                        }
                        redisClient = new RedisClient(node.getHost(), node.getPort(), null, RedisClientHub.eventLoopGroup,
                                -1, -1, RedisClientHub.connectTimeoutMillis);
                        redisClient.start();
                    }

                    if (redisClient.isValid()) {
                        CompletableFuture<Reply> future1 = redisClient.sendCommand(RedisCommand.SENTINEL.raw(), SENTINEL_GET_MASTER_ADDR_BY_NAME, SafeEncoder.encode(master));
                        Reply getMasterReply = future1.get(10, TimeUnit.SECONDS);
                        processMasterGet(getMasterReply);

                        CompletableFuture<Reply> future2 = redisClient.sendCommand(RedisCommand.SUBSCRIBE.raw(), MASTER_SWITCH);
                        Reply subscribeReply = future2.get();
                        processMasterSwitch(subscribeReply);
                        while (running) {
                            CompletableFuture<Reply> future01 = new CompletableFuture<>();
                            CompletableFuture<Reply> future02 = new CompletableFuture<>();
                            List<Command> commands = Collections.singletonList(new Command(new byte[][] {RedisCommand.PING.raw()}));
                            List<CompletableFuture<Reply>> futures = new ArrayList<>();
                            futures.add(future01);
                            futures.add(future02);
                            redisClient.sendCommand(commands, futures);

                            for (CompletableFuture<Reply> future : futures) {
                                Reply reply = null;
                                while (running) {
                                    try {
                                        reply = future.get(10, TimeUnit.SECONDS);
                                        break;
                                    } catch (Exception ignore) {
                                        if (!redisClient.isValid()) {
                                            break;
                                        }
                                    }
                                }
                                if (!redisClient.isValid()) {
                                    break;
                                }
                                processMasterSwitch(reply);
                            }
                            if (!redisClient.isValid()) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Lost connection to Sentinel at {}. Sleeping 5000ms and retrying.", node.getHost() + ":" + node.getPort(), e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(5000);
                    } catch (InterruptedException ex) {
                        logger.error(ex.getMessage(), e);
                    }
                }
            }
            if (redisClient != null && redisClient.isValid()) {
                redisClient.stop();
            }
            logger.info("redis sentinel master listener thread stop, resource = {}, node = {}", redisSentinelResource.getUrl(), node.getHost() + ":" + node.getPort());
        }

        public void shutdown() {
            running = false;
        }

        private void processMasterSwitch(Reply reply) {
            if (reply == null) return;
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length == 3) {
                    BulkReply bulkReply = (BulkReply) replies[0];
                    if (RedisKeyword.MESSAGE.name().toLowerCase().equalsIgnoreCase(SafeEncoder.encode(bulkReply.getRaw()))) {
                        BulkReply msgReply = (BulkReply) replies[2];
                        String msg = SafeEncoder.encode(msgReply.getRaw());
                        String[] switchMasterMsg = msg.split(" ");
                        if (switchMasterMsg.length > 3) {
                            if (master.equals(switchMasterMsg[0])) {
                                synchronized (lock) {
                                    RedisClientAddr oldNode = redisSentinelClient.redisClientAddr;
                                    RedisClientAddr newNode = new RedisClientAddr(switchMasterMsg[3], Integer.parseInt(switchMasterMsg[4]),
                                            redisSentinelResource.getPassword());
                                    if (!Objects.equals(oldNode, newNode)) {
                                        redisSentinelClient.redisClientAddr = newNode;
                                        if (logger.isInfoEnabled()) {
                                            logger.info("sentinel redis master node update, resource = {}, old = {}, new = {}", redisSentinelResource.getUrl(), oldNode, newNode);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean processMasterGet(Reply reply) {
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies != null && replies.length == 2) {
                BulkReply hostReply = (BulkReply) replies[0];
                BulkReply portReply = (BulkReply) replies[1];
                String redisHost = SafeEncoder.encode(hostReply.getRaw());
                int redisPort = Integer.parseInt(SafeEncoder.encode(portReply.getRaw()));
                synchronized (lock) {
                    RedisClientAddr newNode = new RedisClientAddr(redisHost, redisPort, redisSentinelResource.getPassword());
                    RedisClientAddr oldNode = redisClientAddr;
                    if (!Objects.equals(newNode, oldNode)) {
                        redisClientAddr = newNode;
                        if (logger.isInfoEnabled()) {
                            logger.info("sentinel redis master node refresh, resource = {}, old = {}, new = {}", redisSentinelResource.getUrl(), oldNode, newNode);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

}
