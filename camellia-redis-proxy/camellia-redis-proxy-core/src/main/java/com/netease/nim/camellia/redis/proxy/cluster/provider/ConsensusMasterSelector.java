package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;

/**
 * Created by caojiajun on 2024/6/18
 */
public interface ConsensusMasterSelector {

    /**
     * 获取master
     * @return master
     */
    ProxyNode getMaster();

    /**
     * 获取slotMap
     * @return slotMap
     */
    ProxyClusterSlotMap getSlotMap();

    /**
     * 保存slotMap
     * @param slotMap slotMap
     */
    void saveSlotMap(ProxyClusterSlotMap slotMap);

    /**
     * 增加一个master变更的回调
     * @param listener ConsensusMasterChangeListener
     */
    void addConsensusMasterChangeListener(ConsensusMasterChangeListener listener);

}
