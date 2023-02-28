package com.netease.nim.camellia.external.call.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/27
 */
public class ExternalCallSelectInfo {

    private String isolationKey;
    private List<MqInfo> mqInfoList = new ArrayList<>();

    public String getIsolationKey() {
        return isolationKey;
    }

    public void setIsolationKey(String isolationKey) {
        this.isolationKey = isolationKey;
    }

    public List<MqInfo> getMqInfoList() {
        return mqInfoList;
    }

    public void setMqInfoList(List<MqInfo> mqInfoList) {
        this.mqInfoList = mqInfoList;
    }
}
