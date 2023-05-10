package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNStats implements Comparable<TopNStats> {

    private final String key;
    private final KeyAction action;
    private final long total;
    private final long maxQps;

    public TopNStats(String key, KeyAction action, long total, long maxQps) {
        this.key = key;
        this.action = action;
        this.total = total;
        this.maxQps = maxQps;
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

    public long getMaxQps() {
        return maxQps;
    }

    @Override
    public int compareTo(TopNStats topNStats) {
        return Long.compare(topNStats.maxQps, maxQps);
    }
}
