package com.netease.nim.camellia.hot.key.sdk.discovery;

import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/10
 */
public class LocalConfHotKeyServerDiscovery extends HotKeyServerDiscovery {

    private final String name;
    private final List<HotKeyServerAddr> addrs;

    public LocalConfHotKeyServerDiscovery(String name, List<HotKeyServerAddr> addrs) {
        this.name = name;
        this.addrs = addrs;
    }

    @Override
    public List<HotKeyServerAddr> findAll() {
        return addrs;
    }

    @Override
    public void setCallback(Callback<HotKeyServerAddr> callback) {
    }

    @Override
    public void clearCallback(Callback<HotKeyServerAddr> callback) {
    }

    @Override
    public String getName() {
        return name;
    }
}
