package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class RedisMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisMonitor.class);
    private static final ThreadLocal<SimpleDateFormat> dataFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    private static final AtomicBoolean initOk = new AtomicBoolean(false);
    private static MonitorCallback monitorCallback;
    private static int intervalSeconds;

    private static boolean monitorEnable;
    private static boolean commandSpendTimeMonitorEnable;
    private static boolean upstreamRedisSpendTimeMonitorEnable;

    private static Stats stats = new Stats();

    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    public static void init(CamelliaServerProperties serverProperties, MonitorCallback monitorCallback) {
        if (initOk.compareAndSet(false, true)) {
            int seconds = serverProperties.getMonitorIntervalSeconds();
            intervalSeconds = seconds;
            ExecutorUtils.scheduleAtFixedRate(RedisMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
            RedisMonitor.monitorEnable = true;
            RedisMonitor.commandSpendTimeMonitorEnable = serverProperties.isCommandSpendTimeMonitorEnable();
            RedisMonitor.upstreamRedisSpendTimeMonitorEnable = serverProperties.isUpstreamRedisSpendTimeMonitorEnable();
            RedisMonitor.monitorCallback = monitorCallback;
            ProxyDynamicConf.registerCallback(RedisMonitor::reloadConf);
            reloadConf();
            logger.info("RedisMonitor init success, intervalSeconds = {}, monitorEnable = {}, commandSpendTimeMonitorEnable = {}, upstreamRedisSpendTimeMonitorEnable = {}",
                    intervalSeconds, monitorEnable, commandSpendTimeMonitorEnable, upstreamRedisSpendTimeMonitorEnable);
        }
    }

    private static void reloadConf() {
        RedisMonitor.monitorEnable = ProxyDynamicConf.monitorEnable(RedisMonitor.monitorEnable);
        RedisMonitor.commandSpendTimeMonitorEnable = ProxyDynamicConf.commandSpendTimeMonitorEnable(RedisMonitor.commandSpendTimeMonitorEnable);
        RedisMonitor.upstreamRedisSpendTimeMonitorEnable = ProxyDynamicConf.upstreamRedisSpendTimeMonitorEnable(RedisMonitor.upstreamRedisSpendTimeMonitorEnable);
    }

    /**
     * monitorEnable
     * @return monitorEnable
     */
    public static boolean isMonitorEnable() {
        return monitorEnable;
    }

    /**
     * 命令耗时开关
     */
    public static boolean isCommandSpendTimeMonitorEnable() {
        return monitorEnable && commandSpendTimeMonitorEnable;
    }

    /**
     * 后端响应耗时开关
     */
    public static boolean isUpstreamRedisSpendTimeMonitorEnable() {
        return monitorEnable && upstreamRedisSpendTimeMonitorEnable;
    }

    /**
     * command count incr
     */
    public static void incr(Long bid, String bgroup, String command) {
        if (!monitorEnable) return;
        try {
            String key = bid + "|" + bgroup + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(map, key, k -> new LongAdder());
            count.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * command fail incr
     */
    public static void incrFail(String failReason) {
        if (!monitorEnable) return;
        try {
            LongAdder failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, failReason, k -> new LongAdder());
            failCount.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * get Stats
     */
    public static Stats getStats() {
        return stats;
    }

    /**
     * 定时计算
     */
    private static void calc() {
        try {
            long totalCount = 0;
            long totalReadCount = 0;
            long totalWriteCount = 0;
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

                Stats.TotalStats totalStats = totalStatsMap.get(command);
                if (totalStats == null) {
                    totalStats = totalStatsMap.computeIfAbsent(command, Stats.TotalStats::new);
                }
                totalStats.setCount(totalStats.getCount() + count);

                String key = bid + "|" + bgroup;
                Stats.BidBgroupStats bidBgroupStats = bidBgroupStatsMap.get(key);
                if (bidBgroupStats == null) {
                    bidBgroupStats = bidBgroupStatsMap.computeIfAbsent(key, k -> new Stats.BidBgroupStats());
                }
                bidBgroupStats.setBid(bid);
                bidBgroupStats.setBgroup(bgroup);
                bidBgroupStats.setCount(bidBgroupStats.getCount() + count);

                detailStatsList.add(new Stats.DetailStats(bid, bgroup, command, count));

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

            Stats stats = new Stats();
            stats.setIntervalSeconds(intervalSeconds);
            stats.setClientConnectCount(ChannelMonitor.getChannelMap().size());
            stats.setCount(totalCount);
            stats.setTotalReadCount(totalReadCount);
            stats.setTotalWriteCount(totalWriteCount);
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

            CommandSpendMonitor.CommandSpendStats commandSpendStats = CommandSpendMonitor.calc();
            stats.setSpendStatsList(commandSpendStats.spendStatsList);
            stats.setBidBgroupSpendStatsList(commandSpendStats.bidBgroupSpendStatsList);

            ResourceStatsMonitor.ResourceStats resourceStats = ResourceStatsMonitor.calc();
            stats.setResourceStatsList(resourceStats.resourceStatsList);
            stats.setResourceCommandStatsList(resourceStats.resourceCommandStatsList);
            stats.setResourceBidBgroupStatsList(resourceStats.resourceBidBgroupStatsList);
            stats.setResourceBidBgroupCommandStatsList(resourceStats.resourceBidBgroupCommandStatsList);

            stats.setRouteConfList(RouteConfMonitor.calc());
            stats.setRedisConnectStats(RedisClientMonitor.calc());
            stats.setUpstreamRedisSpendStatsList(UpstreamRedisSpendTimeMonitor.calc());

            RedisMonitor.stats = stats;

            if (monitorCallback != null) {
                monitorCallback.callback(stats);
            }
            ProxyInfoUtils.updateStats(stats);
        } catch (Exception e) {
            logger.error("calc error", e);
        }
    }

    /**
     * get stats json
     */
    public static JSONObject getStatsJson() {
        Stats stats = RedisMonitor.getStats();

        JSONObject monitorJson = new JSONObject();
        JSONArray connectJsonArray = new JSONArray();
        JSONObject connectJson = new JSONObject();
        connectJson.put("connect", stats.getClientConnectCount());
        connectJsonArray.add(connectJson);
        monitorJson.put("connectStats", connectJsonArray);

        JSONArray countJsonArray = new JSONArray();
        JSONObject countJson = new JSONObject();
        countJson.put("count", stats.getCount());
        countJson.put("totalReadCount", stats.getTotalReadCount());
        countJson.put("totalWriteCount", stats.getTotalWriteCount());
        countJsonArray.add(countJson);
        monitorJson.put("countStats", countJsonArray);

        JSONArray qpsJsonArray = new JSONArray();
        JSONObject qpsJson = new JSONObject();
        qpsJson.put("qps", stats.getCount() / (stats.getIntervalSeconds() * 1.0));
        qpsJson.put("readQps", stats.getTotalReadCount() / (stats.getIntervalSeconds() * 1.0));
        qpsJson.put("writeQps", stats.getTotalWriteCount() / (stats.getIntervalSeconds() * 1.0));
        qpsJsonArray.add(qpsJson);
        monitorJson.put("qpsStats", qpsJsonArray);

        JSONArray totalJsonArray = new JSONArray();
        for (Stats.TotalStats totalStats : stats.getTotalStatsList()) {
            JSONObject totalJson = new JSONObject();
            totalJson.put("command", totalStats.getCommand());
            totalJson.put("count", totalStats.getCount());
            totalJson.put("qps", totalStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            totalJsonArray.add(totalJson);
        }
        monitorJson.put("total", totalJsonArray);

        JSONArray bigBgroupJsonArray = new JSONArray();
        for (Stats.BidBgroupStats bidBgroupStats : stats.getBidBgroupStatsList()) {
            JSONObject bidBgroupJson = new JSONObject();
            bidBgroupJson.put("bid", bidBgroupStats.getBid() == null ? "default" : String.valueOf(bidBgroupStats.getBid()));
            bidBgroupJson.put("bgroup", bidBgroupStats.getBgroup() == null ? "default" : bidBgroupStats.getBgroup());
            bidBgroupJson.put("count", bidBgroupStats.getCount());
            bidBgroupJson.put("qps", bidBgroupStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            bigBgroupJsonArray.add(bidBgroupJson);
        }
        monitorJson.put("bidbgroup", bigBgroupJsonArray);

        JSONArray detailJsonArray = new JSONArray();
        for (Stats.DetailStats detailStats : stats.getDetailStatsList()) {
            JSONObject detailJson = new JSONObject();
            detailJson.put("bid", detailStats.getBid() == null ? "default" : String.valueOf(detailStats.getBid()));
            detailJson.put("bgroup", detailStats.getBgroup() == null ? "default" : detailStats.getBgroup());
            detailJson.put("command", detailStats.getCommand());
            detailJson.put("count", detailStats.getCount());
            detailJson.put("qps", detailStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            detailJsonArray.add(detailJson);
        }
        monitorJson.put("detail", detailJsonArray);

        JSONArray failJsonArray = new JSONArray();
        for (Map.Entry<String, Long> entry : stats.getFailMap().entrySet()) {
            String failReason = entry.getKey();
            Long count = entry.getValue();
            JSONObject failJson = new JSONObject();
            failJson.put("reason", failReason);
            failJson.put("count", count);
            failJsonArray.add(failJson);
        }
        monitorJson.put("failStats", failJsonArray);

        JSONArray spendJsonArray = new JSONArray();
        for (Stats.SpendStats spendStats : stats.getSpendStatsList()) {
            JSONObject spendJson = new JSONObject();
            spendJson.put("command", spendStats.getCommand());
            spendJson.put("count", spendStats.getCount());
            spendJson.put("avgSpendMs", spendStats.getAvgSpendMs());
            spendJson.put("maxSpendMs", spendStats.getMaxSpendMs());
            spendJsonArray.add(spendJson);
        }
        monitorJson.put("spendStats", spendJsonArray);

        JSONArray bidBgroupSpendJsonArray = new JSONArray();
        for (Stats.BidBgroupSpendStats bidBgroupSpendStats : stats.getBidBgroupSpendStatsList()) {
            JSONObject spendJson = new JSONObject();
            spendJson.put("bid", bidBgroupSpendStats.getBid() == null ? "default" : bidBgroupSpendStats.getBid());
            spendJson.put("bgroup", bidBgroupSpendStats.getBgroup() == null ? "default" : bidBgroupSpendStats.getBgroup());
            spendJson.put("command", bidBgroupSpendStats.getCommand());
            spendJson.put("count", bidBgroupSpendStats.getCount());
            spendJson.put("avgSpendMs", bidBgroupSpendStats.getAvgSpendMs());
            spendJson.put("maxSpendMs", bidBgroupSpendStats.getMaxSpendMs());
            bidBgroupSpendJsonArray.add(spendJson);
        }
        monitorJson.put("bidBgroupSpendStats", bidBgroupSpendJsonArray);

        JSONArray resourceStatsJsonArray = new JSONArray();
        for (Stats.ResourceStats resourceStats : stats.getResourceStatsList()) {
            JSONObject json = new JSONObject();
            json.put("resource", resourceStats.getResource());
            json.put("count", resourceStats.getCount());
            json.put("qps", resourceStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            resourceStatsJsonArray.add(json);
        }
        monitorJson.put("resourceStats", resourceStatsJsonArray);

        JSONArray resourceCommandStatsJsonArray = new JSONArray();
        for (Stats.ResourceCommandStats resourceCommandStats : stats.getResourceCommandStatsList()) {
            JSONObject json = new JSONObject();
            json.put("resource", resourceCommandStats.getResource());
            json.put("command", resourceCommandStats.getCommand());
            json.put("count", resourceCommandStats.getCount());
            json.put("qps", resourceCommandStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            resourceCommandStatsJsonArray.add(json);
        }
        monitorJson.put("resourceCommandStats", resourceCommandStatsJsonArray);

        JSONArray resourceBidBgroupStatsJsonArray = new JSONArray();
        for (Stats.ResourceBidBgroupStats resourceBidBgroupStats : stats.getResourceBidBgroupStatsList()) {
            JSONObject json = new JSONObject();
            json.put("bid", resourceBidBgroupStats.getBid() == null ? "default" : resourceBidBgroupStats.getBid());
            json.put("bgroup", resourceBidBgroupStats.getBgroup() == null ? "default" : resourceBidBgroupStats.getBgroup());
            json.put("resource", resourceBidBgroupStats.getResource());
            json.put("count", resourceBidBgroupStats.getCount());
            json.put("qps", resourceBidBgroupStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            resourceBidBgroupStatsJsonArray.add(json);
        }
        monitorJson.put("resourceBidBgroupStats", resourceBidBgroupStatsJsonArray);

        JSONArray resourceBidBgroupCommandStatsJsonArray = new JSONArray();
        for (Stats.ResourceBidBgroupCommandStats resourceBidBgroupCommandStats : stats.getResourceBidBgroupCommandStatsList()) {
            JSONObject json = new JSONObject();
            json.put("bid", resourceBidBgroupCommandStats.getBid() == null ? "default" : resourceBidBgroupCommandStats.getBid());
            json.put("bgroup", resourceBidBgroupCommandStats.getBgroup() == null ? "default" : resourceBidBgroupCommandStats.getBgroup());
            json.put("resource", resourceBidBgroupCommandStats.getResource());
            json.put("command", resourceBidBgroupCommandStats.getCommand());
            json.put("count", resourceBidBgroupCommandStats.getCount());
            json.put("qps", resourceBidBgroupCommandStats.getCount() / (stats.getIntervalSeconds() * 1.0));
            resourceBidBgroupCommandStatsJsonArray.add(json);
        }
        monitorJson.put("resourceBidBgroupCommandStats", resourceBidBgroupCommandStatsJsonArray);

        JSONArray routeConfJsonArray = new JSONArray();
        for (Stats.RouteConf routeConf : stats.getRouteConfList()) {
            JSONObject json = new JSONObject();
            json.put("bid", routeConf.getBid() == null ? "default" : routeConf.getBid());
            json.put("bgroup", routeConf.getBgroup() == null ? "default" : routeConf.getBgroup());
            json.put("resourceTable", routeConf.getResourceTable());
            json.put("updateTime", dataFormat.get().format(new Date(routeConf.getUpdateTime())));
            routeConfJsonArray.add(json);
        }
        monitorJson.put("routeConf", routeConfJsonArray);

        Stats.RedisConnectStats redisConnectStats = stats.getRedisConnectStats();
        JSONArray redisConnectTotalStatsJsonArray = new JSONArray();
        JSONObject redisConnectTotalStatsJson = new JSONObject();
        redisConnectTotalStatsJson.put("connect", redisConnectStats.getConnectCount());
        redisConnectTotalStatsJsonArray.add(redisConnectTotalStatsJson);
        monitorJson.put("redisConnectStats", redisConnectTotalStatsJsonArray);

        JSONArray redisConnectDetailStatsJsonArray = new JSONArray();
        for (Stats.RedisConnectStats.Detail detail : redisConnectStats.getDetailList()) {
            JSONObject json = new JSONObject();
            json.put("addr", detail.getAddr());
            json.put("connect", detail.getConnectCount());
            redisConnectDetailStatsJsonArray.add(json);
        }
        monitorJson.put("redisConnectDetailStats", redisConnectDetailStatsJsonArray);

        List<Stats.UpstreamRedisSpendStats> upstreamRedisSpendStatsList = stats.getUpstreamRedisSpendStatsList();
        JSONArray upstreamRedisSpendStatsJsonArray = new JSONArray();
        for (Stats.UpstreamRedisSpendStats spendStats : upstreamRedisSpendStatsList) {
            JSONObject json = new JSONObject();
            json.put("addr", spendStats.getAddr());
            json.put("count", spendStats.getCount());
            json.put("avgSpendMs", spendStats.getAvgSpendMs());
            json.put("maxSpendMs", spendStats.getMaxSpendMs());
            upstreamRedisSpendStatsJsonArray.add(json);
        }
        monitorJson.put("upstreamRedisSpendStats", upstreamRedisSpendStatsJsonArray);

        return monitorJson;
    }

}
