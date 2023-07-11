package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public class UpstreamRouterConfig {

    private List<Upstream> upstreams;
    private List<UpstreamRoute> routes;

    public List<Upstream> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(List<Upstream> upstreams) {
        this.upstreams = upstreams;
    }

    public List<UpstreamRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<UpstreamRoute> routes) {
        this.routes = routes;
    }

}
