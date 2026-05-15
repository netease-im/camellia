package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyConfigRepPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.NotifyHotKeyRepPack;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyClientListener;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyPackBizClientHandler;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import com.netease.nim.camellia.hotkey.tests.support.ReflectionTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HotKeyPackBizClientHandlerTest {

    @Test
    public void shouldDispatchHotKeyAndConfigNotifications() throws Exception {
        HotKeyPackBizClientHandler handler = new HotKeyPackBizClientHandler(1, 100);
        AtomicReference<HotKey> hotKeyRef = new AtomicReference<>();
        AtomicReference<HotKeyConfig> configRef = new AtomicReference<>();
        handler.registerListener(new HotKeyClientListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                hotKeyRef.set(hotKey);
            }

            @Override
            public void onHotKeyConfig(HotKeyConfig hotKeyConfig) {
                configRef.set(hotKeyConfig);
            }
        });

        try {
            HotKey hotKey = new HotKey("namespace", "key-a", KeyAction.QUERY, 1000L);
            CompletableFuture<NotifyHotKeyRepPack> hotKeyFuture = handler.onNotifyHotKeyPack(null,
                    new NotifyHotKeyPack(Collections.singletonList(hotKey)));
            Assert.assertSame(NotifyHotKeyRepPack.INSTANCE, hotKeyFuture.get(2, TimeUnit.SECONDS));
            Assert.assertEquals("key-a", hotKeyRef.get().getKey());

            HotKeyConfig config = HotKeyTestFixtures.config("namespace",
                    HotKeyTestFixtures.rule("all", RuleType.match_all, null, 100, 1, 1000));
            CompletableFuture<NotifyHotKeyConfigRepPack> configFuture = handler.onNotifyHotKeyConfigPack(null,
                    new NotifyHotKeyConfigPack(config));
            Assert.assertSame(NotifyHotKeyConfigRepPack.INSTANCE, configFuture.get(2, TimeUnit.SECONDS));
            Assert.assertSame(config, configRef.get());
        } finally {
            ((ThreadPoolExecutor) ReflectionTestUtils.getField(handler, "executor")).shutdownNow();
        }
    }
}
