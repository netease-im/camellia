package com.netease.nim.camellia.hot.key.server.calculate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统计namespace下的topN的key
 * Created by caojiajun on 2023/5/9
 */
public class TopNCounter {

    private static final Logger logger = LoggerFactory.getLogger(TopNCounter.class);

    private final String namespace;
    private final int topN;
    private final long checkMillis = 1000;//固定为1s
    private final List<Cache<String, Counter>> cacheList;
    private final int bizWorkThread;
    private final boolean is2Power;
    private final List<Stats> buffer;

    public TopNCounter(String namespace, ScheduledThreadPoolExecutor scheduler, HotKeyServerProperties properties) {
        this.namespace = namespace;
        this.topN = properties.getTopnCount();
        this.bizWorkThread = properties.getBizWorkThread();
        this.is2Power = MathUtil.is2Power(bizWorkThread);
        this.cacheList = new ArrayList<>(bizWorkThread);
        for (int i=0; i<bizWorkThread; i++) {
            Cache<String, Counter> cache = Caffeine.newBuilder()
                    .initialCapacity(properties.getTopnCacheCounterCapacity())
                    .maximumSize(properties.getTopnCacheCounterCapacity())
                    .build();
            cacheList.add(cache);
        }
        this.buffer = new ArrayList<>(properties.getTopnCollectSeconds() * properties.getTopnCount());
        scheduler.scheduleAtFixedRate(this::schedule, checkMillis * properties.getTopnTinyCollectSeconds(),
                checkMillis * properties.getTopnTinyCollectSeconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * 线程不安全
     * @param counter counter
     * @param source source
     */
    public void update(KeyCounter counter, String source) {
        int code = Math.abs((counter.getNamespace() + "|" + counter.getKey()).hashCode());
        int index = MathUtil.mod(is2Power, code, bizWorkThread);
        Counter c = cacheList.get(index).get(counter.getKey() + "|" + counter.getAction().getValue(), k -> new Counter(checkMillis));
        if (c == null) return;
        c.update(counter.getCount(), source);
    }

    public synchronized TopNStatsResult collect() {
        TopNStatsResult result = new TopNStatsResult();
        result.setNamespace(namespace);
        Map<String, Stats> map = new HashMap<>();
        for (Stats stats : buffer) {
            Stats oldStats = CamelliaMapUtils.computeIfAbsent(map, stats.getKey() + "|" + stats.getAction(),
                    k -> new Stats(stats.getKey(), stats.getAction(), 0, 0, new HashSet<>()));
            oldStats.setTotal(oldStats.getTotal() + stats.getTotal());
            if (stats.getMaxQps() > oldStats.getMaxQps()) {
                oldStats.setMaxQps(stats.maxQps);
            }
            if (stats.getSourceSet() != null) {
                oldStats.getSourceSet().addAll(stats.getSourceSet());
            }
        }
        buffer.clear();
        List<Stats> list = new ArrayList<>(map.values());
        Collections.sort(list);
        List<Stats> subList;
        if (list.size() <= topN) {
            subList = list;
        } else {
            subList = list.subList(0, topN);
        }
        List<TopNStats> topN = subList.stream().map(Stats::toTopNStats)
                .collect(Collectors.toList());
        result.setTopN(topN);
        return result;
    }

    private synchronized void schedule() {
        try {
            List<Stats> list = collect0();
            buffer.addAll(list);
        } catch (Exception e) {
            logger.error("schedule error, namespace = {}", namespace, e);
        }
    }

    public List<Stats> collect0() {
        Map<String, Counter> collectMap = new HashMap<>();
        for (Cache<String, Counter> cache : cacheList) {
            collectMap.putAll(cache.asMap());
            cache.invalidateAll();
        }
        List<Stats> list = new ArrayList<>();
        for (Map.Entry<String, Counter> entry : collectMap.entrySet()) {
            String uniqueKey = entry.getKey();
            int index = uniqueKey.lastIndexOf("|");
            String key = uniqueKey.substring(0, index);
            KeyAction action = KeyAction.getByValue(Integer.parseInt(uniqueKey.substring(index + 1)));
            Counter counter = entry.getValue();
            list.add(new Stats(key, action, counter.getTotal(), counter.getMax(), counter.getSourceSet()));
        }
        Collections.sort(list);
        if (list.size() <= topN) {
            return list;
        }
        return list.subList(0, topN);
    }

    private static class Stats implements Comparable<Stats> {
        private String key;
        private KeyAction action;
        private long total;
        private long maxQps;
        private Set<String> sourceSet;

        public Stats(String key, KeyAction action, long total, long maxQps, Set<String> sourceSet) {
            this.key = key;
            this.action = action;
            this.total = total;
            this.maxQps = maxQps;
            this.sourceSet = sourceSet;
        }

        public TopNStats toTopNStats() {
            return new TopNStats(key, action, total, maxQps, sourceSet);
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public KeyAction getAction() {
            return action;
        }

        public void setAction(KeyAction action) {
            this.action = action;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getMaxQps() {
            return maxQps;
        }

        public void setMaxQps(long maxQps) {
            this.maxQps = maxQps;
        }

        public Set<String> getSourceSet() {
            return sourceSet;
        }

        public void setSourceSet(Set<String> sourceSet) {
            this.sourceSet = sourceSet;
        }

        @Override
        public int compareTo(Stats stats) {
            return Long.compare(stats.maxQps, maxQps);
        }
    }

    private static class Counter {
        private final long checkMillis;
        private long total;
        private long max;
        private long time = TimeCache.currentMillis;
        private long current;
        private Set<String> sourceSet;

        public Counter(long checkMillis) {
            this.checkMillis = checkMillis;
        }

        public void update(long c, String source) {
            this.total += c;
            long step = (TimeCache.currentMillis - time) / checkMillis;
            if (step == 1) {
                if (current > max) {
                    max = current;
                }
                current = 0;
                time = TimeCache.currentMillis;
            } else if (step > 0) {
                current = 0;
                time = TimeCache.currentMillis;
            }
            this.current += c;
            if (step == 0 && current > max) {
                max = current;
            }

            if (source != null) {
                if (sourceSet == null) {
                    sourceSet = new HashSet<>();
                }
                if (sourceSet.size() >= HotKeyConstants.Server.maxHotKeySourceSetSize) {
                    sourceSet.clear();
                }
                sourceSet.add(source);
            }
        }

        public long getTotal() {
            return total;
        }

        public long getMax() {
            return max;
        }

        public Set<String> getSourceSet() {
            return sourceSet;
        }
    }
}
