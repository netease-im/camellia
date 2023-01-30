package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/4/8
 */
public class RedisSentinelUtils {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelUtils.class);

    public static final byte[] SENTINEL_GET_MASTER_ADDR_BY_NAME = Utils.stringToBytes("get-master-addr-by-name");
    public static final byte[] MASTER_SWITCH = Utils.stringToBytes("+switch-master");
    public static final byte[] SLAVES = Utils.stringToBytes("slaves");

    public static RedisSentinelMasterResponse getMasterAddr(String host, int port, String masterName) {
        RedisConnection redisConnection = null;
        boolean sentinelAvailable = false;
        HostAndPort master = null;
        try {
            redisConnection = RedisConnectionHub.getInstance().newClient(host, port, null, null);
            if (redisConnection != null && redisConnection.isValid()) {
                sentinelAvailable = true;
                CompletableFuture<Reply> future = redisConnection.sendCommand(RedisCommand.SENTINEL.raw(), SENTINEL_GET_MASTER_ADDR_BY_NAME, Utils.stringToBytes(masterName));
                Reply reply = future.get(10, TimeUnit.SECONDS);
                master = processMasterGet(reply);
            }
            return new RedisSentinelMasterResponse(master, sentinelAvailable);
        } catch (Exception e) {
            logger.error("Can not get master addr, master name = {}, sentinel = {}", master, host + ":" + port, e);
            return new RedisSentinelMasterResponse(master, sentinelAvailable);
        } finally {
            if (redisConnection != null) {
                redisConnection.stop(true);
            }
        }
    }

    public static RedisSentinelSlavesResponse getSlaveAddrs(String host, int port, String masterName) {
        RedisConnection redisConnection = null;
        boolean sentinelAvailable = false;
        List<HostAndPort> slaves = null;
        try {
            redisConnection = RedisConnectionHub.getInstance().newClient(host, port, null, null);
            if (redisConnection != null && redisConnection.isValid()) {
                sentinelAvailable = true;
                CompletableFuture<Reply> future = redisConnection.sendCommand(RedisCommand.SENTINEL.raw(), SLAVES, Utils.stringToBytes(masterName));
                Reply reply = future.get(10, TimeUnit.SECONDS);
                slaves = processSlaves(reply);
            }
            return new RedisSentinelSlavesResponse(slaves, sentinelAvailable);
        } catch (Exception e) {
            logger.error("can not get slaves addr, master name = {}, sentinel = {}", masterName, host + ":" + port, e);
            return new RedisSentinelSlavesResponse(null, sentinelAvailable);
        } finally {
            if (redisConnection != null) {
                redisConnection.stop(true);
            }
        }
    }

    public static List<HostAndPort> processSlaves(Reply reply) {
        List<HostAndPort> slaves = new ArrayList<>();
        if (reply instanceof MultiBulkReply) {
            for (Reply reply1 : ((MultiBulkReply) reply).getReplies()) {
                if (reply1 instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply1).getReplies();
                    if (replies.length % 2 == 0) {
                        Map<String, String> map = new HashMap<>();
                        for (int i = 0; i < replies.length; i++, i++) {
                            Reply key = replies[i];
                            Reply value = replies[i + 1];
                            if (key instanceof BulkReply && value instanceof BulkReply) {
                                map.put(key.toString(), value.toString());
                            }
                        }
                        String slaveIp = map.get("ip");
                        int slavePort = Integer.parseInt(map.get("port"));
                        String flags = map.get("flags");
                        if (slaveIp != null && flags != null && flags.equals("slave")) {
                            slaves.add(new HostAndPort(slaveIp, slavePort));
                        }
                    }
                }
            }
        }
        return slaves;
    }

    public static HostAndPort processMasterGet(Reply reply) {
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies != null && replies.length == 2) {
                BulkReply hostReply = (BulkReply) replies[0];
                BulkReply portReply = (BulkReply) replies[1];
                String redisHost = Utils.bytesToString(hostReply.getRaw());
                int redisPort = Integer.parseInt(Utils.bytesToString(portReply.getRaw()));
                return new HostAndPort(redisHost, redisPort);
            }
        }
        return null;
    }
}
