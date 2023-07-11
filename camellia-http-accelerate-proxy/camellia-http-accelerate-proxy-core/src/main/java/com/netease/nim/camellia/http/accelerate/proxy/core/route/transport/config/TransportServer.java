package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/10
 */
public class TransportServer {
    private String server;
    private TransportServerType type;
    private List<String> addrs;
    private int connect = 1;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public TransportServerType getType() {
        return type;
    }

    public void setType(TransportServerType type) {
        this.type = type;
    }

    public List<String> getAddrs() {
        return addrs;
    }

    public void setAddrs(List<String> addrs) {
        this.addrs = addrs;
    }

    public int getConnect() {
        return connect;
    }

    public void setConnect(int connect) {
        this.connect = connect;
    }
}
