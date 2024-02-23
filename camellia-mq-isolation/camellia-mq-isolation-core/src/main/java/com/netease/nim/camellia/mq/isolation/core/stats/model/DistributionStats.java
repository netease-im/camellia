package com.netease.nim.camellia.mq.isolation.core.stats.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/2/23
 */
public class DistributionStats {
    private List<NamespaceBizIdDistributionStats> distributionStatsList = new ArrayList<>();

    public List<NamespaceBizIdDistributionStats> getDistributionStatsList() {
        return distributionStatsList;
    }

    public void setDistributionStatsList(List<NamespaceBizIdDistributionStats> distributionStatsList) {
        this.distributionStatsList = distributionStatsList;
    }
}
