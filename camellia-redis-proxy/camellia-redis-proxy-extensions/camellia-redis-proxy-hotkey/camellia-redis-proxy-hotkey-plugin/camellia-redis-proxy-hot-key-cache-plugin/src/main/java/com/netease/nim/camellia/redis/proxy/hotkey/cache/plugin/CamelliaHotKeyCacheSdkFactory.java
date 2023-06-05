package com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin;

import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * 工厂，返回全局唯一CamelliaHotKeyCacheSdk
 */
public class CamelliaHotKeyCacheSdkFactory {

    private static volatile ICamelliaHotKeyCacheSdk cacheSdk;

    private static void init(HotKeyCacheConfig hotKeyCacheConfig) {

        int cacheMaxCapacity = ProxyDynamicConf.getInt("hot.key.cache.max.capacity", Constants.Server.hotKeyCacheMaxCapacity);
        boolean cacheNull = ProxyDynamicConf.getBoolean("hot.key.cache.null", Constants.Server.hotKeyCacheNeedCacheNull);

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(hotKeyCacheConfig.getDiscovery());

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);
        //设置相关参数，按需设置，一般来说默认即可
        CamelliaHotKeyCacheSdkConfig camelliaHotKeyCacheSdkConfig = new CamelliaHotKeyCacheSdkConfig();
        camelliaHotKeyCacheSdkConfig.setCapacity(cacheMaxCapacity);//最多保留多少个热key的缓存，各个namespace之间是隔离的，独立计算容量
        camelliaHotKeyCacheSdkConfig.setCacheNull(cacheNull);//是否缓存null
        cacheSdk = new CamelliaHotKeyCacheSdk(sdk, camelliaHotKeyCacheSdkConfig);
    }

    /**
     * 获得全局唯一的sdk
     * <p>Get the globally unique sdk
     */
    public static ICamelliaHotKeyCacheSdk getSdk(HotKeyCacheConfig hotKeyCacheConfig) {
        if (cacheSdk == null) {
            synchronized (CamelliaHotKeyCacheSdkFactory.class) {
                if (cacheSdk == null) {
                    init(hotKeyCacheConfig);
                }
            }
        }
        return cacheSdk;
    }

}
