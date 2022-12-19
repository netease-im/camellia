package com.netease.nim.camellia.cache.core;

public class CamelliaCacheName {

    /**
     * 分布式缓存，如Redis、Memcache等
     */
    //##不缓存Null
    public static final String REMOTE_MINUTE_1 = "REMOTE_MINUTE_1";
    public static final String REMOTE_MINUTE_10 = "REMOTE_MINUTE_10";
    public static final String REMOTE_MINUTE_30 = "REMOTE_MINUTE_30";
    public static final String REMOTE_HOUR_1 = "REMOTE_HOUR_1";
    public static final String REMOTE_HOUR_4 = "REMOTE_HOUR_4";
    public static final String REMOTE_HOUR_12 = "REMOTE_HOUR_12";
    public static final String REMOTE_DAY_1 = "REMOTE_DAY_1";
    public static final String REMOTE_DAY_3 = "REMOTE_DAY_3";
    public static final String REMOTE_DAY_7 = "REMOTE_DAY_7";
    public static final String REMOTE_DAY_30 = "REMOTE_DAY_30";
    public static final String REMOTE_DAY_365 = "REMOTE_DAY_365";
    public static final String REMOTE_FOREVER = "REMOTE_FOREVER";
    //##缓存Null
    public static final String REMOTE_MINUTE_1_CACHE_NULL = "REMOTE_MINUTE_1_CACHE_NULL";
    public static final String REMOTE_MINUTE_10_CACHE_NULL = "REMOTE_MINUTE_10_CACHE_NULL";
    public static final String REMOTE_MINUTE_30_CACHE_NULL = "REMOTE_MINUTE_30_CACHE_NULL";
    public static final String REMOTE_HOUR_1_CACHE_NULL = "REMOTE_HOUR_1_CACHE_NULL";
    public static final String REMOTE_HOUR_4_CACHE_NULL = "REMOTE_HOUR_4_CACHE_NULL";
    public static final String REMOTE_HOUR_12_CACHE_NULL = "REMOTE_HOUR_12_CACHE_NULL";
    public static final String REMOTE_DAY_1_CACHE_NULL = "REMOTE_DAY_1_CACHE_NULL";
    public static final String REMOTE_DAY_3_CACHE_NULL = "REMOTE_DAY_3_CACHE_NULL";
    public static final String REMOTE_DAY_7_CACHE_NULL = "REMOTE_DAY_7_CACHE_NULL";
    public static final String REMOTE_DAY_30_CACHE_NULL = "REMOTE_DAY_30_CACHE_NULL";
    public static final String REMOTE_DAY_365_CACHE_NULL = "REMOTE_DAY_365_CACHE_NULL";
    public static final String REMOTE_FOREVER_CACHE_NULL = "REMOTE_FOREVER_CACHE_NULL";

    /**
     * 本地缓存，如LRU_Local_Cache，典型的如Google ConcurrentLinkedHashMap
     * 所谓的安全和不安全指的是缓存中获取的对象在外部程序中被改变的情况下，缓存中的对象是否改变
     */
    //##不缓存Null，不安全的
    public static final String LOCAL_MILLIS_10 = "LOCAL_MILLIS_10";
    public static final String LOCAL_MILLIS_100 = "LOCAL_MILLIS_100";
    public static final String LOCAL_MILLIS_500 = "LOCAL_MILLIS_500";
    public static final String LOCAL_SECOND_1 = "LOCAL_SECOND_1";
    public static final String LOCAL_SECOND_5 = "LOCAL_SECOND_5";
    public static final String LOCAL_SECOND_10 = "LOCAL_SECOND_10";
    public static final String LOCAL_SECOND_30 = "LOCAL_SECOND_30";
    public static final String LOCAL_MINUTE_1 = "LOCAL_MINUTE_1";
    public static final String LOCAL_MINUTE_5 = "LOCAL_MINUTE_5";
    public static final String LOCAL_MINUTE_10 = "LOCAL_MINUTE_10";
    public static final String LOCAL_MINUTE_30 = "LOCAL_MINUTE_30";
    public static final String LOCAL_HOUR_1 = "LOCAL_HOUR_1";
    public static final String LOCAL_HOUR_24 = "LOCAL_DAY_1";
    public static final String LOCAL_FOREVER = "LOCAL_FOREVER";
    //##缓存Null，不安全的
    public static final String LOCAL_MILLIS_10_CACHE_NULL = "LOCAL_MILLIS_10_CACHE_NULL";
    public static final String LOCAL_MILLIS_100_CACHE_NULL = "LOCAL_MILLIS_100_CACHE_NULL";
    public static final String LOCAL_MILLIS_500_CACHE_NULL = "LOCAL_MILLIS_500_CACHE_NULL";
    public static final String LOCAL_SECOND_1_CACHE_NULL = "LOCAL_SECOND_1_CACHE_NULL";
    public static final String LOCAL_SECOND_5_CACHE_NULL = "LOCAL_SECOND_5_CACHE_NULL";
    public static final String LOCAL_SECOND_10_CACHE_NULL = "LOCAL_SECOND_10_CACHE_NULL";
    public static final String LOCAL_SECOND_30_CACHE_NULL = "LOCAL_SECOND_30_CACHE_NULL";
    public static final String LOCAL_MINUTE_1_CACHE_NULL = "LOCAL_MINUTE_1_CACHE_NULL";
    public static final String LOCAL_MINUTE_5_CACHE_NULL = "LOCAL_MINUTE_5_CACHE_NULL";
    public static final String LOCAL_MINUTE_10_CACHE_NULL = "LOCAL_MINUTE_10_CACHE_NULL";
    public static final String LOCAL_MINUTE_30_CACHE_NULL = "LOCAL_MINUTE_30_CACHE_NULL";
    public static final String LOCAL_HOUR_1_CACHE_NULL = "LOCAL_HOUR_1_CACHE_NULL";
    public static final String LOCAL_HOUR_24_CACHE_NULL = "LOCAL_DAY_1_CACHE_NULL";
    public static final String LOCAL_FOREVER_CACHE_NULL = "LOCAL_FOREVER_CACHE_NULL";
    //##不缓存Null，安全的
    public static final String SAFE_LOCAL_MILLIS_10 = "SAFE_LOCAL_MILLIS_10";
    public static final String SAFE_LOCAL_MILLIS_100 = "SAFE_LOCAL_MILLIS_100";
    public static final String SAFE_LOCAL_MILLIS_500 = "SAFE_LOCAL_MILLIS_500";
    public static final String SAFE_LOCAL_SECOND_1 = "SAFE_LOCAL_SECOND_1";
    public static final String SAFE_LOCAL_SECOND_5 = "SAFE_LOCAL_SECOND_5";
    public static final String SAFE_LOCAL_SECOND_10 = "SAFE_LOCAL_SECOND_10";
    public static final String SAFE_LOCAL_SECOND_30 = "SAFE_LOCAL_SECOND_30";
    public static final String SAFE_LOCAL_MINUTE_1 = "SAFE_LOCAL_MINUTE_1";
    public static final String SAFE_LOCAL_MINUTE_5 = "SAFE_LOCAL_MINUTE_5";
    public static final String SAFE_LOCAL_MINUTE_10 = "SAFE_LOCAL_MINUTE_10";
    public static final String SAFE_LOCAL_MINUTE_30 = "SAFE_LOCAL_MINUTE_30";
    public static final String SAFE_LOCAL_HOUR_1 = "SAFE_LOCAL_HOUR_1";
    public static final String SAFE_LOCAL_HOUR_24 = "SAFE_LOCAL_DAY_1";
    public static final String SAFE_LOCAL_FOREVER = "SAFE_LOCAL_FOREVER";
    //##缓存Null，安全的
    public static final String SAFE_LOCAL_MILLIS_10_CACHE_NULL = "SAFE_LOCAL_MILLIS_10_CACHE_NULL";
    public static final String SAFE_LOCAL_MILLIS_100_CACHE_NULL = "SAFE_LOCAL_MILLIS_100_CACHE_NULL";
    public static final String SAFE_LOCAL_MILLIS_500_CACHE_NULL = "SAFE_LOCAL_MILLIS_500_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_1_CACHE_NULL = "SAFE_LOCAL_SECOND_1_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_5_CACHE_NULL = "SAFE_LOCAL_SECOND_5_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_10_CACHE_NULL = "SAFE_LOCAL_SECOND_10_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_30_CACHE_NULL = "SAFE_LOCAL_SECOND_30_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_1_CACHE_NULL = "SAFE_LOCAL_MINUTE_1_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_5_CACHE_NULL = "SAFE_LOCAL_MINUTE_5_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_10_CACHE_NULL = "SAFE_LOCAL_MINUTE_10_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_30_CACHE_NULL = "SAFE_LOCAL_MINUTE_30_CACHE_NULL";
    public static final String SAFE_LOCAL_HOUR_1_CACHE_NULL = "SAFE_LOCAL_HOUR_1_CACHE_NULL";
    public static final String SAFE_LOCAL_HOUR_24_CACHE_NULL = "SAFE_LOCAL_DAY_1_CACHE_NULL";
    public static final String SAFE_LOCAL_FOREVER_CACHE_NULL = "SAFE_LOCAL_FOREVER_CACHE_NULL";
}
