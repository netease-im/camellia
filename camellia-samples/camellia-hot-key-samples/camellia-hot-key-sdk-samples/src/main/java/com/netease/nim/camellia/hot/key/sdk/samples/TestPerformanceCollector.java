package com.netease.nim.camellia.hot.key.sdk.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.collect.CollectorType;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.hot.key.sdk.discovery.LocalConfHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.tools.statistic.CamelliaStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/7/18
 */
public class TestPerformanceCollector {

    public static void main(String[] args) {
        HotKeyConstants.Client.source = "test3";

        CamelliaHotKeyMonitorSdk sdk1 = init(CollectorType.Caffeine);
        CamelliaHotKeyMonitorSdk sdk2 = init(CollectorType.ConcurrentLinkedHashMap);
        CamelliaHotKeyMonitorSdk sdk3 = init(CollectorType.ConcurrentHashMap);

        for (int i=0; i<100; i++) {
            test(CollectorType.Caffeine, sdk1);
            test(CollectorType.ConcurrentLinkedHashMap, sdk2);
            test(CollectorType.ConcurrentHashMap, sdk3);
        }
    }

    private static CamelliaHotKeyMonitorSdk init(CollectorType collectorType) {
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        List<HotKeyServerAddr> addrList = new ArrayList<>();
        addrList.add(new HotKeyServerAddr("127.0.0.1", 7070));
        LocalConfHotKeyServerDiscovery discovery = new LocalConfHotKeyServerDiscovery("local", addrList);
        config.setDiscovery(discovery);
        config.setCollectorType(collectorType);
        config.setAsync(false);
        config.setAsyncQueueCapacity(5000);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        return new CamelliaHotKeyMonitorSdk(sdk, new CamelliaHotKeyMonitorSdkConfig());
    }


    private static void test(CollectorType collectorType, CamelliaHotKeyMonitorSdk monitorSdk) {

        String namespace1 = "namespace1";

        CamelliaStatistics statistics = new CamelliaStatistics();
        for (int i=0; i<100; i++) {
            long start = System.currentTimeMillis();
            for (int j=0; j<10000; j++) {
                String key = UUID.randomUUID().toString();
                monitorSdk.push(namespace1, key, 1);
            }
            statistics.update(System.currentTimeMillis() - start);
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("#####" + collectorType + "," + JSONObject.toJSON(statistics.getStatsDataAndReset()));
    }
}
