package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
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
                Collections.singletonList(new HotKeyServerAddr("127.0.0.1", 7070)));
        config.setDiscovery(discovery);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, new CamelliaHotKeyMonitorSdkConfig());

        String namespace1 = "namespace1";

        monitorSdk.preheat(namespace1);

        monitorSdk.push(namespace1, "hahahaha");

        new Thread(() -> {
            while (true) {
                monitorSdk.push(namespace1, UUID.randomUUID().toString());
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                monitorSdk.push(namespace1, "abc");
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
