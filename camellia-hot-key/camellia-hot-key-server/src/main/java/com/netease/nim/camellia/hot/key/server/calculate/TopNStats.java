package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNStats implements Comparable<TopNStats> {

    private final String key;
    private final KeyAction action;
    private final long total;
    private final long max;

    public TopNStats(String key, KeyAction action, long total, long max) {
        this.key = key;
        this.action = action;
        this.total = total;
        this.max = max;
    }

    public String getKey() {
        return key;
    }

    public KeyAction getAction() {
        return action;
    }

    public long getTotal() {
        return total;
    }

    public long getMax() {
        return max;
    }

    @Override
    public int compareTo(TopNStats topNStats) {
        return Long.compare(topNStats.max, max);
    }
}
