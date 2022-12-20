package com.netease.nim.camellia.cache.core;



public class CamelliaCacheEnv {

    //批量操作时，每批次的上限
    public static int multiOpBatchSize = 500;
    //避免缓存穿透时并发操作的锁的过期时间
    public static long syncLoadExpireMillis = 1000;
    //等待避免缓存穿透时并发操作的锁时的最多等待次数
    public static int syncLoadMaxRetry = 1;
    //等待避免缓存穿透时并发操作的锁时两次等待的时间间隔
    public static long syncLoadSleepMillis = 100;
    //缓存value值的最大值，超过则不存入缓存中
    public static int maxCacheValue = 2*1024*1024;
}
