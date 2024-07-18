package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2024/7/18
 */
public class ClusterModeConfig {

    public static int clusterModeRefreshNodesIntervalSeconds() {
        return ProxyDynamicConf.getInt("proxy.cluster.mode.refresh.nodes.interval.seconds", 60);
    }

    public static boolean clusterModeCommandMoveEnable() {
        return ProxyDynamicConf.getBoolean("proxy.cluster.mode.command.move.enable", true);
    }

    public static int clusterModeCommandMoveIntervalSeconds() {
        return ProxyDynamicConf.getInt("proxy.cluster.mode.command.move.interval.seconds", 30);
    }

    public static boolean clusterModeCommandMoveAlways() {
        return ProxyDynamicConf.getBoolean("proxy.cluster.mode.command.move.always", false);
    }
}
