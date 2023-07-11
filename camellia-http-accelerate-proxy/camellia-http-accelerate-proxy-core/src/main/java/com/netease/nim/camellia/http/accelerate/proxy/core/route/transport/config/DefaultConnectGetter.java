package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultConnectGetter implements DynamicValueGetter<Integer> {

    private int connect;

    public DefaultConnectGetter(int connect) {
        this.connect = connect;
    }

    @Override
    public Integer get() {
        return connect;
    }

    public void updateConnect(int connect) {
        this.connect = connect;
    }
}
