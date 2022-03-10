package com.netease.nim.camellia.core.discovery;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/2
 */
public interface CamelliaDiscovery<T> {

    List<T> findAll();

    void setCallback(Callback<T> callback);

    void clearCallback(Callback<T> callback);

    interface Callback<T> {

        void add(T server);

        void remove(T server);
    }
}
