package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;

/**
 * Created by caojiajun on 2023/5/6
 */
public interface ICamelliaHotKeyCacheSdk {

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则会走loader穿透
     * 如果不是热key，则通过loader获取到value后返回
     * <p>
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     * <p>
     * 如果key没有更新，sdk也会在配置的expireMillis之前尝试刷新一下（单机只会穿透一次）
     *
     * @param namespace namespace
     * @param key       key
     * @param loader    value loader
     * @return value
     */
    <T> T getValue(String namespace, String key, ValueLoader<T> loader);

    /**
     * key的value被更新了，需要调用本方法给hot-key-server，进而广播给所有人
     *
     * @param namespace namespace
     * @param key       key
     */
    void keyUpdate(String namespace, String key);

    /**
     * key的value被删除了，需要调用本方法给hot-key-server，进而广播给所有人
     *
     * @param namespace namespace
     * @param key       key
     */
    void keyDelete(String namespace, String key);

    /**
     * 获取当配置
     *
     * @return 配置
     */
    CamelliaHotKeyCacheSdkConfig getConfig();

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则返回null
     * <p>
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     * <p>
     * 如果key没有更新，sdk也会在配置的expireMillis之前打一个tag，之后通过{@link #tryBuildCache(String, String, ValueLoader)}
     * 尝试创建cache，并且消除这个tag
     *
     * @param namespace namespace
     * @param key       key
     * @return value
     */
    <T> T getValue(String namespace, String key);

    /**
     * 尝试创建本地cache
     *
     * @param namespace namespace
     * @param key       key
     * @param loader    loader
     */
    <T> void tryBuildCache(String namespace, String key, ValueLoader<T> loader);

    /**
     * Check if the key is hot-key.
     *
     * @param namespace namespace
     * @param key       key
     * @return true/false
     */
    boolean checkKey(String namespace, String key);
}
