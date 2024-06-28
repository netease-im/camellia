package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;

import java.util.List;

/**
 * Created by caojiajun on 2023/12/1
 */
public abstract class AbstractProxyNodesDiscovery implements ProxyNodesDiscovery {

    private final ProxyClusterModeProcessor proxyClusterModeProcessor;
    private final ProxySentinelModeProcessor proxySentinelModeProcessor;

    public AbstractProxyNodesDiscovery(ProxyClusterModeProcessor proxyClusterModeProcessor, ProxySentinelModeProcessor proxySentinelModeProcessor) {
        this.proxyClusterModeProcessor = proxyClusterModeProcessor;
        this.proxySentinelModeProcessor = proxySentinelModeProcessor;
    }

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
