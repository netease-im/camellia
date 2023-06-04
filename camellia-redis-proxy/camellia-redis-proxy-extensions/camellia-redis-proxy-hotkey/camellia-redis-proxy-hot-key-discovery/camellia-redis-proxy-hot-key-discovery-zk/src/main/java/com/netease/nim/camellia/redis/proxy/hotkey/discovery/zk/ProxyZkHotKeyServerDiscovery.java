package com.netease.nim.camellia.redis.proxy.hotkey.discovery.zk;

import com.netease.nim.camellia.hot.key.extensions.discovery.zk.ZkHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyServerDiscovery;

/**
 * 工厂
 */
public class ProxyZkHotKeyServerDiscovery implements ProxyHotKeyServerDiscovery {

    public static final String CAMELLIA_HOT_KEY_SERVER_APPLICATION_NAME = "camellia-hot-key-server";
    public static final String CAMELLIA_HOT_KEY_BASE_PATH = "/camellia-hot-key";
    public static final String ZK_URL = "127.0.0.1:2181";

    @Override
    public HotKeyServerDiscovery getDiscovery() {
        // 配置zk发现Server机制
        String zkUrl = ProxyDynamicConf.getString("hot.key.server.zk.zkUrl",
                null, null, ZK_URL);
        String basePath = ProxyDynamicConf.getString("hot.key.server.zk.basePath",
                null, null, CAMELLIA_HOT_KEY_BASE_PATH);
        String applicationName = ProxyDynamicConf.getString("hot.key.server.zk.applicationName",
                null, null, CAMELLIA_HOT_KEY_SERVER_APPLICATION_NAME);
        return new ZkHotKeyServerDiscovery(zkUrl, basePath, applicationName);
    }

}
