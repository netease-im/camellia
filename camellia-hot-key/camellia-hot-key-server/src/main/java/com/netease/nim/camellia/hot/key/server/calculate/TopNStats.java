package com.netease.nim.camellia.hot.key.server.calculate;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;

import java.util.Set;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNStats implements Comparable<TopNStats> {

    private final String key;
    private final KeyAction action;
    private final long total;
    private final long maxQps;
    private final Set<String> sourceSet;

    public TopNStats(String key, KeyAction action, long total, long maxQps, Set<String> sourceSet) {
        this.key = key;
        this.action = action;
        this.total = total;
        this.maxQps = maxQps;
        this.sourceSet = sourceSet;
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

    public Set<String> getSourceSet() {
        return sourceSet;
    }

    @Override
    public int compareTo(TopNStats topNStats) {
        return Long.compare(topNStats.maxQps, maxQps);
    }
}
