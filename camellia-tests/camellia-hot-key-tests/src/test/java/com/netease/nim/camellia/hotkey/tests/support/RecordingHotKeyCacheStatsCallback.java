package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCacheStats;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCacheStatsCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final List<List<HotKeyCacheStats>> EVENTS = Collections.synchronizedList(new ArrayList<List<HotKeyCacheStats>>());

    public static void clear() {
        EVENTS.clear();
    }

    public static List<List<HotKeyCacheStats>> events() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    @Override
    public void newStats(List<HotKeyCacheStats> statsList) {
        EVENTS.add(new ArrayList<>(statsList));
    }
}
