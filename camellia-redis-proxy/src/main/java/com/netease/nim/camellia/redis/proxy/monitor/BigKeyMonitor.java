package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2020/12/1
 */
public class BigKeyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(BigKeyMonitor.class);

    private static ConcurrentHashMap<String, BigKeyStats> statsMap = new ConcurrentHashMap<>();

    public static void init(int seconds) {
        ExecutorUtils.scheduleAtFixedRate(BigKeyMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void bigKey(Command command, byte[] key, long size, long threshold) {
        try {
            CommandContext commandContext = command.getCommandContext();
            String bid = commandContext.getBid() == null ? "default" : String.valueOf(commandContext.getBid());
            String bgroup = commandContext.getBgroup() == null ? "default" : commandContext.getBgroup();
            RedisCommand redisCommand = command.getRedisCommand();
            String keyStr = Utils.bytesToString(key);
            String uniqueKey = bid + "|" + bgroup + "|" + redisCommand + "|" + keyStr;
            BigKeyStats bigKeyStats = statsMap.computeIfAbsent(uniqueKey, k -> new BigKeyStats());
            bigKeyStats.bid = bid;
            bigKeyStats.bgroup = bgroup;
            bigKeyStats.commandType = redisCommand.getCommandType().name();
            bigKeyStats.command = command.getName();
            bigKeyStats.key = keyStr;
            bigKeyStats.size = size;
            bigKeyStats.threshold = threshold;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static JSONObject monitorJson = new JSONObject();

    private static void calc() {
        try {
            JSONObject json = new JSONObject();
            if (statsMap.isEmpty()) {
                monitorJson = json;
                return;
            }
            ConcurrentHashMap<String, BigKeyStats> statsMap = BigKeyMonitor.statsMap;
            BigKeyMonitor.statsMap = new ConcurrentHashMap<>();
            JSONArray bigKeyJsonArray = new JSONArray();
            for (BigKeyStats bigKeyStats : statsMap.values()) {
                JSONObject bigKeyJson = new JSONObject();
                bigKeyJson.put("bid", bigKeyStats.bid);
                bigKeyJson.put("bgroup", bigKeyStats.bgroup);
                bigKeyJson.put("commandType", bigKeyStats.commandType);
                bigKeyJson.put("command", bigKeyStats.command);
                bigKeyJson.put("key", bigKeyStats.key);
                bigKeyJson.put("size", bigKeyStats.size);
                bigKeyJson.put("threshold", bigKeyStats.threshold);
                bigKeyJsonArray.add(bigKeyJson);
            }
            json.put("bigKeyStats", bigKeyJsonArray);
            monitorJson = json;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static JSONObject getBigKeyStatsJson() {
        return monitorJson;
    }

    private static class BigKeyStats {
        String bid;
        String bgroup;
        String commandType;
        String command;
        String key;
        long size;
        long threshold;
    }
}
