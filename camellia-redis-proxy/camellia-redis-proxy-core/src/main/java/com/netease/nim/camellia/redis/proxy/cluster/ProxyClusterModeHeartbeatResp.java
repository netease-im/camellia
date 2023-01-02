package com.netease.nim.camellia.redis.proxy.cluster;

import java.util.List;

/**
 * Created by caojiajun on 2022/9/30
 */
public class ProxyClusterModeHeartbeatResp {
    private List<ProxyNode> onlineNodes;
    private List<ProxyNode> pendingNodes;

    public List<ProxyNode> getOnlineNodes() {
        return onlineNodes;
    }

    public void setOnlineNodes(List<ProxyNode> onlineNodes) {
        this.onlineNodes = onlineNodes;
    }

    public List<ProxyNode> getPendingNodes() {
        return pendingNodes;
    }

    public void setPendingNodes(List<ProxyNode> pendingNodes) {
        this.pendingNodes = pendingNodes;
    }
}
