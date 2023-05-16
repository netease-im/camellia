package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.extensions.discovery.eureka.EurekaHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.extensions.discovery.zk.ZkHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Created by caojiajun on 2023/5/15
 */
public class TestEureka {

    @Autowired
    private DiscoveryClient discoveryClient;

    public void test() {
        String applicationName = "camellia-hot-key-server";
        int refreshIntervalSeconds = 5;
        EurekaHotKeyServerDiscovery discovery = new EurekaHotKeyServerDiscovery(discoveryClient, applicationName, refreshIntervalSeconds);

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(discovery);
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig monitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, monitorSdkConfig);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }
}
