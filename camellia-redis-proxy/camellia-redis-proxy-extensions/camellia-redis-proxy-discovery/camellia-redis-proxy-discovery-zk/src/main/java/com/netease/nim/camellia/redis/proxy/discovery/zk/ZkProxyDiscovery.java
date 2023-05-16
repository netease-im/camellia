package com.netease.nim.camellia.redis.proxy.discovery.zk;

import com.netease.nim.camellia.redis.base.proxy.IProxyDiscovery;
import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.zk.ZkClientFactory;
import com.netease.nim.camellia.zk.ZkConstants;
import com.netease.nim.camellia.zk.ZkDiscovery;
import org.apache.curator.framework.CuratorFramework;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class ZkProxyDiscovery implements IProxyDiscovery {

    private final ZkDiscovery<Proxy> zkDiscovery;

    public ZkProxyDiscovery(String zkUrl, String applicationName) {
        zkDiscovery = new ZkDiscovery<>(Proxy.class, zkUrl, ZkProxyConstants.basePath, applicationName);
    }

    public ZkProxyDiscovery(String zkUrl, String basePath, String applicationName) {
        zkDiscovery = new ZkDiscovery<>(Proxy.class, ZkClientFactory.DEFAULT.getClient(zkUrl), basePath, applicationName, ZkConstants.reloadIntervalSeconds);
    }

    public ZkProxyDiscovery(ZkClientFactory factory, String zkUrl, String basePath, String applicationName, long reloadIntervalSeconds) {
        zkDiscovery = new ZkDiscovery<>(Proxy.class, factory.getClient(zkUrl), basePath, applicationName, reloadIntervalSeconds);
    }

    public ZkProxyDiscovery(CuratorFramework client, String basePath, String applicationName, long reloadIntervalSeconds) {
        zkDiscovery = new ZkDiscovery<>(Proxy.class, client, basePath, applicationName, reloadIntervalSeconds);
    }

    @Override
    public List<Proxy> findAll() {
        return zkDiscovery.findAll();
    }

    @Override
    public void setCallback(Callback<Proxy> callback) {
        zkDiscovery.setCallback(callback);
    }

    @Override
    public void clearCallback(Callback<Proxy> callback) {
        zkDiscovery.clearCallback(callback);
    }
}
