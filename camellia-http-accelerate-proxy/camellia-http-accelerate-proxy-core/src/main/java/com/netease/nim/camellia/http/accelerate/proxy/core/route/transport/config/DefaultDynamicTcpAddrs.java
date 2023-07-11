package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.DynamicTcpAddrs;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.TcpAddr;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultDynamicTcpAddrs implements DynamicTcpAddrs {

    private List<TcpAddr> addrs;

    public DefaultDynamicTcpAddrs(List<TcpAddr> addrs) {
        this.addrs = addrs;
    }

    @Override
    public List<TcpAddr> getAddrs() {
        return addrs;
    }

    public void updateAddrs(List<TcpAddr> addrs) {
        this.addrs = addrs;
    }
}
