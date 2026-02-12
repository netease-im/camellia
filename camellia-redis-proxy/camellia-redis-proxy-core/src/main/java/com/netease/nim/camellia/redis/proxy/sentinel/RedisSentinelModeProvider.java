package com.netease.nim.camellia.redis.proxy.sentinel;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/7/26
 */
public class RedisSentinelModeProvider implements SentinelModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelModeProvider.class);

    private static final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("leader-selector-scheduler"));

    private ProxyNode currentNode;
    private String redisUrl;
    private String redisKey;

    private Set<ProxyNode> nodes;

    @Override
    public void init(ProxyNode currentNode) {
        this.currentNode = currentNode;
        this.redisUrl = ProxyDynamicConf.getString("sentinel.mode.provider.redis.url", null);
        if (redisUrl == null) {
            throw new IllegalArgumentException("'sentinel.mode.provider.redis.url' is empty");
        }
        this.redisKey = ProxyDynamicConf.getString("sentinel.mode.provider.redis.key", null);
        if (redisKey == null) {
            throw new IllegalArgumentException("'sentinel.mode.provider.redis.key' is empty");
        }
        heartbeat();
        int heartbeatIntervalSeconds = ProxyDynamicConf.getInt("sentinel.mode.provider.heartbeat.interval.seconds", 5);
        heartbeatScheduler.scheduleAtFixedRate(this::heartbeat, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public List<ProxyNode> load() {
        return new ArrayList<>(nodes);
    }

    private void heartbeat() {
        try {
            byte[][] args1 = new byte[][] {RedisCommand.ZADD.raw(), Utils.stringToBytes(redisKey),
                    Utils.stringToBytes(String.valueOf(TimeCache.currentMillis)), Utils.stringToBytes(currentNode.toString())};
            long expireSeconds = ProxyDynamicConf.getLong("sentinel.mode.provider.expire.seconds", 15);
            byte[][] args2 = new byte[][] {RedisCommand.ZREMRANGEBYSCORE.raw(), Utils.stringToBytes(redisKey),
                    Utils.stringToBytes(String.valueOf(0)), Utils.stringToBytes(String.valueOf(TimeCache.currentMillis - expireSeconds * 1000L))};
            byte[][] args3 = new byte[][] {RedisCommand.EXPIRE.raw(), Utils.stringToBytes(redisKey), Utils.stringToBytes(String.valueOf(expireSeconds * 2))};
            byte[][] args4 = new byte[][] {RedisCommand.ZRANGEBYSCORE.raw(), Utils.stringToBytes(redisKey),
                    Utils.stringToBytes(String.valueOf(TimeCache.currentMillis - expireSeconds * 1000L)), Utils.stringToBytes(String.valueOf(TimeCache.currentMillis))};
            long timeoutMillis = ProxyDynamicConf.getLong("sentinel.mode.provider.heartbeat.timeout.millis", 10000);
            List<Command> commands = new ArrayList<>(4);
            commands.add(new Command(args1));
            commands.add(new Command(args2));
            commands.add(new Command(args3));
            commands.add(new Command(args4));
            List<Reply> replies = sendCommands(commands, timeoutMillis);
            for (Reply reply : replies) {
                if (reply instanceof ErrorReply) {
                    logger.error("heartbeat error, error = {}", ((ErrorReply) reply).getError());
                }
            }
            Set<ProxyNode> nodes = new HashSet<>();
            Reply reply = replies.get(3);
            if (reply instanceof MultiBulkReply) {
                Reply[] replies1 = ((MultiBulkReply) reply).getReplies();
                for (Reply reply1 : replies1) {
                    if (reply1 instanceof BulkReply) {
                        String str = Utils.bytesToString(((BulkReply) reply1).getRaw());
                        ProxyNode proxyNode = ProxyNode.parseString(str);
                        if (proxyNode != null) {
                            nodes.add(proxyNode);
                        }
                    }
                }
            }
            if (!nodes.isEmpty()) {
                if (!nodes.equals(this.nodes)) {
                    logger.info("sentinel mode nodes update, old.nodes = {}, new.nodes = {}", this.nodes, nodes);
                    this.nodes = nodes;
                }
            }
        } catch (Exception e) {
            logger.error("heartbeat error", e);
        }
    }

    private List<Reply> sendCommands(List<Command> commands, long timeoutMillis) throws Exception {
        List<CompletableFuture<Reply>> futures = new ArrayList<>(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            futures.add(new CompletableFuture<>());
        }
        IUpstreamClient client = GlobalRedisProxyEnv.getRedisProxyEnv().getClientFactory().get(redisUrl);
        client.sendCommand(-1, commands, futures);
        List<Reply> replies = new ArrayList<>(commands.size());
        for (CompletableFuture<Reply> future : futures) {
            replies.add(future.get(timeoutMillis, TimeUnit.MILLISECONDS));
        }
        return replies;
    }
}
