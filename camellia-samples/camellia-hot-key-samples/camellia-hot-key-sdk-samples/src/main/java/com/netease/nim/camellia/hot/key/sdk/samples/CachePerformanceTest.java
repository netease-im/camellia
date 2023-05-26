package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.hot.key.sdk.discovery.LocalConfHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/16
 */
public class CachePerformanceTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) {

        HotKeyConstants.Client.source = "test1";

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        List<HotKeyServerAddr> addrList = new ArrayList<>();
        addrList.add(new HotKeyServerAddr("127.0.0.1", 7070));
        LocalConfHotKeyServerDiscovery discovery = new LocalConfHotKeyServerDiscovery("local", addrList);
        config.setDiscovery(discovery);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        CamelliaHotKeyCacheSdk cacheSdk = new CamelliaHotKeyCacheSdk(sdk, new CamelliaHotKeyCacheSdkConfig());

        String namespace1 = "namespace1";
        while (true) {
            try {
                String value = cacheSdk.getValue(namespace1, "abc", key -> key + id.incrementAndGet());
                System.out.println(value);
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
