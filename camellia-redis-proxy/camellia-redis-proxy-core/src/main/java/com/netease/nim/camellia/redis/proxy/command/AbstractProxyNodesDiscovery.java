package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;
import com.netease.nim.camellia.tools.utils.InetUtils;

import java.net.InetAddress;
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
        String currentNodeHost = ProxyDynamicConf.getString("proxy.node.current.host", null);
        if (currentNodeHost == null) {
            InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
            if (inetAddress == null) {
                currentNodeHost = "127.0.0.1";
            } else {
                currentNodeHost = inetAddress.getHostAddress();
            }
        }
        ProxyNode currentNode = new ProxyNode();
        currentNode.setHost(currentNodeHost);
        currentNode.setPort(GlobalRedisProxyEnv.getPort());
        currentNode.setCport(GlobalRedisProxyEnv.getCport());
        return currentNode;
    }
}
