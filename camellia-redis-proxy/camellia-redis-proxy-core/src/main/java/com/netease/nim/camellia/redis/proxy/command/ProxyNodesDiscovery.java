package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;

import java.util.List;

/**
 * Created by caojiajun on 2023/12/1
 */
public interface ProxyNodesDiscovery {

    List<ProxyNode> discovery();

    default ProxyNode current() {
        return ProxyCurrentNodeInfo.current();
    }

}
