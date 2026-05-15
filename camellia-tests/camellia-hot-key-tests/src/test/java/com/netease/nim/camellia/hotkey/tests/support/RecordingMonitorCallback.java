package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.server.callback.MonitorCallback;
import com.netease.nim.camellia.hot.key.server.monitor.HotKeyServerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingMonitorCallback implements MonitorCallback {

    private static final List<HotKeyServerStats> EVENTS = Collections.synchronizedList(new ArrayList<HotKeyServerStats>());

    public static void clear() {
        EVENTS.clear();
    }

    public static List<HotKeyServerStats> events() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    @Override
    public void serverStats(HotKeyServerStats serverStats) {
        EVENTS.add(serverStats);
    }
}
