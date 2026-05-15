package com.netease.nim.camellia.hotkey.tests.support;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.hot.key.sdk.collect.IHotKeyCounterCollector;

import java.util.ArrayList;
import java.util.List;

public class RecordingCollector implements IHotKeyCounterCollector {

    private final List<KeyCounter> counters = new ArrayList<>();

    @Override
    public synchronized void push(String namespace, String key, KeyAction keyAction, long count) {
        counters.add(HotKeyTestFixtures.counter(namespace, key, keyAction, count));
    }

    @Override
    public synchronized List<KeyCounter> collect() {
        List<KeyCounter> copy = new ArrayList<>(counters);
        counters.clear();
        return copy;
    }

    public synchronized List<KeyCounter> counters() {
        return new ArrayList<>(counters);
    }
}
