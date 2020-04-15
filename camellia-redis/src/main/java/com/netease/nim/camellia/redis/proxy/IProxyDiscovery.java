package com.netease.nim.camellia.redis.proxy;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public interface IProxyDiscovery {

    List<Proxy> findAll();

    void setCallback(Callback callback);

    void clearCallback(Callback callback);

    interface Callback {

        void add(Proxy proxy);

        void remove(Proxy proxy);
    }
}
