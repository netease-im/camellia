package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMapUtils;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;

import java.util.Arrays;

/**
 * Created by caojiajun on 2024/6/18
 */
public class TestSlotMap {

    public static void main(String[] args) {
        ProxyNode proxyNode1 = new ProxyNode("127.0.0.1", 6379, 16379);
        ProxyNode proxyNode2 = new ProxyNode("127.0.0.1", 6380, 16380);
        ProxyNode proxyNode3 = new ProxyNode("127.0.0.1", 6381, 16381);

        ProxyClusterSlotMap proxyClusterSlotMap = ProxyClusterSlotMapUtils.uniformDistribution(proxyNode1, Arrays.asList(proxyNode1, proxyNode2, proxyNode3));
        System.out.println(proxyClusterSlotMap);

        ProxyClusterSlotMap proxyClusterSlotMap2 = ProxyClusterSlotMap.parseString(proxyClusterSlotMap.toString());

        System.out.println(proxyClusterSlotMap2.equals(proxyClusterSlotMap));
    }
}
