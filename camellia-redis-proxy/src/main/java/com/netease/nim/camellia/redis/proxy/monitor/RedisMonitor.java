package com.netease.nim.camellia.redis.proxy.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.async.*;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.MaxValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();
    private static Stats stats = new Stats();
    private static final ConcurrentHashMap<String, LongAdder> failCountMap = new ConcurrentHashMap<>();

    private static boolean monitorEnable;
    private static boolean commandSpendTimeMonitorEnable;

    private static final ConcurrentHashMap<String, LongAdder> commandSpendCountMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> commandSpendTotalMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MaxValue> commandSpendMaxMap = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> templateMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> redisClientMap = new ConcurrentHashMap<>();

    public static void init(int seconds, boolean commandSpendTimeMonitorEnable, MonitorCallback monitorCallback) {
        if (initOk.compareAndSet(false, true)) {
            intervalSeconds = seconds;
            ExecutorUtils.scheduleAtFixedRate(RedisMonitor::calc, seconds, seconds, TimeUnit.SECONDS);
            RedisMonitor.monitorEnable = true;
            RedisMonitor.commandSpendTimeMonitorEnable = commandSpendTimeMonitorEnable;
            RedisMonitor.monitorCallback = monitorCallback;
            ProxyDynamicConf.registerCallback(RedisMonitor::reloadConf);
            reloadConf();
        }
    }

    private static void reloadConf() {
        RedisMonitor.monitorEnable = ProxyDynamicConf.monitorEnable(RedisMonitor.monitorEnable);
        RedisMonitor.commandSpendTimeMonitorEnable = ProxyDynamicConf.commandSpendTimeMonitorEnable(RedisMonitor.commandSpendTimeMonitorEnable);
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
     * 统计 AsyncCamelliaRedisTemplate
     */
    public static void registerRedisTemplate(Long bid, String bgroup, AsyncCamelliaRedisTemplate template) {
        templateMap.put(bid + "|" + bgroup, template);
    }

    /**
     * 统计RedisClient，增加
     */
    public static void addRedisClient(RedisClient redisClient) {
        try {
            ConcurrentHashMap<String, RedisClient> subMap = getRedisClientSubMap(redisClient);
            subMap.put(redisClient.getClientName(), redisClient);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 统计RedisClient，减少
     */
    public static void removeRedisClient(RedisClient redisClient) {
        try {
            ConcurrentHashMap<String, RedisClient> subMap = getRedisClientSubMap(redisClient);
            subMap.remove(redisClient.getClientName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> getTemplateMap() {
        return templateMap;
    }

    public static ConcurrentHashMap<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> getRedisClientMap() {
        return redisClientMap;
    }

    private static ConcurrentHashMap<String, RedisClient> getRedisClientSubMap(RedisClient redisClient) {
        RedisClientConfig config = redisClient.getRedisClientConfig();
        RedisClientAddr addr = new RedisClientAddr(config.getHost(), config.getPort(), config.getPassword());
        ConcurrentHashMap<String, RedisClient> subMap = redisClientMap.get(addr);
        if (subMap == null) {
            subMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, RedisClient> oldMap = redisClientMap.putIfAbsent(addr, subMap);
            if (oldMap != null) {
                subMap = oldMap;
            }
        }
        return subMap;
    }

    /**
     * 根据后端redis集群记录请求
     */
    public static void incr(Long bid, String bgroup, String url, String command) {
        if (!monitorEnable) return;
        try {
            String key = bid + "|" + bgroup + "|" + url + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(resourceCommandBidBgroupMap, key, k -> new LongAdder());
            count.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
     * command spend time incr
     */
    public static void incrCommandSpendTime(Long bid, String bgroup, String command, long spendNanoTime) {
        try {
            if (!commandSpendTimeMonitorEnable || !monitorEnable) return;
            String key = bid + "|" + bgroup + "|" + command;
            CamelliaMapUtils.computeIfAbsent(commandSpendCountMap, key, k -> new LongAdder()).increment();
            CamelliaMapUtils.computeIfAbsent(commandSpendTotalMap, key, k -> new LongAdder()).add(spendNanoTime);
            MaxValue maxValue = CamelliaMapUtils.computeIfAbsent(commandSpendMaxMap, key, k -> new MaxValue());
            maxValue.update(spendNanoTime);
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
     * get stats json
     */
    public static JSONObject getStatsJson() {
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
        return monitorJson;
    }

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

            List<Stats.BidBgroupSpendStats> bidBgroupSpendStatsList = new ArrayList<>();
            ConcurrentHashMap<String, AtomicLong> spendSumMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Stats.SpendStats> spendStatsMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, LongAdder> entry : commandSpendCountMap.entrySet()) {
                String key = entry.getKey();
                long count = entry.getValue().sumThenReset();
                if (count == 0) continue;
                MaxValue nanoMax = commandSpendMaxMap.get(key);
                double maxSpendMs = 0;
                if (nanoMax != null) {
                    maxSpendMs = nanoMax.getAndSet(0) / 1000000.0;
                }
                double avgSpendMs = 0;
                LongAdder nanoSum = commandSpendTotalMap.get(key);
                long sum = 0;
                if (nanoSum != null) {
                    sum = nanoSum.sumThenReset();
                    avgSpendMs = sum / (1000000.0 * count);
                }
                String[] split = key.split("\\|");
                Long bid = null;
                if (!split[0].equals("null")) {
                    bid = Long.parseLong(split[0]);
                }
                String bgroup = null;
                if (!split[1].equals("null")) {
                    bgroup = split[1];
                }
                String command = split[2];
                Stats.BidBgroupSpendStats bidBgroupSpendStats = new Stats.BidBgroupSpendStats();
                bidBgroupSpendStats.setBid(bid);
                bidBgroupSpendStats.setBgroup(bgroup);
                bidBgroupSpendStats.setCommand(command);
                bidBgroupSpendStats.setAvgSpendMs(avgSpendMs);
                bidBgroupSpendStats.setMaxSpendMs(maxSpendMs);
                bidBgroupSpendStats.setCount(count);
                bidBgroupSpendStatsList.add(bidBgroupSpendStats);

                Stats.SpendStats spendStats = CamelliaMapUtils.computeIfAbsent(spendStatsMap, command, string -> {
                    Stats.SpendStats bean = new Stats.SpendStats();
                    bean.setCommand(string);
                    return bean;
                });
                spendStats.setCount(spendStats.getCount() + bidBgroupSpendStats.getCount());
                if (bidBgroupSpendStats.getMaxSpendMs() > spendStats.getMaxSpendMs()) {
                    spendStats.setMaxSpendMs(bidBgroupSpendStats.getMaxSpendMs());
                }
                CamelliaMapUtils.computeIfAbsent(spendSumMap, command, k -> new AtomicLong()).addAndGet(sum);
            }
            for (Map.Entry<String, Stats.SpendStats> entry : spendStatsMap.entrySet()) {
                String command = entry.getKey();
                Stats.SpendStats spendStats = entry.getValue();
                AtomicLong sum = spendSumMap.get(command);
                if (sum == null) {
                    spendStats.setAvgSpendMs(0.0);
                } else {
                    spendStats.setAvgSpendMs(sum.get() / (1000000.0 * spendStats.getCount()));
                }
            }

            ConcurrentHashMap<String, Stats.ResourceStats> resourceStatsMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Stats.ResourceCommandStats> resourceCommandStatsMap = new ConcurrentHashMap<>();

            ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = RedisMonitor.resourceCommandBidBgroupMap;
            RedisMonitor.resourceCommandBidBgroupMap = new ConcurrentHashMap<>();
            List<Stats.ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList = new ArrayList<>();
            ConcurrentHashMap<String, Stats.ResourceBidBgroupStats> resourceBidBgroupStatsMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, LongAdder> entry : resourceCommandBidBgroupMap.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\|");
                Long bid = null;
                if (!split[0].equals("null")) {
                    bid = Long.parseLong(split[0]);
                }
                String bgroup = null;
                if (!split[1].equals("null")) {
                    bgroup = split[1];
                }
                String url = split[2];
                String command = split[3];
                Stats.ResourceBidBgroupCommandStats stats = new Stats.ResourceBidBgroupCommandStats();
                stats.setBid(bid);
                stats.setBgroup(bgroup);
                stats.setResource(url);
                stats.setCommand(command);
                stats.setCount(entry.getValue().sum());
                resourceBidBgroupCommandStatsList.add(stats);

                Stats.ResourceStats resourceStats = CamelliaMapUtils.computeIfAbsent(resourceStatsMap, url, string -> {
                    Stats.ResourceStats bean = new Stats.ResourceStats();
                    bean.setResource(string);
                    return bean;
                });
                resourceStats.setCount(resourceStats.getCount() + stats.getCount());

                Stats.ResourceCommandStats resourceCommandStats = CamelliaMapUtils.computeIfAbsent(resourceCommandStatsMap, url + "|" + command, string -> {
                    String[] strings = string.split("\\|");
                    Stats.ResourceCommandStats bean = new Stats.ResourceCommandStats();
                    bean.setResource(strings[0]);
                    bean.setCommand(strings[1]);
                    return bean;
                });
                resourceCommandStats.setCount(resourceCommandStats.getCount() + stats.getCount());

                Stats.ResourceBidBgroupStats resourceBidBgroupStats = CamelliaMapUtils.computeIfAbsent(resourceBidBgroupStatsMap, bid + "|" + bgroup + "|" + url, string -> {
                    String[] split1 = string.split("\\|");
                    Long bid1 = null;
                    if (!split1[0].equals("null")) {
                        bid1 = Long.parseLong(split1[0]);
                    }
                    String bgroup1 = null;
                    if (!split1[1].equals("null")) {
                        bgroup1 = split1[1];
                    }
                    String url1 = split1[2];
                    Stats.ResourceBidBgroupStats bean = new Stats.ResourceBidBgroupStats();
                    bean.setBid(bid1);
                    bean.setBgroup(bgroup1);
                    bean.setResource(url1);
                    return bean;
                });
                resourceBidBgroupStats.setCount(resourceBidBgroupStats.getCount() + stats.getCount());
            }

            List<Stats.RouteConf> routeConfList = new ArrayList<>();
            for (Map.Entry<String, AsyncCamelliaRedisTemplate> entry : templateMap.entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("\\|");
                Long bid = null;
                if (!split[0].equals("null")) {
                    bid = Long.parseLong(split[0]);
                }
                String bgroup = null;
                if (!split[1].equals("null")) {
                    bgroup = split[1];
                }
                String resourceTable = ReadableResourceTableUtil.readableResourceTable(entry.getValue().getResourceTable());
                Stats.RouteConf routeConf = new Stats.RouteConf();
                routeConf.setBid(bid);
                routeConf.setBgroup(bgroup);
                routeConf.setResourceTable(resourceTable);
                routeConf.setUpdateTime(entry.getValue().getResourceTableUpdateTime());
                routeConfList.add(routeConf);
            }

            Stats.RedisConnectStats redisConnectStats = new Stats.RedisConnectStats();
            List<Stats.RedisConnectStats.Detail> detailList = new ArrayList<>();
            for (Map.Entry<RedisClientAddr, ConcurrentHashMap<String, RedisClient>> entry : redisClientMap.entrySet()) {
                RedisClientAddr key = entry.getKey();
                ConcurrentHashMap<String, RedisClient> subMap = entry.getValue();
                if (subMap.isEmpty()) continue;
                redisConnectStats.setConnectCount(redisConnectStats.getConnectCount() + subMap.size());
                Stats.RedisConnectStats.Detail detail = new Stats.RedisConnectStats.Detail();
                detail.setAddr(key.getUrl());
                detail.setConnectCount(subMap.size());
                detailList.add(detail);
            }
            redisConnectStats.setDetailList(detailList);

            Stats stats = new Stats();
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
            stats.setSpendStatsList(new ArrayList<>(spendStatsMap.values()));
            stats.setBidBgroupSpendStatsList(bidBgroupSpendStatsList);
            stats.setIntervalSeconds(intervalSeconds);
            stats.setResourceStatsList(new ArrayList<>(resourceStatsMap.values()));
            stats.setResourceCommandStatsList(new ArrayList<>(resourceCommandStatsMap.values()));
            stats.setResourceBidBgroupStatsList(new ArrayList<>(resourceBidBgroupStatsMap.values()));
            stats.setResourceBidBgroupCommandStatsList(resourceBidBgroupCommandStatsList);
            stats.setRouteConfList(routeConfList);
            stats.setRedisConnectStats(redisConnectStats);

            RedisMonitor.stats = stats;

            if (monitorCallback != null) {
                monitorCallback.callback(stats);
            }
        } catch (Exception e) {
            logger.error("calc error", e);
        }
    }
}
