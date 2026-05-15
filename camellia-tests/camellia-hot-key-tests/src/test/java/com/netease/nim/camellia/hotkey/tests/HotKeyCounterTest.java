package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.calculate.HotKeyCounter;
import com.netease.nim.camellia.hot.key.server.utils.TimeCache;
import org.junit.Assert;
import org.junit.Test;

public class HotKeyCounterTest {

    @Test
    public void shouldCountWithinSlidingWindowAndResetAfterWindow() {
        long oldTime = TimeCache.currentMillis;
        try {
            TimeCache.currentMillis = 1000;
            HotKeyCounter counter = new HotKeyCounter(300);

            Assert.assertEquals(2, counter.update(2, "source-a"));
            Assert.assertEquals(5, counter.update(3, "source-b"));
            Assert.assertTrue(counter.getSourceSet().contains("source-a"));
            Assert.assertTrue(counter.getSourceSet().contains("source-b"));

            TimeCache.currentMillis += 100;
            Assert.assertEquals(6, counter.update(1, "source-c"));

            TimeCache.currentMillis += 400;
            Assert.assertEquals(4, counter.update(4, "source-d"));
        } finally {
            TimeCache.currentMillis = oldTime;
        }
    }

    @Test
    public void shouldClearSourceSetWhenItReachesLimit() {
        long oldTime = TimeCache.currentMillis;
        try {
            TimeCache.currentMillis = 2000;
            HotKeyCounter counter = new HotKeyCounter(300);
            for (int i = 0; i < HotKeyConstants.Server.maxHotKeySourceSetSize + 1; i++) {
                counter.update(1, "source-" + i);
            }

            Assert.assertNotNull(counter.getSourceSet());
            Assert.assertTrue(counter.getSourceSet().size() < HotKeyConstants.Server.maxHotKeySourceSetSize);
            Assert.assertTrue(counter.getSourceSet().contains("source-" + HotKeyConstants.Server.maxHotKeySourceSetSize));
        } finally {
            TimeCache.currentMillis = oldTime;
        }
    }
}
