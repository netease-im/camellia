package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultDynamicUpstreamAddrs implements DynamicUpstreamAddrs {

    private List<String> addrs;

    public DefaultDynamicUpstreamAddrs(List<String> addrs) {
        this.addrs = addrs;
    }

    @Override
    public List<String> getAddrs() {
        return addrs;
    }

    public void updateAddrs(List<String> addrs) {
        this.addrs = addrs;
    }
}
