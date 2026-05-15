package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.sdk.collect.CaffeineCollector;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaffeineCollectorTest {

    @Test
    public void shouldAggregateCountersByNamespaceKeyAndAction() {
        CaffeineCollector collector = new CaffeineCollector(100);

        collector.push("namespace-a", "key-1", KeyAction.QUERY, 2);
        collector.push("namespace-a", "key-1", KeyAction.QUERY, 3);
        collector.push("namespace-a", "key-1", KeyAction.UPDATE, 5);
        collector.push("namespace-b", "key-2", KeyAction.DELETE, 7);

        List<KeyCounter> counters = collector.collect();
        Map<String, Long> map = toMap(counters);

        Assert.assertEquals(3, counters.size());
        Assert.assertEquals(Long.valueOf(5), map.get("namespace-a|key-1|QUERY"));
        Assert.assertEquals(Long.valueOf(5), map.get("namespace-a|key-1|UPDATE"));
        Assert.assertEquals(Long.valueOf(7), map.get("namespace-b|key-2|DELETE"));
        Assert.assertTrue(collector.collect().isEmpty());
    }

    private static Map<String, Long> toMap(List<KeyCounter> counters) {
        Map<String, Long> map = new HashMap<>();
        for (KeyCounter counter : counters) {
            map.put(counter.getNamespace() + "|" + counter.getKey() + "|" + counter.getAction(), counter.getCount());
        }
        return map;
    }
}
