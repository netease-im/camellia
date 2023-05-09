package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.HotKeyCounter;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyCounterCollector {

    private final AtomicBoolean backUp = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, LongAdder> map1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> map2 = new ConcurrentHashMap<>();

    public void push(String namespace, String key, KeyAction keyAction) {
        String uniqueKey = namespace + "|" + keyAction.getValue() + "|" + key;
        ConcurrentHashMap<String, LongAdder> map = !backUp.get() ? map1 : map2;
        LongAdder adder = CamelliaMapUtils.computeIfAbsent(map, uniqueKey, k -> new LongAdder());
        adder.increment();
    }

    public synchronized List<HotKeyCounter> collect() {
        List<HotKeyCounter> result = new ArrayList<>();
        if (backUp.get()) {
            if (backUp.compareAndSet(true, false)) {
                result = toResult(map2);
                map2.clear();
            }
        } else {
            if (backUp.compareAndSet(false, true)) {
                result = toResult(map1);
                map1.clear();
            }
        }
        return result;
    }

    private List<HotKeyCounter> toResult(ConcurrentHashMap<String, LongAdder> map) {
        List<HotKeyCounter> result = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
            String uniqueKey = entry.getKey();
            int index1 = uniqueKey.indexOf("|");
            String namespace = uniqueKey.substring(0, index1);
            int index2 = uniqueKey.indexOf("|", index1 + 1);
            KeyAction keyAction = KeyAction.getByValue(Integer.parseInt(uniqueKey.substring(index1 + 1, index2)));
            String key = uniqueKey.substring(index2 + 1);

            HotKeyCounter counter = new HotKeyCounter();
            counter.setNamespace(namespace);
            counter.setKey(key);
            counter.setAction(keyAction);
            counter.setCount(entry.getValue().sum());
            result.add(counter);
        }
        return result;
    }
}
