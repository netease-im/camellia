package com.netease.nim.camellia.hot.key.server;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNCounterManager {

    private final ConcurrentLinkedHashMap<String, TopNCounter> topNCounterMap;

    public TopNCounterManager(HotKeyServerProperties properties) {
        this.topNCounterMap = new ConcurrentLinkedHashMap.Builder<String, TopNCounter>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
    }

    public void update(KeyCounter keyCounter) {
        getTopNCounter(keyCounter.getNamespace()).update(keyCounter);
    }

    private TopNCounter getTopNCounter(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(topNCounterMap, namespace, n -> new TopNCounter(namespace));
    }
}
