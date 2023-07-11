package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/10
 */
public class Upstream {
    private String upstream;
    private UpstreamType type;
    private List<String> addrs;
    private String heartbeatUri;
    private int heartbeatTimeout = 500;

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

    public UpstreamType getType() {
        return type;
    }

    public void setType(UpstreamType type) {
        this.type = type;
    }

    public List<String> getAddrs() {
        return addrs;
    }

    public void setAddrs(List<String> addrs) {
        this.addrs = addrs;
    }

    public String getHeartbeatUri() {
        return heartbeatUri;
    }

    public void setHeartbeatUri(String heartbeatUri) {
        this.heartbeatUri = heartbeatUri;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }
}
