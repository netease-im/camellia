package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.cluster.SlotSplitUtils;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2025/2/21
 */
public class TestSlotMapBalance {

    @Test
    public void test() {
        ProxyNode currentNode = ProxyNode.parseString("10.44.94.47:6383@16383");

        ProxyNode[] slotArray = new ProxyNode[RedisClusterCRC16Utils.SLOT_SIZE];

        {
            for (int i = 4908; i <= 5727; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 6548; i <= 6551; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 7368; i <= 7641; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 7646; i <= 8187; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 10651; i <= 10922; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 11738; i <= 12287; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 13109; i <= 13922; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
            for (int i = 14743; i <= 15554; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.44:6383@16383");
            }
        }
        {
            for (int i = 3272; i <= 4087; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 5728; i <= 6547; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 10648; i <= 10650; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 10923; i <= 11737; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 12288; i <= 12289; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 13924; i <= 14742; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
            for (int i = 15555; i <= 16383; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.45:6383@16383");
            }
        }
        {
            for (int i = 1640; i <= 2455; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.46:6383@16383");
            }
            for (int i = 4088; i <= 4907; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.46:6383@16383");
            }
            for (int i = 6552; i <= 7367; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.46:6383@16383");
            }
            for (int i = 8188; i <= 8195; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.46:6383@16383");
            }
            for (int i = 9012; i <= 10647; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.46:6383@16383");
            }
        }
        {
            for (int i = 0; i <= 1639; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
            for (int i = 2456; i <= 3271; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
            for (int i = 7642; i <= 7645; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
            for (int i = 8196; i <= 9011; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
            for (int i = 12290; i <= 13108; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
            for (int i = 13923; i <= 13923; i++) {
                slotArray[i] = ProxyNode.parseString("10.44.94.47:6383@16383");
            }
        }

        ProxyClusterSlotMap oldSlotMap = new ProxyClusterSlotMap(currentNode, slotArray);

        List<ProxyNode> onlineNodes = new ArrayList<>();
        onlineNodes.add(ProxyNode.parseString("10.44.94.47:6383@16383"));
        onlineNodes.add(ProxyNode.parseString("10.44.94.46:6383@16383"));
        onlineNodes.add(ProxyNode.parseString("10.44.94.45:6383@16383"));
        onlineNodes.add(ProxyNode.parseString("10.44.94.44:6383@16383"));
        onlineNodes.add(ProxyNode.parseString("10.44.94.37:6383@16383"));
        ProxyClusterSlotMap slotMap = ProxyClusterSlotMapUtils.optimizeBalance(oldSlotMap, currentNode, onlineNodes, new ArrayList<>());
        Map<ProxyNode, List<Integer>> nodeSlotMap = slotMap.getNodeSlotMap();
        for (Map.Entry<ProxyNode, List<Integer>> proxyNodeListEntry : nodeSlotMap.entrySet()) {
            System.out.println(proxyNodeListEntry.getKey());
            System.out.println(proxyNodeListEntry.getValue().size());
        }
        ProxyNode[] slotArray1 = slotMap.getSlotArray();
        ProxyNode lastNode = null;
        for (int i=13922; i<=15557; i++) {
            if (lastNode == null || !lastNode.equals(slotArray1[i])) {
                System.out.println("i=" + i + ",node=" + slotArray1[i]);
            }
            lastNode = slotArray1[i];
        }
        String string = slotMap.toString();
        System.out.println(string);
    }

    @Test
    public void test2() {

    }
}
