package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;

import java.util.List;

/**
 * Created by caojiajun on 2024/6/18
 */
public interface ConsensusLeaderSelector {

    /**
     * init method
     * @param currentNode current node
     * @param initNodes init nodes
     */
    void init(ProxyNode currentNode, List<ProxyNode> initNodes);

    /**
     * 获取master
     * @return master
     */
    ProxyNode getLeader();

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
    void addConsensusLeaderChangeListener(ConsensusLeaderChangeListener listener);

}
