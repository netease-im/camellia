package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProcessor;

import java.util.List;

/**
 * Created by caojiajun on 2023/12/1
 */
public abstract class AbstractProxyNodesDiscovery implements ProxyNodesDiscovery {

    private final ClusterModeProcessor proxyClusterModeProcessor;
    private final SentinelModeProcessor proxySentinelModeProcessor;

    /**
     * abstract proxy nodes discovery
     * @param proxyClusterModeProcessor proxyClusterModeProcessor
     * @param proxySentinelModeProcessor proxySentinelModeProcessor
     */
    public AbstractProxyNodesDiscovery(ClusterModeProcessor proxyClusterModeProcessor, SentinelModeProcessor proxySentinelModeProcessor) {
        this.proxyClusterModeProcessor = proxyClusterModeProcessor;
        this.proxySentinelModeProcessor = proxySentinelModeProcessor;
    }

    /**
     * discovery nodes
     * @return nodes
     */
    @Override
    public List<ProxyNode> discovery() {
        if (proxyClusterModeProcessor != null) {
            return proxyClusterModeProcessor.getOnlineNodes();
        }
        if (proxySentinelModeProcessor != null) {
            return proxySentinelModeProcessor.getOnlineNodes();
        }
        return null;
    }

    /**
     * current node
     * @return node
     */
    @Override
    public ProxyNode current() {
        if (proxyClusterModeProcessor != null) {
            return proxyClusterModeProcessor.getCurrentNode();
        }
        if (proxySentinelModeProcessor != null) {
            return proxySentinelModeProcessor.getCurrentNode();
        }
        return ProxyCurrentNodeInfo.current();
    }
}
