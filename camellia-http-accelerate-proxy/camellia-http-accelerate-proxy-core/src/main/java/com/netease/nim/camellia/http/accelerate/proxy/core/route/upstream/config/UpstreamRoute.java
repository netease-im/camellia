package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

/**
 * Created by caojiajun on 2023/7/10
 */
public class UpstreamRoute {
    private UpstreamRouteType type;
    private String host;
    private String upstream;
    public UpstreamRouteType getType() {
        return type;
    }

    public void setType(UpstreamRouteType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

}
