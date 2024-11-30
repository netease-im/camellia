package com.netease.nim.camellia.mq.isolation.core.stats.model;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/19
 */
public class ConsumerBizStatsRequest {

    private String instanceId;
    private List<ConsumerBizStats> list;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public List<ConsumerBizStats> getList() {
        return list;
    }

    public void setList(List<ConsumerBizStats> list) {
        this.list = list;
    }
}
