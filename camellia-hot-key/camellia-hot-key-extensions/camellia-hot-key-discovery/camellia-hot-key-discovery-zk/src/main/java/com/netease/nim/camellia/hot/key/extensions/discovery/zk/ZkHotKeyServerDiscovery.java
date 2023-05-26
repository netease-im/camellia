package com.netease.nim.camellia.hot.key.extensions.discovery.zk;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.zk.ZkDiscovery;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class ZkHotKeyServerDiscovery implements HotKeyServerDiscovery {

    private final ZkDiscovery<HotKeyServerAddr> zkDiscovery;
    /**
     * /basePath/applicationName@127.0.0.1:2181,127.0.0.2:2181
     */
    private final String name;

    public ZkHotKeyServerDiscovery(String zkUrl, String basePath, String applicationName) {
        zkDiscovery = new ZkDiscovery<>(HotKeyServerAddr.class, zkUrl, basePath, applicationName);
        this.name = basePath + "/" + applicationName + "@" + zkUrl;
    }

    @Override
    public List<HotKeyServerAddr> findAll() {
        return zkDiscovery.findAll();
    }

    @Override
    public void setCallback(Callback<HotKeyServerAddr> callback) {
        zkDiscovery.setCallback(callback);
    }

    @Override
    public void clearCallback(Callback<HotKeyServerAddr> callback) {
        zkDiscovery.clearCallback(callback);
    }

    @Override
    public String getName() {
        return name;
    }
}
