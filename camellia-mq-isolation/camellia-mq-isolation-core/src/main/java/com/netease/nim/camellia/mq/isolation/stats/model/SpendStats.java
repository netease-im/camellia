package com.netease.nim.camellia.mq.isolation.stats.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/2/7
 */
public class SpendStats {
    private List<NamespaceSpendStats> spendStatsList = new ArrayList<>();
    private List<NamespaceBizIdSpendStats> bizIdSpendStatsList = new ArrayList<>();

    public List<NamespaceSpendStats> getSpendStatsList() {
        return spendStatsList;
    }

    public void setSpendStatsList(List<NamespaceSpendStats> spendStatsList) {
        this.spendStatsList = spendStatsList;
    }

    public List<NamespaceBizIdSpendStats> getBizIdSpendStatsList() {
        return bizIdSpendStatsList;
    }

    public void setBizIdSpendStatsList(List<NamespaceBizIdSpendStats> bizIdSpendStatsList) {
        this.bizIdSpendStatsList = bizIdSpendStatsList;
    }
}
