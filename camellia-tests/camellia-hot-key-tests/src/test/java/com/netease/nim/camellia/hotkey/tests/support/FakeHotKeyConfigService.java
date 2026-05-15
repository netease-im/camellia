package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyConfigService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeHotKeyConfigService extends HotKeyConfigService {

    private static final Map<String, HotKeyConfig> CONFIGS = new ConcurrentHashMap<>();

    public static void put(HotKeyConfig config) {
        CONFIGS.put(config.getNamespace(), config);
    }

    public static void clear() {
        CONFIGS.clear();
    }

    public static void notifyUpdate(String namespace) {
        new FakeHotKeyConfigService().invokeUpdateForTest(namespace);
    }

    @Override
    public HotKeyConfig get(String namespace) {
        return CONFIGS.get(namespace);
    }

    private void invokeUpdateForTest(String namespace) {
        invokeUpdate(namespace);
    }
}
