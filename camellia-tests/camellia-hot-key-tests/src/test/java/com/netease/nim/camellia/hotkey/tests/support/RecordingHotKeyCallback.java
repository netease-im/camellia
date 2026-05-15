package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallback;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingHotKeyCallback implements HotKeyCallback {

    private static final List<HotKeyInfo> EVENTS = Collections.synchronizedList(new ArrayList<HotKeyInfo>());

    public static void clear() {
        EVENTS.clear();
    }

    public static List<HotKeyInfo> events() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    @Override
    public void newHotKey(HotKeyInfo hotKeyInfo) {
        EVENTS.add(hotKeyInfo);
    }
}
