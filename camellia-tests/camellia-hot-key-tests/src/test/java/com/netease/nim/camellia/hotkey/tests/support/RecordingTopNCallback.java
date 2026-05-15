package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyTopNCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingTopNCallback implements HotKeyTopNCallback {

    private static final List<TopNStatsResult> EVENTS = Collections.synchronizedList(new ArrayList<TopNStatsResult>());

    public static void clear() {
        EVENTS.clear();
    }

    public static List<TopNStatsResult> events() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    @Override
    public void topN(TopNStatsResult result) {
        EVENTS.add(result);
    }
}
