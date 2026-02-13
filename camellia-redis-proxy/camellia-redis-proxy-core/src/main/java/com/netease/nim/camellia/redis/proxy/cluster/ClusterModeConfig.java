package com.netease.nim.camellia.redis.proxy.cluster;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2024/7/18
 */
public class ClusterModeConfig {

    public static int clusterModeHeartbeatIntervalSeconds() {
        return ProxyDynamicConf.getInt("cluster.mode.heartbeat.interval.seconds", 5);
    }

    public static int clusterModeRefreshNodesIntervalSeconds() {
        return ProxyDynamicConf.getInt("cluster.mode.refresh.nodes.interval.seconds", 60);
    }

    public static boolean clusterModeCommandMoveEnable() {
        return ProxyDynamicConf.getBoolean("cluster.mode.command.move.enable", true);
    }

    public static int clusterModeCommandMoveIntervalSeconds() {
        return ProxyDynamicConf.getInt("cluster.mode.command.move.interval.seconds", 30);
    }

    public static boolean clusterModeCommandMoveAlways() {
        return ProxyDynamicConf.getBoolean("cluster.mode.command.move.always", false);
    }

    public static long clusterModeCommandMoveDelayMillis() {
        return ProxyDynamicConf.getLong("cluster.mode.command.move.graceful.delay.millis", 100L);
    }

    public static int clusterModeCommandMoveMaxRetry() {
        return ProxyDynamicConf.getInt("cluster.mode.command.move.graceful.max.retry", 5);
    }

    public static int clusterModeCommandMoveCacheMillis() {
        return ProxyDynamicConf.getInt("cluster.mode.command.move.graceful.cache.millis", 50);
    }

}
