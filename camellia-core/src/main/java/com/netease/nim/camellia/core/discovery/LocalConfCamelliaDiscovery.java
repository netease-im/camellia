package com.netease.nim.camellia.core.discovery;

import java.util.List;

/**
 * Created by caojiajun on 2022/3/2
 */
public class LocalConfCamelliaDiscovery<T> implements CamelliaDiscovery<T> {
    private final List<T> list;

    public LocalConfCamelliaDiscovery(List<T> list) {
        this.list = list;
    }

    @Override
    public List<T> findAll() {
        return list;
    }

    @Override
    public void setCallback(Callback<T> callback) {
    }

    @Override
    public void clearCallback(Callback<T> callback) {
    }
}
