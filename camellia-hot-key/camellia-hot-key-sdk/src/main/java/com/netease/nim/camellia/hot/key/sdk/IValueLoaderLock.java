package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/8
 */
public interface IValueLoaderLock {

    /**
     * 尝试获取一把锁
     * @param key key
     * @return 成功/失败
     */
    boolean tryLock(String key);

    /**
     * 释放一把锁
     * @param key key
     * @return 成功/失败
     */
    boolean release(String key);


}
