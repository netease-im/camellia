package com.netease.nim.camellia.hot.key.extensions.discovery.zk;

import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.zk.ZkDiscovery;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class ZkHotKeyServerDiscovery extends HotKeyServerDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ZkHotKeyServerDiscovery.class);

    private final ZkDiscovery<HotKeyServerAddr> zkDiscovery;

    public ZkHotKeyServerDiscovery(String zkUrl, String basePath, String applicationName) {
        zkDiscovery = new ZkDiscovery<>(zkUrl, basePath, applicationName);
    }

    public ZkHotKeyServerDiscovery(CuratorFramework client, String basePath, String applicationName, long reloadIntervalSeconds) {
        zkDiscovery = new ZkDiscovery<>(client, basePath, applicationName, reloadIntervalSeconds);
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
        return zkDiscovery.getApplicationName();
    }
}
