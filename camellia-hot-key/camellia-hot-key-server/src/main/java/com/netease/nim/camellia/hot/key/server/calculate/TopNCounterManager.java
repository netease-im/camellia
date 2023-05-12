package com.netease.nim.camellia.hot.key.server.calculate;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.monitor.TopNMonitor;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNCounterManager {

    private static final Logger logger = LoggerFactory.getLogger(TopNCounterManager.class);

    private static final String TAG = "topN";

    private final HotKeyServerProperties properties;

    private final ConcurrentLinkedHashMap<String, TopNCounter> topNCounterMap;
    private final ScheduledExecutorService callbackScheduler;

    private final CamelliaRedisTemplate template;

    private final HotKeyCallbackManager callbackManager;
    private final ScheduledThreadPoolExecutor scheduler;

    public TopNCounterManager(HotKeyServerProperties properties, HotKeyCallbackManager callbackManager) {
        this.properties = properties;
        this.callbackManager = callbackManager;
        this.template = properties.getRedisTemplate();
        this.topNCounterMap = new ConcurrentLinkedHashMap.Builder<String, TopNCounter>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
        //找到最近的整秒的中间位置
        //这样不同节点的执行时间就接近了
        long nearestTime = (System.currentTimeMillis() / properties.getTopnCollectSeconds()) * properties.getTopnCollectSeconds()
                + (properties.getTopnCollectSeconds() / 2) * 1000L;
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-hot-key-topn-collect"))
                .scheduleAtFixedRate(this::scheduleCollect, nearestTime - System.currentTimeMillis(),
                        properties.getTopnCollectSeconds() * 1000L, TimeUnit.MILLISECONDS);
        callbackScheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-hot-key-topn-callback-scheduler"));
        scheduler = new ScheduledThreadPoolExecutor(SysUtils.getCpuHalfNum(), new CamelliaThreadFactory("camella-hot-key-topn-scheduler"));

        TopNMonitor.register(this);
    }

    /**
     * 线程不安全
     * @param keyCounter keyCounter
     */
    public void update(KeyCounter keyCounter) {
        getTopNCounter(keyCounter.getNamespace()).update(keyCounter);
    }

    /**
     * 最近一次的获取topN配置
     * @param namespace namespace
     * @param backtrack 回溯几个周期
     * @return TopNStatsResult
     */
    public TopNStatsResult getTopNStats(String namespace, int backtrack) {
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd#HH:mm:ss");
        long time = (System.currentTimeMillis() / (properties.getTopnCollectSeconds() * 1000L)) * properties.getTopnCollectSeconds() * 1000L;
        time = time - properties.getTopnCollectSeconds() * backtrack * 1000L;
        String timeKey = dataFormat.format(new Date(time));
        logger.info("getTopNStats, namespace = {}, timeKey = {}", namespace, timeKey);
        String key = mergeKey(namespace, timeKey);
        Set<String> set = template.zrevrange(key, 0, -1);
        TopNStatsResult result = new TopNStatsResult();
        result.setTime(timeKey);
        result.setNamespace(namespace);
        List<TopNStats> topN = new ArrayList<>();
        for (String str : set) {
            TopNStats topNStats = JSONObject.parseObject(str, TopNStats.class);
            topN.add(topNStats);
        }
        result.setTopN(topN);
        return result;
    }

    private TopNCounter getTopNCounter(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(topNCounterMap, namespace, n -> new TopNCounter(namespace, scheduler, properties));
    }

    private void scheduleCollect() {
        try {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd#HH:mm:ss");
            long time = (System.currentTimeMillis() / (properties.getTopnCollectSeconds() * 1000L)) * properties.getTopnCollectSeconds() * 1000L;
            String timeKey = dataFormat.format(new Date(time));
            Set<String> namespaceSet = new HashSet<>(topNCounterMap.keySet());
            List<TopNStatsResult> results = new ArrayList<>();
            for (String namespace : namespaceSet) {
                TopNCounter counter = topNCounterMap.get(namespace);
                if (counter != null) {
                    results.add(counter.collect());
                }
            }
            String namespaceKeys = namespaceKeys(timeKey);
            for (TopNStatsResult result : results) {
                try {
                    template.zadd(namespaceKeys, System.currentTimeMillis(), result.getNamespace());
                    template.expire(namespaceKeys, properties.getTopnRedisExpireSeconds());
                    String key = mergeKey(result.getNamespace(), timeKey);
                    List<TopNStats> topN = result.getTopN();
                    Map<String, Double> scoreMembers = new HashMap<>();
                    for (TopNStats stats : topN) {
                        scoreMembers.put(JSONObject.toJSONString(stats), (double) stats.getMaxQps());
                        if (scoreMembers.size() >= 100) {
                            template.zadd(key, scoreMembers);
                            scoreMembers.clear();
                        }
                    }
                    if (!scoreMembers.isEmpty()) {
                        template.zadd(key, scoreMembers);
                    }
                    template.zremrangeByRank(key, 0, - properties.getTopnCount() - 1);
                    template.expire(key, properties.getTopnRedisExpireSeconds());
                } catch (Exception e) {
                    logger.error("write topn stats to redis error, namespace = {}, timeKey = {}", result.getNamespace(), timeKey, e);
                }
            }
            logger.info("write topn stats to redis success, namespace.size = {}, timeKey = {}", results.size(), timeKey);
            //延迟一下再发callback，让大家都写完redis
            callbackScheduler.schedule(() -> callback(timeKey), properties.getTopnCollectSeconds() / 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("hot key topn schedule error", e);
        }
    }

    private void callback(String timeKey) {
        try {
            logger.info("try callback hot key topn stats, timeKey = {}", timeKey);
            String namespaceKeys = namespaceKeys(timeKey);
            Long namespaceNum = template.zcard(namespaceKeys);
            if (namespaceNum != null && namespaceNum > 0) {
                for (int i = 0; i < namespaceNum; i += 100) {
                    Set<String> namespaceSet = template.zrange(namespaceKeys, i, i + 99);
                    for (String namespace : namespaceSet) {
                        String lockKey = lockKey(namespace, timeKey);
                        long lockExpireMillis = properties.getTopnCollectSeconds() * 2 * 1000L;
                        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, lockExpireMillis, lockExpireMillis);
                        if (lock.tryLock()) {
                            try {
                                String key = mergeKey(namespace, timeKey);
                                Set<String> set = template.zrevrange(key, 0, -1);
                                TopNStatsResult result = new TopNStatsResult();
                                result.setNamespace(namespace);
                                result.setTime(timeKey);
                                List<TopNStats> topN = new ArrayList<>();
                                for (String str : set) {
                                    TopNStats topNStats = JSONObject.parseObject(str, TopNStats.class);
                                    topN.add(topNStats);
                                }
                                result.setTopN(topN);
                                callbackManager.topN(result);
                            } catch (Exception e) {
                                logger.error("hot key topn callback error, namespace = {}", namespace, e);
                                lock.release();
                            }
                        }
                    }
                }
            }
            logger.info("callback hot key topn stats done, timeKey = {}", timeKey);
        } catch (Exception e) {
            logger.error("hot key topn callback error", e);
        }
    }

    private String lockKey(String namespace, String timeKey) {
        return CacheUtil.buildCacheKey(properties.getTopnRedisKeyPrefix(), TAG, timeKey, namespace, "notify~lock");
    }

    private String namespaceKeys(String timeKey) {
        return CacheUtil.buildCacheKey(properties.getTopnRedisKeyPrefix(), TAG, timeKey, "namespace");
    }

    private String mergeKey(String namespace, String timeKey) {
        return CacheUtil.buildCacheKey(properties.getTopnRedisKeyPrefix(), TAG, namespace, timeKey);
    }
}
