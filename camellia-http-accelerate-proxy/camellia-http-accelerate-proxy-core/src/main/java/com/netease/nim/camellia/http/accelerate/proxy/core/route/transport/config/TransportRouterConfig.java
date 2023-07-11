package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;


import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TransportRouterConfig {
    private List<TransportServer> servers;
    private List<TransportRoute> routes;

    public List<TransportServer> getServers() {
        return servers;
    }

    public void setServers(List<TransportServer> servers) {
        this.servers = servers;
    }

    public List<TransportRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<TransportRoute> routes) {
        this.routes = routes;
    }
}
