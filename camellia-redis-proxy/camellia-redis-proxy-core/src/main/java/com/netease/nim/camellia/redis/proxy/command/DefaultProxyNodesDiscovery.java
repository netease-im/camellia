package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/12/1
 */
public class DefaultProxyNodesDiscovery extends AbstractProxyNodesDiscovery {

    public DefaultProxyNodesDiscovery(ClusterModeProcessor proxyClusterModeProcessor, SentinelModeProcessor proxySentinelModeProcessor) {
        super(proxyClusterModeProcessor, proxySentinelModeProcessor);
    }

    @Override
    public List<ProxyNode> discovery() {
        List<ProxyNode> proxyNodes = super.discovery();
        if (proxyNodes != null) {
            return proxyNodes;
        }
        String string = ProxyDynamicConf.getString("proxy.nodes", "");
        List<ProxyNode> list = new ArrayList<>();
        if (string != null && !string.trim().isEmpty()) {
            string = string.trim();
            String[] split = string.split(",");
            for (String str : split) {
                ProxyNode proxyNode = ProxyNode.parseString(str);
                if (proxyNode == null) {
                    continue;
                }
                list.add(proxyNode);
            }
        }
        return list;
    }
}
