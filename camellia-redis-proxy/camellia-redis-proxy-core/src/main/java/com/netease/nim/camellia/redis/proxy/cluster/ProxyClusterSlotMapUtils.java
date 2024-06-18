package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.upstream.cluster.RedisClusterSlotInfo;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ProxyClusterSlotMapUtils {

    public static ProxyClusterSlotMap uniformDistribution(ProxyNode currentNode, List<ProxyNode> onlineNodes) {
        ProxyNode[] slotArray = new ProxyNode[RedisClusterCRC16Utils.SLOT_SIZE];
        List<ProxyNode> nodes = new ArrayList<>(onlineNodes);
        Collections.sort(nodes);
        int i=0;
        int size = nodes.size();
        int slotsPerNode = RedisClusterSlotInfo.SLOT_SIZE / size;
        int slotCurrent = 0;
        for (ProxyNode proxyNode : nodes) {
            i++;
            int start;
            int stop;
            start = slotCurrent;
            if (i == size) {
                stop = RedisClusterSlotInfo.SLOT_SIZE - 1;
            } else {
                stop = slotCurrent + slotsPerNode;
            }
            for (int j=start; j<=stop; j++) {
                slotArray[j] = proxyNode;
            }
            slotCurrent += slotsPerNode;
            slotCurrent ++;
        }
        return new ProxyClusterSlotMap(currentNode, slotArray);
    }

}
