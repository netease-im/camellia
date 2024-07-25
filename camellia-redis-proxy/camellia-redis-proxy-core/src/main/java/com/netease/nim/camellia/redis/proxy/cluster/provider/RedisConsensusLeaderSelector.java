package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/6/28
 */
public class RedisConsensusLeaderSelector extends AbstractConsensusLeaderSelector {

    private static final Logger logger = LoggerFactory.getLogger(RedisConsensusLeaderSelector.class);

    private static final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("leader-selector-scheduler"));
    private static final ScheduledExecutorService slotMapScheduler = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("slot-map-flush-scheduler"));

    private ProxyNode currentNode;
    private ProxyNode leaderNode;
    private String redisUrl;
    private String redisKey;
    private String slotMapKey;
    private ProxyClusterSlotMap slotMap;

    @Override
    public void init(ProxyNode currentNode, List<ProxyNode> initNodes) {
        this.currentNode = currentNode;
        this.redisUrl = ProxyDynamicConf.getString("redis.consensus.leader.selector.redis.url", null);
        if (redisUrl == null) {
            throw new IllegalArgumentException("'redis.consensus.leader.selector.redis.url' is empty");
        }
        this.redisKey = ProxyDynamicConf.getString("redis.consensus.leader.selector.redis.key", null);
        if (redisKey == null) {
            throw new IllegalArgumentException("'redis.consensus.leader.selector.redis.key' is empty");
        }
        this.slotMapKey = redisKey + "#slot_map";
        heartbeat();
        if (this.leaderNode == null) {
            throw new IllegalStateException("heartbeat error");
        }
        int heartbeatIntervalSeconds = ProxyDynamicConf.getInt("redis.consensus.leader.selector.heartbeat.interval.seconds", 5);
        heartbeatScheduler.scheduleAtFixedRate(this::heartbeat, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
        int slotMapFlushIntervalSeconds = ProxyDynamicConf.getInt("redis.consensus.leader.selector.slot.map.flush.interval.seconds", 30);
        slotMapScheduler.scheduleAtFixedRate(this::flushSlotMapToRedis, slotMapFlushIntervalSeconds, slotMapFlushIntervalSeconds, TimeUnit.SECONDS);
        logger.info("RedisConsensusMasterSelector init success, currentNode = {}, redisKey = {}, heartbeatIntervalSeconds = {}, slotMapFlushIntervalSeconds = {}, redisUrl = {}",
                currentNode, redisKey, heartbeatIntervalSeconds, slotMapFlushIntervalSeconds, PasswordMaskUtils.maskResource(redisUrl));
    }

    @Override
    public ProxyNode getLeader() {
        return leaderNode;
    }

    @Override
    public ProxyClusterSlotMap getSlotMap() {
        try {
            IUpstreamClient client = GlobalRedisProxyEnv.getRedisProxyEnv().getClientFactory().get(redisUrl);
            byte[][] args = new byte[][]{RedisCommand.GET.raw(), Utils.stringToBytes(slotMapKey)};
            Command command = new Command(args);
            CompletableFuture<Reply> future = new CompletableFuture<>();
            client.sendCommand(-1, Collections.singletonList(command), Collections.singletonList(future));
            long timeoutMillis = ProxyDynamicConf.getLong("redis.consensus.leader.selector.slot.map.load.timeout.millis", 5000);
            Reply reply = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof ErrorReply) {
                logger.error("getSlotMap error, reply = {}", reply);
                throw new IllegalStateException(((ErrorReply) reply).getError());
            }
            if (reply instanceof BulkReply) {
                byte[] raw = ((BulkReply) reply).getRaw();
                if (raw == null) {
                    return null;
                }
                return ProxyClusterSlotMap.parseString(Utils.bytesToString(raw));
            }
            throw new IllegalStateException();
        } catch (Exception e) {
            logger.error("getSlotMap error", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void saveSlotMap(ProxyClusterSlotMap slotMap) {
        if (!currentNode.equals(leaderNode)) {
            logger.error("saveSlotMap forbidden, currentNode = {}, leaderNode = {}", currentNode, leaderNode);
            throw new IllegalStateException("follower node can't save slot map");
        }
        this.slotMap = slotMap;
        flushSlotMapToRedis();
    }

    private long leaderHeartbeatExpireSeconds() {
        return ProxyDynamicConf.getInt("redis.consensus.leader.selector.heartbeat.expire.seconds", 15);
    }

    private static final byte[] SCRIPT = SafeEncoder.encode("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end");

    private void heartbeat() {
        try {
            byte[][] args1 = new byte[][] {RedisCommand.SET.raw(), Utils.stringToBytes(redisKey), Utils.stringToBytes(currentNode.toString()),
                    RedisKeyword.NX.getRaw(), RedisKeyword.EX.getRaw(), Utils.stringToBytes(String.valueOf(leaderHeartbeatExpireSeconds()))};
            Command command1 = new Command(args1);
            byte[][] args2 = new byte[][] {RedisCommand.GET.raw(), Utils.stringToBytes(redisKey)};
            Command command2 = new Command(args2);
            List<Command> commands = new ArrayList<>(2);
            commands.add(command1);
            commands.add(command2);
            long timeoutMillis = ProxyDynamicConf.getLong("redis.consensus.leader.selector.heartbeat.timeout.millis", 5000);
            List<Reply> replies = sendCommands(commands, timeoutMillis);
            Reply reply = replies.get(1);
            if (reply instanceof ErrorReply) {
                logger.error("heartbeat error, reply = {}", reply);
                return;
            }
            if (reply instanceof BulkReply) {
                ProxyNode leaderNode = ProxyNode.parseString(Utils.bytesToString(((BulkReply) reply).getRaw()));
                if (leaderNode == null) {
                    logger.error("heartbeat error, leaderNode is null");
                    return;
                }
                if (leaderNode.equals(this.leaderNode)) {
                    if (leaderNode.equals(currentNode)) {
                        byte[][] args3 = new byte[][]{RedisCommand.EVAL.raw(), SCRIPT, Utils.stringToBytes("1"), Utils.stringToBytes(redisKey),
                                Utils.stringToBytes(leaderNode.toString()), Utils.stringToBytes(String.valueOf(leaderHeartbeatExpireSeconds() * 1000L))};
                        Command command3 = new Command(args3);
                        Reply reply1 = sendCommand(command3, timeoutMillis);
                        if (reply1 instanceof ErrorReply) {
                            logger.error("delay leader redis key error, reply = {}", reply1);
                        }
                    }
                    return;
                }
                logger.info("leaderNode changed, leaderNode = {} -> {}", this.leaderNode, leaderNode);
                this.leaderNode = leaderNode;
                notifyLeaderChange();
                return;
            }
            logger.warn("heartbeat error, reply = {}", reply);
        } catch (Exception e) {
            logger.warn("heartbeat error", e);
        }
    }

    private void flushSlotMapToRedis() {
        if (!currentNode.equals(leaderNode)) {
            return;
        }
        try {
            long seconds = ProxyDynamicConf.getLong("redis.consensus.leader.selector.slot.map.expire.seconds", 120);
            byte[][] args = new byte[][] {RedisCommand.SETEX.raw(), Utils.stringToBytes(slotMapKey),
                    Utils.stringToBytes(String.valueOf(seconds)), Utils.stringToBytes(slotMap.toString())};
            Command command = new Command(args);
            long timeoutMillis = ProxyDynamicConf.getLong("redis.consensus.leader.selector.slot.map.flush.timeout.millis", 10000);
            Reply reply = sendCommand(command, timeoutMillis);
            if (reply instanceof ErrorReply) {
                logger.error("flushSlotMapToRedis error, reply = {}", reply);
                return;
            }
            logger.info("flushSlotMapToRedis success, slotMap = {}, reply = {}", slotMap, reply);
        } catch (Exception e) {
            logger.error("flushSlotMapToRedis error", e);
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

    private Reply sendCommand(Command command, long timeoutMillis) throws Exception {
        CompletableFuture<Reply> future = new CompletableFuture<>();
        IUpstreamClient client = GlobalRedisProxyEnv.getRedisProxyEnv().getClientFactory().get(redisUrl);
        client.sendCommand(-1, Collections.singletonList(command), Collections.singletonList(future));
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

}
