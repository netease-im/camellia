package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.upstream.cluster.RedisClusterSlotInfo;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ProxyClusterSlotMapUtils {

    public static ProxyClusterSlotMap localSlotMap(ProxyNode currentNode, ProxyClusterSlotMap slotMap) {
        return new ProxyClusterSlotMap(currentNode, slotMap.getSlotArray());
    }

    public static ProxyClusterSlotMap optimizeBalance(ProxyClusterSlotMap oldSlotMap, ProxyNode currentNode,
                                                      List<ProxyNode> onlineNodes, List<ProxyNode> offlineNodes) {
        Set<ProxyNode> currentOnlineNodes = new HashSet<>(oldSlotMap.getOnlineNodes());

        Set<ProxyNode> addedNodes = new HashSet<>();
        Set<ProxyNode> deletedNodes = new HashSet<>();

        if (onlineNodes != null && !onlineNodes.isEmpty()) {
            for (ProxyNode onlineNode : onlineNodes) {
                if (!currentOnlineNodes.contains(onlineNode)) {
                    addedNodes.add(onlineNode);
                }
            }
        }
        if (offlineNodes != null && !offlineNodes.isEmpty()) {
            for (ProxyNode offlineNode : offlineNodes) {
                if (currentOnlineNodes.contains(offlineNode)) {
                    deletedNodes.add(offlineNode);
                }
            }
        }

        if (addedNodes.isEmpty() && deletedNodes.isEmpty()) {
            return null;
        }

        Set<ProxyNode> leaveOnlineNodes = new HashSet<>(oldSlotMap.getOnlineNodes());
        leaveOnlineNodes.removeAll(deletedNodes);

        Set<ProxyNode> newOnlineNodes = new HashSet<>(oldSlotMap.getOnlineNodes());
        newOnlineNodes.removeAll(deletedNodes);
        newOnlineNodes.addAll(addedNodes);

        int newSize = newOnlineNodes.size();

        int slotSizePerNode = RedisClusterCRC16Utils.SLOT_SIZE / newSize;

        Map<ProxyNode, AtomicInteger> map = new HashMap<>();

        ProxyNode[] slotArray = oldSlotMap.getSlotArray();
        for (int i=0; i<slotArray.length; i++) {
            ProxyNode node = slotArray[i];
            if (leaveOnlineNodes.contains(node)) {
                AtomicInteger count = CamelliaMapUtils.computeIfAbsent(map, node, k -> new AtomicInteger());
                if (count.get() >= slotSizePerNode) {
                    slotArray[i] = null;
                }
                slotArray[i] = node;
                count.incrementAndGet();
                continue;
            }
            slotArray[i] = null;
        }

        List<ProxyNode> newNodes = new ArrayList<>(addedNodes);
        int index = 0;

        for (int i=0; i<slotArray.length; i++) {
            ProxyNode node = slotArray[i];
            if (node == null) {
                while (true) {
                    ProxyNode proxyNode = newNodes.get(index);
                    AtomicInteger count = CamelliaMapUtils.computeIfAbsent(map, proxyNode, k -> new AtomicInteger());
                    if (count.get() >= slotSizePerNode) {
                        index ++;
                    } else {
                        slotArray[i] = proxyNode;
                        break;
                    }
                }
            }
        }

        return new ProxyClusterSlotMap(currentNode, slotArray);
    }

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
