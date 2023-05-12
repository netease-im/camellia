package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.Result;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.hot.key.sdk.discovery.LocalConfHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class PerformanceTest {

    public static void main(String[] args) {
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        LocalConfHotKeyServerDiscovery discovery = new LocalConfHotKeyServerDiscovery("local",
                Collections.singletonList(new HotKeyServerAddr("10.189.249.35", 7070)));
        config.setDiscovery(discovery);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, new CamelliaHotKeyMonitorSdkConfig());

        String namespace1 = "sql_hot_key";

        monitorSdk.preheat(namespace1);

        String key0 = "hahahaha";
        Result result0 = monitorSdk.push(namespace1, key0);
        System.out.println("key=" + key0 + ",hot=" + result0.isHot());

        new Thread(() -> {
            while (true) {
                String key = UUID.randomUUID().toString();
                Result result = monitorSdk.push(namespace1, key);
//                System.out.println("key=" + key + ",hot=" + result.isHot());
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                String key = "abc";
                Result result = monitorSdk.push(namespace1, key);
//                System.out.println("key=" + key + ",hot=" + result.isHot());
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
