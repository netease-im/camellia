package com.netease.nim.camellia.hot.key.server.calculate;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.*;

/**
 * 统计namespace下的topN的key
 * Created by caojiajun on 2023/5/9
 */
public class TopNCounter {

    private final String namespace;
    private final int topN;
    private final long checkMillis;
    private final ConcurrentLinkedHashMap<String, Counter> map;

    public TopNCounter(String namespace, HotKeyServerProperties properties) {
        this.namespace = namespace;
        this.topN = properties.getTopnCount();
        this.checkMillis = properties.getTopnCheckMillis();
        this.map = new ConcurrentLinkedHashMap.Builder<String, Counter>()
                .initialCapacity(properties.getTopnCacheCapacity())
                .maximumWeightedCapacity(properties.getTopnCacheCapacity())
                .build();
    }

    /**
     * 线程不安全
     * @param counter counter
     */
    public void update(KeyCounter counter) {
        Counter c = CamelliaMapUtils.computeIfAbsent(map, counter.getKey() + "|" + counter.getAction(), k -> new Counter(checkMillis));
        c.update(counter.getCount());
    }

    public TopNStatsResult collect() {
        TopNStatsResult resp = new TopNStatsResult();
        resp.setNamespace(namespace);
        Map<String, Counter> collectMap = new HashMap<>(map);
        map.clear();
        List<TopNStats> list = new ArrayList<>();
        for (Map.Entry<String, Counter> entry : collectMap.entrySet()) {
            String uniqueKey = entry.getKey();
            int index = uniqueKey.lastIndexOf("|");
            String key = uniqueKey.substring(0, index);
            KeyAction action = KeyAction.getByValue(Integer.parseInt(uniqueKey.substring(index + 1)));
            Counter counter = entry.getValue();
            list.add(new TopNStats(key, action, counter.getTotal(), counter.getMax()));
        }
        Collections.sort(list);
        List<TopNStats> topNStats = list.subList(0, topN);
        resp.setTopN(topNStats);
        return resp;
    }

    private static class Counter {
        private final long checkMillis;
        private long total;
        private long max;
        private long time = TimeCache.currentMillis;
        private long current;

        public Counter(long checkMillis) {
            this.checkMillis = checkMillis;
        }

        public void update(long c) {
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
        }

        public long getTotal() {
            return total;
        }

        public long getMax() {
            return max;
        }
    }
}
