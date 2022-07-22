package com.netease.nim.camellia.delayqueue.sdk.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2022/7/13
 */
public class LocalConfDelayQueueServerDiscovery implements DelayQueueServerDiscovery {

    private final List<DelayQueueServer> list;

    public LocalConfDelayQueueServerDiscovery(String url) {
        List<DelayQueueServer> list = new ArrayList<>();
        list.add(new DelayQueueServer(url));
        this.list = list;
    }

    public LocalConfDelayQueueServerDiscovery(List<DelayQueueServer> list) {
        this.list = list;
    }

    @Override
    public List<DelayQueueServer> findAll() {
        return list;
    }

    @Override
    public void setCallback(Callback<DelayQueueServer> callback) {

    }

    @Override
    public void clearCallback(Callback<DelayQueueServer> callback) {

    }
}
