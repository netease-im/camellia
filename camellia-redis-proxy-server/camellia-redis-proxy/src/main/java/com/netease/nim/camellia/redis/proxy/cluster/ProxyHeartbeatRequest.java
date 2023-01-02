package com.netease.nim.camellia.redis.proxy.cluster;


/**
 * Created by caojiajun on 2022/9/30
 */
public class ProxyHeartbeatRequest {
    private ProxyNode node;
    private ClusterModeStatus.Status status;

    public ProxyNode getNode() {
        return node;
    }

    public void setNode(ProxyNode node) {
        this.node = node;
    }

    public ClusterModeStatus.Status getStatus() {
        return status;
    }

    public void setStatus(ClusterModeStatus.Status status) {
        this.status = status;
    }
}
