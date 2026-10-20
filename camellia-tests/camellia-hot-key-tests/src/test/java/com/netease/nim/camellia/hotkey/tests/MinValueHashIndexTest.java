package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.server.calculate.HotKeyCounterManager;
import com.netease.nim.camellia.hot.key.server.calculate.TopNCounter;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import org.junit.Assert;
import org.junit.Test;

public class MinValueHashIndexTest {

    // hashCode() == Integer.MIN_VALUE, for which Math.abs overflows and stays negative
    private static final String MIN_HASH_KEY = "I=%xe~";

    private HotKeyServerProperties properties() {
        HotKeyServerProperties properties = new HotKeyServerProperties();
        properties.setCacheCount(3); // not a power of two, so MathUtil.mod uses '%'
        return properties;
    }

    @Test
    public void hotKeyCounterShouldAcceptMinValueHashCodeKey() {
        HotKeyCounterManager manager = new HotKeyCounterManager(properties());
        Rule rule = new Rule();
        rule.setCheckMillis(1000L);

        Assert.assertNotNull(manager.getHotKeyCounter("ns", MIN_HASH_KEY, rule));
        Assert.assertNotNull(manager.getHotKeyCounter("ns", "ordinary-key", rule));
    }

    @Test
    public void topNCounterShouldAcceptMinValueHashCodeKey() {
        TopNCounter counter = new TopNCounter("ns", properties());

        counter.update(keyCounter(MIN_HASH_KEY), "test");
        counter.update(keyCounter("ordinary-key"), "test");
    }

    private KeyCounter keyCounter(String key) {
        KeyCounter keyCounter = new KeyCounter();
        keyCounter.setNamespace("ns");
        keyCounter.setKey(key);
        keyCounter.setAction(KeyAction.QUERY);
        keyCounter.setCount(1);
        return keyCounter;
    }
}
