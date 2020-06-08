package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class RedisMonitor {

    private static final Logger logger = LoggerFactory.getLogger("stats");

    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();
    private static Stats stats = new Stats();
    private static final ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    public static void init(int seconds) {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("monitor"))
                .scheduleAtFixedRate(RedisMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
    }

    public static void incr(Long bid, String bgroup, String command) {
        String key = bid + "|" + bgroup + "|" + command;
        LongAdder count = map.computeIfAbsent(key, k -> new LongAdder());
        count.increment();
    }

    public static void incrFail(String failReason) {
        LongAdder failCount = failCountMap.computeIfAbsent(failReason, k -> new LongAdder());
        failCount.increment();
    }

    public static Stats getStats() {
        return stats;
    }

    private static void calc() {
        long totalCount = 0;
        Map<String, Stats.TotalStats> totalStatsMap = new HashMap<>();
        Map<String, Stats.BidBgroupStats> bidBgroupStatsMap = new HashMap<>();
        List<Stats.DetailStats> detailStatsList = new ArrayList<>();

        ConcurrentHashMap<String, LongAdder> map = RedisMonitor.map;
        RedisMonitor.map = new ConcurrentHashMap<>();
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

            Stats.TotalStats totalStats = totalStatsMap.computeIfAbsent(command, Stats.TotalStats::new);
            totalStats.setCount(totalStats.getCount() + count);

            String key = bid + "|" + bgroup;
            Stats.BidBgroupStats bidBgroupStats = bidBgroupStatsMap.computeIfAbsent(key, k -> new Stats.BidBgroupStats());
            bidBgroupStats.setBid(bid);
            bidBgroupStats.setBgroup(bgroup);
            bidBgroupStats.setCount(bidBgroupStats.getCount() + count);

            detailStatsList.add(new Stats.DetailStats(bid, bgroup, command, count));

            totalCount += count;
        }

        Stats stats = new Stats();
        stats.setCount(totalCount);
        stats.setDetailStatsList(detailStatsList);
        stats.setTotalStatsList(new ArrayList<>(totalStatsMap.values()));
        stats.setBidBgroupStatsList(new ArrayList<>(bidBgroupStatsMap.values()));
        Map<String, Long> failMap = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : failCountMap.entrySet()) {
            long count = entry.getValue().sumThenReset();
            if (count > 0) {
                failMap.put(entry.getKey(), count);
            }
        }
        stats.setFailMap(failMap);

        RedisMonitor.stats = stats;

        logger.info(">>>>>>>START>>>>>>>");
        logger.info("connect.count={}", ChannelMonitor.getChannelMap().size());
        logger.info("total.count={}", stats.getCount());
        logger.info("====total====");
        for (Stats.TotalStats totalStats : stats.getTotalStatsList()) {
            logger.info("total.command.{}, count={}", totalStats.getCommand(), totalStats.getCount());
        }
        logger.info("====bidbgroup====");
        for (Stats.BidBgroupStats bgroupStats : stats.getBidBgroupStatsList()) {
            logger.info("bidbgroup.{}.{}, count={}", bgroupStats.getBid() == null ? "default" : bgroupStats.getBid(),
                    bgroupStats.getBgroup() == null ? "default" : bgroupStats.getBgroup(), bgroupStats.getCount());
        }
        logger.info("====detail====");
        for (Stats.DetailStats detailStats : stats.getDetailStatsList()) {
            logger.info("detail.{}.{}.{}, count={}", detailStats.getBid() == null ? "default" : detailStats.getBid(),
                    detailStats.getBgroup() == null ? "default" : detailStats.getBgroup(), detailStats.getCommand(), detailStats.getCount());
        }
        logger.info("====fail====");
        for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
            logger.info("fail[{}], count = {}", entry.getKey(), entry.getValue());
        }
        logger.info("<<<<<<<END<<<<<<<");
    }
}
