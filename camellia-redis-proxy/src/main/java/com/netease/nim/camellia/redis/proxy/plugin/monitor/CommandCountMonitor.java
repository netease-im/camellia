package com.netease.nim.camellia.redis.proxy.plugin.monitor;

import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.model.BidBgroupStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.DetailStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.TotalStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/9/16
 */
public class CommandCountMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandCountMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();

    public static void incr(Long bid, String bgroup, String command) {
        try {
            String key = bid + "|" + bgroup + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(map, key, k -> new LongAdder());
            count.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class CommandCounterStats {
        public long count;
        public long totalReadCount;
        public long totalWriteCount;
        public List<TotalStats> totalStatsList = new ArrayList<>();
        public List<BidBgroupStats> bidBgroupStatsList = new ArrayList<>();
        public List<DetailStats> detailStatsList = new ArrayList<>();
    }

    public static CommandCounterStats collect() {
        long totalCount = 0;
        long totalReadCount = 0;
        long totalWriteCount = 0;
        Map<String, TotalStats> totalStatsMap = new HashMap<>();
        Map<String, BidBgroupStats> bidBgroupStatsMap = new HashMap<>();
        List<DetailStats> detailStatsList = new ArrayList<>();

        ConcurrentHashMap<String, LongAdder> map = CommandCountMonitor.map;
        CommandCountMonitor.map = new ConcurrentHashMap<>();
        for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            long count = entry.getValue().longValue();
            Long bid = null;
            if (!split[0].equalsIgnoreCase("null")) {
                bid = Long.parseLong(split[0]);
            }
            String bgroup = null;
            if (!split[1].equalsIgnoreCase("null")) {
                bgroup = split[1];
            }
            String command = split[2];

            TotalStats totalStats = totalStatsMap.get(command);
            if (totalStats == null) {
                totalStats = totalStatsMap.computeIfAbsent(command, TotalStats::new);
            }
            totalStats.setCount(totalStats.getCount() + count);

            String key = bid + "|" + bgroup;
            BidBgroupStats bidBgroupStats = bidBgroupStatsMap.get(key);
            if (bidBgroupStats == null) {
                bidBgroupStats = bidBgroupStatsMap.computeIfAbsent(key, k -> new BidBgroupStats());
            }
            bidBgroupStats.setBid(bid);
            bidBgroupStats.setBgroup(bgroup);
            bidBgroupStats.setCount(bidBgroupStats.getCount() + count);

            detailStatsList.add(new DetailStats(bid, bgroup, command, count));

            totalCount += count;
            RedisCommand redisCommand = RedisCommand.getRedisCommandByName(command);
            if (redisCommand != null && redisCommand.getType() != null) {
                if (redisCommand.getType() == RedisCommand.Type.READ) {
                    totalReadCount += count;
                } else {
                    totalWriteCount += count;
                }
            }
        }

        CommandCounterStats counterStats = new CommandCounterStats();
        counterStats.count = totalCount;
        counterStats.totalReadCount = totalReadCount;
        counterStats.totalWriteCount = totalWriteCount;
        counterStats.detailStatsList = detailStatsList;
        counterStats.totalStatsList = new ArrayList<>(totalStatsMap.values());
        counterStats.bidBgroupStatsList = new ArrayList<>(bidBgroupStatsMap.values());
        return counterStats;
    }
}
