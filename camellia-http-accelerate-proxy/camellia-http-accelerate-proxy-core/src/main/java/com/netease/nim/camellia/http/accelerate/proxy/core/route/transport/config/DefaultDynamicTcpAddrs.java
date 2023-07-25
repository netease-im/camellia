package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.DynamicAddrs;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultDynamicTcpAddrs implements DynamicAddrs {

    private List<ServerAddr> addrs;

    public DefaultDynamicTcpAddrs(List<ServerAddr> addrs) {
        this.addrs = addrs;
    }

    @Override
    public List<ServerAddr> getAddrs() {
        return addrs;
    }

    public void updateAddrs(List<ServerAddr> addrs) {
        this.addrs = addrs;
    }
}
