package com.netease.nim.camellia.hot.key.server.monitor;


import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyCalculatorMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCalculatorMonitor.class);

    private Map<String, Counter> map = new HashMap<>();
    private final Counter total = new Counter();

    public void updateNormal(String namespace, long count) {
        update(namespace, TrafficStats.Type.NORMAL, count);
    }

    public void updateRuleNotMatch(String namespace, long count) {
        update(namespace, TrafficStats.Type.RULE_NOT_MATCH, count);
    }

    public void updateHot(String namespace, long count) {
        update(namespace, TrafficStats.Type.HOT, count);
    }

    private void update(String namespace, TrafficStats.Type type, long count) {
        try {
            String key = namespace + "|" + type.getValue();
            Counter counter = CamelliaMapUtils.computeIfAbsent(map, key, k -> new Counter());
            counter.update(count);
            total.update(count);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public TrafficStats collect() {
        TrafficStats trafficStats = new TrafficStats();
        try {
            trafficStats.setTotal(total.getCount());
            total.reset();
            Map<String, Counter> map = this.map;
            this.map = new HashMap<>();
            List<TrafficStats.Stats> statsList = new ArrayList<>();
            for (Map.Entry<String, Counter> entry : map.entrySet()) {
                String key = entry.getKey();
                int index = key.lastIndexOf("|");
                String namespace = key.substring(0, index);
                TrafficStats.Type type = TrafficStats.Type.getByValue(Integer.parseInt(key.substring(index + 1)));

                TrafficStats.Stats stats = new TrafficStats.Stats();
                stats.setNamespace(namespace);
                stats.setType(type);
                stats.setCount(entry.getValue().getCount());
                statsList.add(stats);
            }
            trafficStats.setStatsList(statsList);
            return trafficStats;
        } catch (Exception e) {
            logger.error("collect error", e);
            return trafficStats;
        }
    }

    private static class Counter {
        private long count;

        public void update(long count) {
            this.count += count;
        }

        public long getCount() {
            return count;
        }

        public void reset() {
            count = 0;
        }
    }
}
