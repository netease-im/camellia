package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.server.calculate.TopNCounter;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStats;
import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import org.junit.Assert;
import org.junit.Test;

public class TopNCounterTest {

    @Test
    public void shouldCollectTopNByMaxQpsAndKeepActionAndSource() {
        long oldTime = TimeCache.currentMillis;
        try {
            TimeCache.currentMillis = 10000;
            HotKeyServerProperties properties = new HotKeyServerProperties();
            properties.setTopnCount(2);
            properties.setCacheCount(1);
            properties.setTopnCacheCounterCapacity(100);
            properties.setTopnCollectSeconds(2);
            TopNCounter counter = new TopNCounter("namespace", properties);

            counter.update(HotKeyTestFixtures.counter("namespace", "key-a", KeyAction.QUERY, 10), "source-a");
            counter.update(HotKeyTestFixtures.counter("namespace", "key-b", KeyAction.QUERY, 30), "source-b");
            counter.update(HotKeyTestFixtures.counter("namespace", "key-c", KeyAction.UPDATE, 20), "source-c");

            TimeCache.currentMillis += 1000;
            counter.tinyCollect();
            TopNStatsResult result = counter.collect();

            Assert.assertEquals("namespace", result.getNamespace());
            Assert.assertEquals(2, result.getTopN().size());
            Assert.assertEquals("key-b", result.getTopN().get(0).getKey());
            Assert.assertEquals(KeyAction.QUERY, result.getTopN().get(0).getAction());
            Assert.assertEquals(30, result.getTopN().get(0).getTotal());
            Assert.assertTrue(result.getTopN().get(0).getSourceSet().contains("source-b"));

            TopNStats second = result.getTopN().get(1);
            Assert.assertEquals("key-c", second.getKey());
            Assert.assertEquals(KeyAction.UPDATE, second.getAction());
        } finally {
            TimeCache.currentMillis = oldTime;
        }
    }

    @Test
    public void shouldAggregateSameKeyAcrossTinyCollectWindows() {
        long oldTime = TimeCache.currentMillis;
        try {
            TimeCache.currentMillis = 20000;
            HotKeyServerProperties properties = new HotKeyServerProperties();
            properties.setTopnCount(3);
            properties.setCacheCount(1);
            properties.setTopnCacheCounterCapacity(100);
            properties.setTopnCollectSeconds(2);
            TopNCounter counter = new TopNCounter("namespace", properties);

            counter.update(HotKeyTestFixtures.counter("namespace", "key-a", KeyAction.QUERY, 10), "source-a");
            TimeCache.currentMillis += 1000;
            counter.tinyCollect();
            counter.update(HotKeyTestFixtures.counter("namespace", "key-a", KeyAction.QUERY, 20), "source-b");
            TimeCache.currentMillis += 1000;
            counter.tinyCollect();

            TopNStatsResult result = counter.collect();

            Assert.assertEquals(1, result.getTopN().size());
            Assert.assertEquals("key-a", result.getTopN().get(0).getKey());
            Assert.assertEquals(30, result.getTopN().get(0).getTotal());
            Assert.assertTrue(result.getTopN().get(0).getSourceSet().contains("source-a"));
            Assert.assertTrue(result.getTopN().get(0).getSourceSet().contains("source-b"));
        } finally {
            TimeCache.currentMillis = oldTime;
        }
    }
}
