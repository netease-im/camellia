package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.KeyCounter;

/**
 * 统计namespace下的topN的key
 * Created by caojiajun on 2023/5/9
 */
public class TopNCounter {

    private final String namespace;

    public TopNCounter(String namespace) {
        this.namespace = namespace;
    }

    public void update(KeyCounter counter) {

    }
}
