package com.netease.nim.camellia.hot.key.sdk.samples;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.sdk.*;
import com.netease.nim.camellia.hot.key.sdk.collect.CollectorType;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;

/**
 * Created by caojiajun on 2023/5/9
 */
public class Test {

    public static void main(String[] args) {

        //非必填，可以标识来源，进程内唯一，从而hot-key-server回调热key的时候，会带上source列表，从而帮助定位热key的来源
        HotKeyConstants.Client.source = "xxx";

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();

        config.setDiscovery(null);//设置一个发现器，默认提供zk/eureka，也可以自己实现基于etcd/consul/nacos等其他注册中心
        config.setCollectorType(CollectorType.Caffeine);//默认是Caffeine，还可以使用ConcurrentLinkedHashMap
        config.setAsync(false);//是否异步，默认false，如果Collector的延迟不满足业务要求，则可以使用异步采集
        config.setAsyncQueueCapacity(100000);//异步队列的大小，默认10w
        //如果需要同时访问多个集群，则需要初始化多个sdk，否则初始化一个实例即可
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //基于CamelliaHotKeySdk，可以构造CamelliaHotKeyMonitorSdk
        testMonitor(sdk);

        //基于CamelliaHotKeySdk，可以构造CamelliaHotKeyCacheSdk
        testCache(sdk);
    }

    private static void testMonitor(CamelliaHotKeySdk sdk) {
        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig config = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, config);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }

    private static void testCache(CamelliaHotKeySdk sdk) {
        //设置相关参数，按需设置，一般来说默认即可
        CamelliaHotKeyCacheSdkConfig config = new CamelliaHotKeyCacheSdkConfig();
        config.setCapacity(1000);//最多保留多少个热key的缓存，各个namespace之间是隔离的，独立计算容量
        config.setCacheNull(true);//是否缓存null

        //初始化CamelliaHotKeyCacheSdk，一般来说如果对于上述配置策略没有特殊要求的话，或者缓存不想互相挤占的话，全局一个即可
        CamelliaHotKeyCacheSdk cacheSdk = new CamelliaHotKeyCacheSdk(sdk, config);

        String namespace1 = "db";
        String value1 = cacheSdk.getValue(namespace1, "key1", Test::getValueFromDb);
        System.out.println(value1);

        String namespace2 = "redis";
        String value2 = cacheSdk.getValue(namespace2, "key1", Test::getValueRedis);
        System.out.println(value2);
    }

    private static String getValueFromDb(String key) {
        return key + "-value";
    }

    private static String getValueRedis(String key) {
        return key + "-value";
    }
}
