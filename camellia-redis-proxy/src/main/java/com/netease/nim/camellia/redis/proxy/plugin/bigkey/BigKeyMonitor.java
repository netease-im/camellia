package com.netease.nim.camellia.redis.proxy.plugin.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.monitor.model.BigKeyStats;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class BigKeyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(BigKeyMonitor.class);

    private static ConcurrentHashMap<String, BigKeyStats> statsMap = new ConcurrentHashMap<>();

    public static void bigKey(Command command, byte[] key, long size, long threshold) {
        try {
            int maxCount = ProxyDynamicConf.getInt("big.key.monitor.json.max.count", 100);
            if (statsMap.size() >= maxCount) return;
            CommandContext commandContext = command.getCommandContext();
            String bid = commandContext.getBid() == null ? "default" : String.valueOf(commandContext.getBid());
            String bgroup = commandContext.getBgroup() == null ? "default" : commandContext.getBgroup();
            RedisCommand redisCommand = command.getRedisCommand();
            String keyStr = Utils.bytesToString(key);
            String uniqueKey = bid + "|" + bgroup + "|" + redisCommand + "|" + keyStr;
            BigKeyStats bigKeyStats = CamelliaMapUtils.computeIfAbsent(statsMap, uniqueKey, k -> new BigKeyStats());
            bigKeyStats.setBid(bid);
            bigKeyStats.setBgroup(bgroup);
            bigKeyStats.setCommandType(redisCommand.getCommandType().name());;
            bigKeyStats.setCommand(command.getName());
            bigKeyStats.setKey(keyStr);
            bigKeyStats.setSize(size);
            bigKeyStats.setThreshold(threshold);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static List<BigKeyStats> collect() {
        ConcurrentHashMap<String, BigKeyStats> statsMap = BigKeyMonitor.statsMap;
        BigKeyMonitor.statsMap = new ConcurrentHashMap<>();
        return new ArrayList<>(statsMap.values());
    }

}
