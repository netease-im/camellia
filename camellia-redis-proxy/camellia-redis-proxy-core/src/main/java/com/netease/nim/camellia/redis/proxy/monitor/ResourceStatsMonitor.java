package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.model.ResourceBidBgroupCommandStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.ResourceBidBgroupStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.ResourceCommandStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.ResourceStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class ResourceStatsMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ResourceStatsMonitor.class);

    private static int count = 0;

    private static ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = new ConcurrentHashMap<>();

    public static void incr(Long bid, String bgroup, String url, String command) {
        if (!ProxyMonitorCollector.isMonitorEnable()) return;
        try {
            String key = bid + "|" + bgroup + "|" + url + "|" + command;
            LongAdder count = CamelliaMapUtils.computeIfAbsent(resourceCommandBidBgroupMap, key, k -> new LongAdder());
            count.increment();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class ResourceStatsCollect {
        List<ResourceStats> resourceStatsList = new ArrayList<>();
        List<ResourceCommandStats> resourceCommandStatsList = new ArrayList<>();
        List<ResourceBidBgroupStats> resourceBidBgroupStatsList = new ArrayList<>();
        List<ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList = new ArrayList<>();
    }

    public static ResourceStatsCollect collect() {
        count ++;
        ConcurrentHashMap<String, ResourceStats> resourceStatsMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ResourceCommandStats> resourceCommandStatsMap = new ConcurrentHashMap<>();

        ConcurrentHashMap<String, LongAdder> resourceCommandBidBgroupMap = ResourceStatsMonitor.resourceCommandBidBgroupMap;
        if (count >= ProxyDynamicConf.getInt("monitor.cache.reset.interval.periods", 60)) {
            ResourceStatsMonitor.resourceCommandBidBgroupMap = new ConcurrentHashMap<>();
            count = 0;
        }
        List<ResourceBidBgroupCommandStats> resourceBidBgroupCommandStatsList = new ArrayList<>();
        ConcurrentHashMap<String, ResourceBidBgroupStats> resourceBidBgroupStatsMap = new ConcurrentHashMap<>();
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
            long count = entry.getValue().sumThenReset();
            if (count == 0) continue;
            String url = PasswordMaskUtils.maskResource(split[2]);
            String command = split[3];
            ResourceBidBgroupCommandStats stats = new ResourceBidBgroupCommandStats();
            stats.setBid(bid);
            stats.setBgroup(bgroup);
            stats.setResource(url);
            stats.setCommand(command);
            stats.setCount(count);
            resourceBidBgroupCommandStatsList.add(stats);

            ResourceStats resourceStats = CamelliaMapUtils.computeIfAbsent(resourceStatsMap, url, string -> {
                ResourceStats bean = new ResourceStats();
                bean.setResource(string);
                return bean;
            });
            resourceStats.setCount(resourceStats.getCount() + stats.getCount());

            ResourceCommandStats resourceCommandStats = CamelliaMapUtils.computeIfAbsent(resourceCommandStatsMap, url + "|" + command, string -> {
                String[] strings = string.split("\\|");
                ResourceCommandStats bean = new ResourceCommandStats();
                bean.setResource(strings[0]);
                bean.setCommand(strings[1]);
                return bean;
            });
            resourceCommandStats.setCount(resourceCommandStats.getCount() + stats.getCount());

            ResourceBidBgroupStats resourceBidBgroupStats = CamelliaMapUtils.computeIfAbsent(resourceBidBgroupStatsMap, bid + "|" + bgroup + "|" + url, string -> {
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
                ResourceBidBgroupStats bean = new ResourceBidBgroupStats();
                bean.setBid(bid1);
                bean.setBgroup(bgroup1);
                bean.setResource(url1);
                return bean;
            });
            resourceBidBgroupStats.setCount(resourceBidBgroupStats.getCount() + stats.getCount());
        }

        ResourceStatsCollect resourceStats = new ResourceStatsCollect();
        resourceStats.resourceStatsList = new ArrayList<>(resourceStatsMap.values());
        resourceStats.resourceCommandStatsList = new ArrayList<>(resourceCommandStatsMap.values());
        resourceStats.resourceBidBgroupStatsList = new ArrayList<>(resourceBidBgroupStatsMap.values());
        resourceStats.resourceBidBgroupCommandStatsList = resourceBidBgroupCommandStatsList;

        return resourceStats;
    }
}
