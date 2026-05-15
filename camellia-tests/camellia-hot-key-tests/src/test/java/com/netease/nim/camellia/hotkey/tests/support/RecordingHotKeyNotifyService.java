package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.notify.HotKeyNotifyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingHotKeyNotifyService extends HotKeyNotifyService {

    private List<HotKey> hotKeys;
    private List<String> configNamespaces;

    public RecordingHotKeyNotifyService(CacheableHotKeyConfigService hotKeyConfigService) {
        super(hotKeyConfigService);
        init();
    }

    public static RecordingHotKeyNotifyService createWithoutConfigService() {
        RecordingHotKeyNotifyService service = (RecordingHotKeyNotifyService) ReflectionTestUtils.allocate(RecordingHotKeyNotifyService.class);
        service.init();
        return service;
    }

    private void init() {
        this.hotKeys = Collections.synchronizedList(new ArrayList<HotKey>());
        this.configNamespaces = Collections.synchronizedList(new ArrayList<String>());
    }

    @Override
    public void notifyHotKeyNotifyChange(String namespace) {
        configNamespaces.add(namespace);
    }

    @Override
    public void notifyHotKey(HotKey hotKey) {
        hotKeys.add(hotKey);
    }

    public List<HotKey> hotKeys() {
        synchronized (hotKeys) {
            return new ArrayList<>(hotKeys);
        }
    }

    public List<String> configNamespaces() {
        synchronized (configNamespaces) {
            return new ArrayList<>(configNamespaces);
        }
    }
}
