package com.netease.nim.camellia.redis.proxy.info;

import java.util.List;

/**
 * Created by caojiajun on 2025/3/18
 */
public record RedisPhysicsNodeTopology(List<PhysicsNodeView> physicsNodeViewList, List<RedisClusterView> redisClusterViewList) {

    public static record PhysicsNodeView(String host, int nodes, List<RedisInfo> clusterInfoList) {
        //主机名
        //redis节点数
        //包含的redis集群信息
    }

    public static record RedisClusterView(String url, int size, int nodes, List<NodeInfo> nodeInfoList) {
        //集群url
        //集群主节点数
        //集群节点数
        //主机信息
    }

    public static record RedisInfo(String url, int master, int slave) {
        //集群url
        //主节点数量
        //从节点数量
    }

    public static record NodeInfo(String host, int master, int slave) {
        //主机名
        //主节点数
        //从节点数
    }

}
