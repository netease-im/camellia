package com.netease.nim.camellia.hot.key.server.calculate;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/10
 */
public class TopNStatsResult {

    private String namespace;
    private List<TopNStats> topN;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<TopNStats> getTopN() {
        return topN;
    }

    public void setTopN(List<TopNStats> topN) {
        this.topN = topN;
    }
}
