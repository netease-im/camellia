package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;

/**
 * Created by caojiajun on 2024/6/18
 */
public interface ConsensusMasterSelector {

    ProxyNode getMaster();

    ProxyClusterSlotMap getSlotMap();

    void saveSlotMap(ProxyClusterSlotMap slotMap);

}
