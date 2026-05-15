package com.netease.nim.camellia.hotkey.tests;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.model.RuleType;
import com.netease.nim.camellia.hot.key.server.callback.DummyHotKeyCallback;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.event.HotKeyEventHandler;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.hotkey.tests.support.HotKeyTestFixtures;
import com.netease.nim.camellia.hotkey.tests.support.RecordingHotKeyNotifyService;
import com.netease.nim.camellia.hotkey.tests.support.ReflectionTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class HotKeyEventHandlerTest {

    @Test
    public void shouldNotifyHotKeyAndThrottleUpdateEvents() throws Exception {
        RecordingHotKeyNotifyService notifyService = RecordingHotKeyNotifyService.createWithoutConfigService();
        HotKeyEventHandler handler = new HotKeyEventHandler(new HotKeyServerProperties(), notifyService,
                new HotKeyCallbackManager(callbackProperties()));
        Rule rule = HotKeyTestFixtures.rule("all", RuleType.match_all, null, 100, 1, 400);

        try {
            HotKey hotKey = new HotKey("namespace", "key-a", KeyAction.QUERY, 400L);
            handler.newHotKey(hotKey, rule, 2, Collections.singleton("source-a"));
            waitUntilSize(notifyService, 1);
            Assert.assertEquals(KeyAction.QUERY, notifyService.hotKeys().get(0).getAction());

            TimeUnit.MILLISECONDS.sleep(230);
            handler.newHotKey(hotKey, rule, 3, Collections.singleton("source-b"));
            waitUntilSize(notifyService, 2);

            KeyCounter update = HotKeyTestFixtures.counter("namespace", "key-a", KeyAction.UPDATE, 1);
            handler.hotKeyUpdate(update);
            handler.hotKeyUpdate(update);
            waitUntilSize(notifyService, 3);
            TimeUnit.MILLISECONDS.sleep(80);
            Assert.assertEquals(3, notifyService.hotKeys().size());
            Assert.assertEquals(KeyAction.UPDATE, notifyService.hotKeys().get(2).getAction());
        } finally {
            ((CamelliaHashedExecutor) ReflectionTestUtils.getField(handler, "executor")).shutdown();
            shutdownCallbackExecutor(handler);
        }
    }

    private static HotKeyServerProperties callbackProperties() {
        HotKeyServerProperties properties = new HotKeyServerProperties();
        properties.setHotKeyCallbackClassName(DummyHotKeyCallback.class.getName());
        properties.setCallbackExecutorSize(1);
        return properties;
    }

    private static void waitUntilSize(RecordingHotKeyNotifyService notifyService, int size) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (notifyService.hotKeys().size() >= size) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.fail("timeout waiting notify size " + size + ", actual=" + notifyService.hotKeys().size());
    }

    private static void shutdownCallbackExecutor(HotKeyEventHandler handler) {
        HotKeyCallbackManager callbackManager = (HotKeyCallbackManager) ReflectionTestUtils.getField(handler, "callbackManager");
        ((java.util.concurrent.ThreadPoolExecutor) ReflectionTestUtils.getField(callbackManager, "executor")).shutdownNow();
    }
}
