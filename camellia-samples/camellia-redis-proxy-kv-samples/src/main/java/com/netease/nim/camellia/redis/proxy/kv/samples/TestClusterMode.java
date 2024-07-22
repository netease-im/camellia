package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by caojiajun on 2024/7/22
 */
public class TestClusterMode {

    public static void main(String[] args) {
        ProxyNode proxyNode1 = new ProxyNode("127.0.0.1", 6379, 16379);
        ProxyNode proxyNode2 = new ProxyNode("127.0.0.1", 6379+1, 16379+1);
        ProxyNode proxyNode3 = new ProxyNode("127.0.0.1", 6379+2, 16379+2);
        ProxyNode proxyNode4 = new ProxyNode("127.0.0.1", 6379+3, 16379+3);
        ProxyNode proxyNode5 = new ProxyNode("127.0.0.1", 6379+4, 16379+4);

        ProxyClusterSlotMap slotMap = ProxyClusterSlotMapUtils.uniformDistribution(proxyNode1, Arrays.asList(proxyNode1, proxyNode2, proxyNode3));
        System.out.println(slotMap);

//        ProxyClusterSlotMap slotMap1 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, Collections.singletonList(proxyNode4), null);
//        System.out.println(slotMap1);
//
//        ProxyClusterSlotMap slotMap2 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, null, Collections.singletonList(proxyNode2));
//        System.out.println(slotMap2);
//
//        ProxyClusterSlotMap slotMap3 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, Collections.singletonList(proxyNode4), Collections.singletonList(proxyNode2));
//        System.out.println(slotMap3);
//
        ProxyClusterSlotMap slotMap4 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, Arrays.asList(proxyNode4, proxyNode5), null);
        System.out.println(slotMap4);

        ProxyClusterSlotMap slotMap5 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, Arrays.asList(proxyNode4, proxyNode5), Collections.singletonList(proxyNode2));
        System.out.println(slotMap5);

        ProxyClusterSlotMap slotMap6 = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, proxyNode1, null, Arrays.asList(proxyNode2, proxyNode3));
        System.out.println(slotMap6);
    }
}
