package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.upstream.cluster.RedisClusterSlotInfo;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

import java.util.*;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ProxyClusterSlotMapUtils {

    public static ProxyClusterSlotMap uniformDistribution(ProxyNode currentNode, List<ProxyNode> onlineNodes) {
        if (onlineNodes.isEmpty()) {
            return null;
        }
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

    public static List<Integer> removedSlots(ProxyClusterSlotMap oldSlotMap, ProxyClusterSlotMap newSlotMap) {
        Map<ProxyNode, List<Integer>> map1 = oldSlotMap == null ? new HashMap<>() : oldSlotMap.getNodeSlotMap();
        Map<ProxyNode, List<Integer>> map2 = newSlotMap.getNodeSlotMap();
        ProxyNode currentNode = newSlotMap.getCurrentNode();
        List<Integer> oldSlots = map1.get(currentNode);
        if (oldSlots == null) {
            oldSlots = new ArrayList<>();
        }
        List<Integer> newSlots = map2.get(currentNode);
        if (newSlots == null) {
            newSlots = new ArrayList<>();
        }
        oldSlots.removeAll(newSlots);
        return oldSlots;
    }
}
