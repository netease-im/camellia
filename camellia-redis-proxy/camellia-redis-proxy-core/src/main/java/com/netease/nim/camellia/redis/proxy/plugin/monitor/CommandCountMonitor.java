package com.netease.nim.camellia.redis.proxy.plugin.monitor;

import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.model.BidBgroupStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.DetailStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.TotalStats;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2022/9/16
 */
public class CommandCountMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CommandCountMonitor.class);

    private static int count = 0;
    private static ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduleService = Executors.newSingleThreadScheduledExecutor(
            new DefaultThreadFactory("camellia-qps-monitor"));
    private static final MaxQps maxQps = new MaxQps();

    private static class MaxQps {
        public AtomicLong qps = new AtomicLong();
        public AtomicLong readQps = new AtomicLong();
        public AtomicLong writeQps = new AtomicLong();
    }

    private static final LongAdder read = new LongAdder();
    private static final LongAdder write = new LongAdder();

    static {
        scheduleService.scheduleAtFixedRate(() -> {
            long readQps = read.sumThenReset();
            long writeQps = write.sumThenReset();
            ProxyInfoUtils.updateLastQps(readQps, writeQps);
            if (readQps + writeQps > maxQps.qps.get()) {
                maxQps.qps.set(readQps + writeQps);
            }
            if (readQps > maxQps.readQps.get()) {
                maxQps.readQps.set(readQps);
            }
            if (writeQps > maxQps.writeQps.get()) {
                maxQps.writeQps.set(writeQps);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void incr(Long bid, String bgroup, String command, RedisCommand.Type type) {
        try {
            String key = bid + "|" + bgroup + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(map, key, k -> new LongAdder());
            count.increment();
            if (type == RedisCommand.Type.READ) {
                read.increment();
            } else if (type == RedisCommand.Type.WRITE) {
                write.increment();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class CommandCounterStats {
        public long count;
        public long maxQps;
        public long totalReadCount;
        public long maxReadQps;
        public long totalWriteCount;
        public long maxWriteQps;
        public List<TotalStats> totalStatsList = new ArrayList<>();
        public List<BidBgroupStats> bidBgroupStatsList = new ArrayList<>();
        public List<DetailStats> detailStatsList = new ArrayList<>();
    }

    public static CommandCounterStats collect() {
        count ++;
        long totalCount = 0;
        long totalReadCount = 0;
        long totalWriteCount = 0;
        Map<String, TotalStats> totalStatsMap = new HashMap<>();
        Map<String, BidBgroupStats> bidBgroupStatsMap = new HashMap<>();
        List<DetailStats> detailStatsList = new ArrayList<>();

        ConcurrentHashMap<String, LongAdder> map = CommandCountMonitor.map;
        if (count >= ProxyDynamicConf.getInt("monitor.cache.reset.interval.periods", 60)) {
            CommandCountMonitor.map = new ConcurrentHashMap<>();
            count = 0;
        }
        for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
            String[] split = entry.getKey().split("\\|");
            long count = entry.getValue().sumThenReset();
            if (count == 0) continue;
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
        counterStats.maxQps = maxQps.qps.getAndSet(0);
        counterStats.totalReadCount = totalReadCount;
        counterStats.maxReadQps = maxQps.readQps.getAndSet(0);
        counterStats.totalWriteCount = totalWriteCount;
        counterStats.maxWriteQps = maxQps.writeQps.getAndSet(0);
        counterStats.detailStatsList = detailStatsList;
        counterStats.totalStatsList = new ArrayList<>(totalStatsMap.values());
        counterStats.bidBgroupStatsList = new ArrayList<>(bidBgroupStatsMap.values());
        return counterStats;
    }
}
