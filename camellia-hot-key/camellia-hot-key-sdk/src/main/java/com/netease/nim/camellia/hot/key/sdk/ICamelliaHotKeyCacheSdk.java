package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/6
 */
public interface ICamelliaHotKeyCacheSdk {

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则会走loader穿透（穿透时支持设置并发控制）
     * 如果不是热key，则通过loader获取到value后返回
     *
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     *
     * @param key key
     * @param loader value loader
     * @param loaderLock loader lock，用于并发控制，如果不传，则不进行并发控制，可以是单机的锁，也可以是集群的锁（如redis）
     * @return value
     */
    <T> T getValue(String key, ValueLoader<T> loader, IValueLoaderLock loaderLock);

    /**
     * key的value被更新了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyUpdate(String key);

    /**
     * key的value被删除了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyDelete(String key);
}
