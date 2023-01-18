package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2021/4/9
 */
public class RedisSentinelMasterListener extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelMasterListener.class);

    private static final AtomicLong id = new AtomicLong(0);

    private final Resource resource;
    private final HostAndPort sentinel;
    private final MasterUpdateCallback callback;
    private final String master;
    private boolean running = true;
    private static final int futureBuffer = 32;
    private final AtomicLong futureCount = new AtomicLong();

    public RedisSentinelMasterListener(Resource resource, HostAndPort sentinel, String master, MasterUpdateCallback callback) {
        this.resource = resource;
        this.sentinel = sentinel;
        this.callback = callback;
        this.master = master;
        setName("redis-sentinel-master-listener-" + sentinel.toString() + "-" + id.incrementAndGet());
    }

    public interface MasterUpdateCallback {
        void masterUpdate(HostAndPort master);
    }

    @Override
    public void run() {
        RedisClient redisClient = null;
        logger.info("redis sentinel master listener thread start, resource = {}, sentinel = {}", PasswordMaskUtils.maskResource(resource.getUrl()), sentinel.getUrl());
        while (running) {
            try {
                if (redisClient == null || !redisClient.isValid()) {
                    if (redisClient != null && !redisClient.isValid()) {
                        redisClient.stop();
                    }
                    redisClient = RedisClientHub.getInstance().newClient(sentinel.getHost(), sentinel.getPort(), null, null);
                    while (redisClient == null || !redisClient.isValid()) {
                        logger.error("connect to sentinel fail, sentinel = {}. sleeping 5000ms and retrying.", sentinel.getUrl());
                        try {
                            TimeUnit.MILLISECONDS.sleep(5000);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                        }
                        redisClient = RedisClientHub.getInstance().newClient(sentinel.getHost(), sentinel.getPort(), null, null);
                    }
                }
                if (redisClient.isValid()) {
                    CompletableFuture<Reply> future1 = redisClient.sendCommand(RedisCommand.SENTINEL.raw(), RedisSentinelUtils.SENTINEL_GET_MASTER_ADDR_BY_NAME, Utils.stringToBytes(master));
                    Reply getMasterReply = future1.get(10, TimeUnit.SECONDS);
                    HostAndPort hostAndPort = RedisSentinelUtils.processMasterGet(getMasterReply);
                    if (hostAndPort != null) {
                        callback.masterUpdate(hostAndPort);
                    }

                    CompletableFuture<Reply> future2 = redisClient.sendCommand(RedisCommand.SUBSCRIBE.raw(), RedisSentinelUtils.MASTER_SWITCH);
                    future2.thenAccept(this::_processMasterSwitch);
                    sendFutures(redisClient);
                    while (running) {
                        if (!redisClient.isValid()) {
                            break;
                        }
                        TimeUnit.SECONDS.sleep(10);
                    }
                }
            } catch (Exception e) {
                logger.error("lost connection to sentinel at {}. sleeping 5000ms and retrying.", sentinel.getUrl(), e);
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
        logger.info("redis sentinel master listener thread stop, resource = {}, sentinel = {}", PasswordMaskUtils.maskResource(resource.getUrl()), sentinel.getUrl());
    }

    private void sendFutures(RedisClient redisClient) {
        if (!redisClient.isValid()) return;
        List<CompletableFuture<Reply>> futureList = new ArrayList<>();
        for (int i=0; i<futureBuffer; i++) {
            CompletableFuture<Reply> future = new CompletableFuture<>();
            future.thenAccept(reply -> processMasterSwitch(redisClient, reply));
            futureList.add(future);
        }
        futureCount.addAndGet(futureList.size());
        redisClient.sendCommand(Collections.emptyList(), futureList);
    }

    private void processMasterSwitch(RedisClient redisClient, Reply reply) {
        futureCount.decrementAndGet();
        try {
            _processMasterSwitch(reply);
        } finally {
            if (futureCount.get() < futureBuffer / 2) {
                sendFutures(redisClient);
            }
        }
    }

    private void _processMasterSwitch(Reply reply) {
        if (reply == null) return;
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies.length == 3) {
                BulkReply bulkReply = (BulkReply) replies[0];
                if (RedisKeyword.MESSAGE.name().toLowerCase().equalsIgnoreCase(Utils.bytesToString(bulkReply.getRaw()))) {
                    BulkReply msgReply = (BulkReply) replies[2];
                    String msg = Utils.bytesToString(msgReply.getRaw());
                    String[] switchMasterMsg = msg.split(" ");
                    if (switchMasterMsg.length > 3) {
                        if (master.equals(switchMasterMsg[0])) {
                            HostAndPort hostAndPort = new HostAndPort(switchMasterMsg[3], Integer.parseInt(switchMasterMsg[4]));
                            callback.masterUpdate(hostAndPort);
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}
