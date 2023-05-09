package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.model.HotKeyCounter;

/**
 * 统计namespace下的topN的key
 * Created by caojiajun on 2023/5/9
 */
public class TopNCounter {

    private final String namespace;

    public TopNCounter(String namespace) {
        this.namespace = namespace;
    }

    public void update(HotKeyCounter counter) {

    }
}
