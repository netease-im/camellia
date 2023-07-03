package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.Result;
import com.netease.nim.camellia.hot.key.sdk.collect.CollectorType;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.hot.key.sdk.discovery.LocalConfHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class MonitorPerformanceTest {

    public static void main(String[] args) {

        HotKeyConstants.Client.source = "test2";

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        List<HotKeyServerAddr> addrList = new ArrayList<>();
        addrList.add(new HotKeyServerAddr("127.0.0.1", 7070));
//        addrList.add(new HotKeyServerAddr("10.156.148.248", 7070));
//        addrList.add(new HotKeyServerAddr("10.189.46.125", 7070));
        LocalConfHotKeyServerDiscovery discovery = new LocalConfHotKeyServerDiscovery("local", addrList);
        config.setDiscovery(discovery);
        config.setCollectorType(CollectorType.ConcurrentLinkedHashMap);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, new CamelliaHotKeyMonitorSdkConfig());

        String namespace1 = "namespace1";

        monitorSdk.preheat(namespace1);

        String key0 = "hahahaha";
        Result result0 = monitorSdk.push(namespace1, key0, 1);
        System.out.println("key=" + key0 + ",hot=" + result0.isHot());

        new Thread(() -> {
            int count = 0;
            while (true) {
                String key = UUID.randomUUID().toString();
                Result result = monitorSdk.push(namespace1, key, 1);
                count ++;
                if (count == 10000) {
//                    System.out.println("key=" + key + ",hot=" + result.isHot());
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count = 0;
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                String key = "abc";
                Result result = monitorSdk.push(namespace1, key, 1);
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
                String key = "def";
                Result result = monitorSdk.push(namespace1, key, 1);
//                System.out.println("key=" + key + ",hot=" + result.isHot());
                try {
                    TimeUnit.MILLISECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
