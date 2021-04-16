package com.netease.nim.camellia.dashboard.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.ResourceStats;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.model.RwStats;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/6/5.
 */
@Service
public class StatsService {

    private static final Logger logger = LoggerFactory.getLogger(StatsService.class);

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private DashboardProperties dashboardProperties;

    private static final String CACHE_TAG = "camellia_stats_";
    private static final String TOTAL_KEYS = "camellia_t_keys";
    private static final String DETAIL_KEYS = "camellia_d_keys";

    public void stats(String ip, ResourceStats resourceStats) {
        long now = System.currentTimeMillis();
        String source = resourceStats.getSource();
        Long bid = resourceStats.getBid();
        String bgroup = resourceStats.getBgroup();
        if (bid == null || bgroup == null) return;
        String uniqueId = source + "|" + ip + "|" + bid + "|" + bgroup;
        List<ResourceStats.Stats> statsList = resourceStats.getStatsList();
        for (ResourceStats.Stats stats : statsList) {
            String resource = stats.getResource();
            String opeType = stats.getOpe();
            long count = stats.getCount();

            if (count <= 0) continue;

            String cacheKey1 = CacheUtil.buildCacheKey(CACHE_TAG, resource, opeType);
            template.hset(cacheKey1, uniqueId, (count) + "|" + now);
            template.expire(cacheKey1, dashboardProperties.getStatsKeyExpireHours() * 3600);

            String cacheKey = CacheUtil.buildCacheKey(CACHE_TAG, TOTAL_KEYS);
            template.hset(cacheKey, cacheKey1, String.valueOf(now));
            template.expire(cacheKey, dashboardProperties.getStatsKeyExpireHours() * 3600);
            if (logger.isDebugEnabled()) {
                logger.debug("bid = {}, bgroup = {}, ip = {}, source = {}, stats = {}", bid, bgroup, ip, source, JSONObject.toJSONString(stats));
            }
        }
        List<ResourceStats.StatsDetail> statsDetailList = resourceStats.getStatsDetailList();
        for (ResourceStats.StatsDetail statsDetail : statsDetailList) {
            String resource = statsDetail.getResource();
            String className = statsDetail.getClassName();
            String methodName = statsDetail.getMethodName();

            long count = statsDetail.getCount();
            String opeType = statsDetail.getOpe();

            if (count <= 0) continue;

            String cacheKey1 = CacheUtil.buildCacheKey(CACHE_TAG, DETAIL_KEYS);

            String cacheKey = CacheUtil.buildCacheKey(CACHE_TAG, resource, className, methodName, opeType);
            template.hset(cacheKey, uniqueId, (count) + "|" + now);
            template.hset(cacheKey1, cacheKey, String.valueOf(now));

            template.expire(cacheKey, dashboardProperties.getStatsKeyExpireHours() * 3600);
            template.expire(cacheKey1, dashboardProperties.getStatsKeyExpireHours() * 3600);
            if (logger.isDebugEnabled()) {
                logger.debug("bid = {}, bgroup = {}, ip = {}, source = {}, statsDetail = {}", bid, bgroup, ip, source, JSONObject.toJSONString(statsDetail));
            }
        }
    }

    public RwStats getStats() {
        List<RwStats.Total> totalList = new ArrayList<>();
        String cacheKey1 = CacheUtil.buildCacheKey(CACHE_TAG, TOTAL_KEYS);
        Map<String, RwStats.BusinessTotal> map1 = new HashMap<>();
        for (Map.Entry<String, String> entry : template.hgetAll(cacheKey1).entrySet()) {
            String key = entry.getKey();
            long timestamp = Long.parseLong(entry.getValue());
            if (isExpire(timestamp)) {
                template.hdel(cacheKey1, key);
            } else {
                String[] split1 = key.split("\\|");
                String resource = split1[1];
                String ope = split1[2];
                long totalCount = 0;
                for (Map.Entry<String, String> stringStringEntry : template.hgetAll(key).entrySet()) {
                    String uniqueId = stringStringEntry.getKey();
                    String[] split = stringStringEntry.getValue().split("\\|");
                    long timestamp1 = Long.parseLong(split[1]);
                    long count = Long.parseLong(split[0]);
                    if (isExpire(timestamp1)) {
                        template.hdel(key, uniqueId);
                        continue;
                    } else {
                        totalCount += count;
                    }
                    String[] split2 = uniqueId.split("\\|");
                    String bid = split2[2];
                    String bgroup = split2[3];
                    String businessKey = bid + "|" + bgroup + "|" + resource + "|" + ope;
                    RwStats.BusinessTotal businessTotal = map1.get(businessKey);
                    if (businessTotal == null) {
                        businessTotal = map1.computeIfAbsent(businessKey, s -> {
                            RwStats.BusinessTotal instance = new RwStats.BusinessTotal();
                            instance.setBid(bid);
                            instance.setBgroup(bgroup);
                            instance.setResource(resource);
                            instance.setOpe(ope);
                            instance.setCount(0);
                            return instance;
                        });
                    }
                    businessTotal.setCount(count + businessTotal.getCount());
                    map1.put(businessKey, businessTotal);
                }
                RwStats.Total total = new RwStats.Total();
                total.setResource(resource);
                total.setOpe(ope);
                total.setCount(totalCount);
                totalList.add(total);
            }
        }

        List<RwStats.Detail> detailList = new ArrayList<>();
        Map<String, RwStats.BusinessDetail> map2 = new HashMap<>();
        String cacheKey2 = CacheUtil.buildCacheKey(CACHE_TAG, DETAIL_KEYS);
        for (Map.Entry<String, String> entry : template.hgetAll(cacheKey2).entrySet()) {
            String key = entry.getKey();
            long timestamp = Long.parseLong(entry.getValue());
            if (isExpire(timestamp)) {
                template.hdel(cacheKey2, key);
            } else {
                String[] split1 = key.split("\\|");
                String resource = split1[1];
                String className = split1[2];
                String methodName = split1[3];
                String ope = split1[4];
                long totalCount = 0;
                for (Map.Entry<String, String> stringStringEntry : template.hgetAll(key).entrySet()) {
                    String uniqueId = stringStringEntry.getKey();
                    String[] split = stringStringEntry.getValue().split("\\|");
                    long count = Long.parseLong(split[0]);
                    long timestamp1 = Long.parseLong(split[1]);
                    if (isExpire(timestamp1)) {
                        template.hdel(key, uniqueId);
                        continue;
                    } else {
                        totalCount += count;
                    }
                    String[] split2 = uniqueId.split("\\|");
                    String bgroup = split2[3];
                    String bid = split2[2];
                    String businessKey = bid + "|" + bgroup + "|" + resource + "|" + className + "|" + methodName + "|" + ope;
                    RwStats.BusinessDetail businessDetail = map2.get(businessKey);
                    if (businessDetail == null) {
                        businessDetail = map2.computeIfAbsent(businessKey, s -> {
                            RwStats.BusinessDetail instance = new RwStats.BusinessDetail();
                            instance.setBid(bid);
                            instance.setBgroup(bgroup);
                            instance.setResource(resource);
                            instance.setClassName(className);
                            instance.setMethodName(methodName);
                            instance.setOpe(ope);
                            instance.setCount(0);
                            return instance;
                        });
                    }
                    businessDetail.setCount(count + businessDetail.getCount());
                    map2.put(businessKey, businessDetail);
                }

                RwStats.Detail detail = new RwStats.Detail();
                detail.setResource(resource);
                detail.setClassName(className);
                detail.setMethodName(methodName);
                detail.setOpe(ope);
                detail.setCount(totalCount);
                detailList.add(detail);
            }
        }

        RwStats stats = new RwStats();
        stats.setTotalList(totalList);
        stats.setDetailList(detailList);
        stats.setBusinessTotalList(new ArrayList<>(map1.values()));
        stats.setBusinessDetailList(new ArrayList<>(map2.values()));
        return stats;
    }

    private boolean isExpire(long timestamp) {
        return System.currentTimeMillis() - timestamp > dashboardProperties.getStatsExpireSeconds() * 1000;
    }
}
