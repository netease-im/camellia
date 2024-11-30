package com.netease.nim.camellia.mq.isolation.core.stats.model;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/19
 */
public class SenderBizStatsRequest {

    private String instanceId;
    private List<SenderBizStats> list;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public List<SenderBizStats> getList() {
        return list;
    }

    public void setList(List<SenderBizStats> list) {
        this.list = list;
    }
}
