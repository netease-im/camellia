package com.netease.nim.camellia.redis.proxy.hotkey.discovery.eureka;

import com.netease.nim.camellia.hot.key.extensions.discovery.eureka.EurekaHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.discovery.HotKeyServerDiscovery;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.hotkey.common.ProxyHotKeyServerDiscoveryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * 基于Eureka的HotKeyServer发现
 */
public class ProxyEurekaHotKeyServerDiscoveryFactory implements ProxyHotKeyServerDiscoveryFactory {
    public static final String CAMELLIA_HOT_KEY_SERVER_APPLICATION_NAME = "camellia-hot-key-server";
    public static final int REFRESH_INTERVAL_SECONDS = 5;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public HotKeyServerDiscovery getDiscovery() {
        String applicationName = ProxyDynamicConf.getString("hot.key.server.eureka.applicationName", CAMELLIA_HOT_KEY_SERVER_APPLICATION_NAME);
        int refreshIntervalSeconds = ProxyDynamicConf.getInt("hot.key.server.eureka.refreshIntervalSeconds", REFRESH_INTERVAL_SECONDS);
        return new EurekaHotKeyServerDiscovery(discoveryClient, applicationName, refreshIntervalSeconds);
    }
}
