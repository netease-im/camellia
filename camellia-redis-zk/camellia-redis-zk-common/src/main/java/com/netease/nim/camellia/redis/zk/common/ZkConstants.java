package com.netease.nim.camellia.redis.zk.common;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class ZkConstants {
    public static int sessionTimeoutMs = 60 * 1000;
    public static int connectionTimeoutMs = 15 * 1000;
    public static int baseSleepTimeMs = 1000;
    public static int maxRetries = 3;
    public static final String basePath = "/camellia";
    public static int reloadIntervalSeconds = 600;
    public static boolean sideCarFirst = false;
    public static boolean preferredHostName = false;
}
