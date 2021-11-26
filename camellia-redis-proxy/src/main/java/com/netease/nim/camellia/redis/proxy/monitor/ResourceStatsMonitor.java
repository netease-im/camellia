package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.util.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class ResourceStatsMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ResourceStatsMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = new ConcurrentHashMap<>();

    public static void incr(Long bid, String bgroup, String url, String command) {
        if (!RedisMonitor.isMonitorEnable()) return;
        try {
            String key = bid + "|" + bgroup + "|" + url + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(resourceCommandBidBgroupMap, key, k -> new LongAdder());
            count.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class ResourceStats {
        List<Stats.ResourceStats> resourceStatsList = new ArrayList<>();
        List<Stats.ResourceCommandStats> resourceCommandStatsList = new ArrayList<>();
        List<Stats.ResourceBidBgroupStats> resourceBidBgroupStatsList = new ArrayList<>();
        List<Stats.ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList = new ArrayList<>();
    }

    public static ResourceStats calc() {
        try {
            ConcurrentHashMap<String, Stats.ResourceStats> resourceStatsMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Stats.ResourceCommandStats> resourceCommandStatsMap = new ConcurrentHashMap<>();

            ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = ResourceStatsMonitor.resourceCommandBidBgroupMap;
            ResourceStatsMonitor.resourceCommandBidBgroupMap = new ConcurrentHashMap<>();
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
                String url = PasswordMaskUtils.maskResource(split[2]);
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

            ResourceStats resourceStats = new ResourceStats();
            resourceStats.resourceStatsList = new ArrayList<>(resourceStatsMap.values());
            resourceStats.resourceCommandStatsList = new ArrayList<>(resourceCommandStatsMap.values());
            resourceStats.resourceBidBgroupStatsList = new ArrayList<>(resourceBidBgroupStatsMap.values());
            resourceStats.resourceBidBgroupCommandStatsList = resourceBidBgroupCommandStatsList;

            return resourceStats;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ResourceStats();
        }
    }
}
