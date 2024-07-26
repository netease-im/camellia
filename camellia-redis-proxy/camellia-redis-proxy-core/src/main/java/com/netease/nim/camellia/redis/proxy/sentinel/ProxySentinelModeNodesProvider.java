package com.netease.nim.camellia.redis.proxy.sentinel;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;

import java.util.List;

/**
 * Created by caojiajun on 2024/7/26
 */
public interface ProxySentinelModeNodesProvider {

    void init(ProxyNode currentNode);

    List<ProxyNode> load();
}
