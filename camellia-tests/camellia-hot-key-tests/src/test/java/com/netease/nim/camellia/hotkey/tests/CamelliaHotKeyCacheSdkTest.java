package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.ValueLoader;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import com.netease.nim.camellia.hotkey.tests.support.ReflectionTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CamelliaHotKeyCacheSdkTest {

    @Test
    public void shouldCallLoaderWhenConfigIsMissing() {
        CamelliaHotKeyCacheSdk cacheSdk = (CamelliaHotKeyCacheSdk) ReflectionTestUtils.allocate(CamelliaHotKeyCacheSdk.class);
        ReflectionTestUtils.setField(cacheSdk, "config", new CamelliaHotKeyCacheSdkConfig());
        ReflectionTestUtils.setField(cacheSdk, "hotKeyCacheKeyMap", new NamespaceCamelliaLocalCache(100, 100, true));
        ReflectionTestUtils.setField(cacheSdk, "hotKeyCacheValueMap", new NamespaceCamelliaLocalCache(100, 100, false));
        ReflectionTestUtils.setField(cacheSdk, "hotKeyCacheHitLockMap", new NamespaceCamelliaLocalCache(100, 100, false));
        ReflectionTestUtils.setField(cacheSdk, "hotKeyCacheHitStatsMap", new NamespaceCamelliaLocalCache(100, 100, false));
        ReflectionTestUtils.setField(cacheSdk, "hotKeyConfigCache", new ConcurrentHashMap<String, HotKeyConfig>());
        ReflectionTestUtils.setField(cacheSdk, "lastReloadConfigTime", new CamelliaLocalCache());
        ((ConcurrentHashMap<String, HotKeyConfig>) ReflectionTestUtils.getField(cacheSdk, "hotKeyConfigCache"))
                .put("namespace", HotKeyTestFixtures.config("namespace"));

        final AtomicInteger loads = new AtomicInteger();
        String value = cacheSdk.getValue("namespace", "key-a", new ValueLoader<String>() {
            @Override
            public String load(String key) {
                loads.incrementAndGet();
                return "value-" + key;
            }
        });

        Assert.assertEquals("value-key-a", value);
        Assert.assertEquals(1, loads.get());
        Assert.assertFalse(cacheSdk.isHotKey("namespace", "key-a"));
    }
}
