package com.netease.nim.camellia.redis.proxy.plugin.hotkeycacheserver;

import com.netease.nim.camellia.hot.key.extensions.discovery.zk.ZkHotKeyServerDiscovery;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.hot.key.sdk.CamelliaHotKeySdk;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyCacheSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * 工厂，返回全局唯一CamelliaHotKeyCacheSdk
 */
public class CamelliaHotKeyCacheSdkFactory {
    public static final String CAMELLIA_HOT_KEY_SERVER = "camellia-hot-key-server";
    public static final String CAMELLIA_HOT_KEY = "/camellia-hot-key";
    public static final String ZK_URL = "127.0.0.1:2181";
    private static volatile CamelliaHotKeyCacheSdk cacheSdk;

    public static void init(IdentityInfo identityInfo) {
        int cacheMaxCapacity = ProxyDynamicConf.getInt("hot.key.cache.max.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheMaxCapacity);
        boolean cacheNull = ProxyDynamicConf.getBoolean("hot.key.cache.null", identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheNeedCacheNull);

        // 配置zk发现Server机制
        String zkUrl = ProxyDynamicConf.getString("hot.key.cache.zkUrl",
                identityInfo.getBid(), identityInfo.getBgroup(), ZK_URL);
        String basePath = ProxyDynamicConf.getString("hot.key.cache.basePath",
                identityInfo.getBid(), identityInfo.getBgroup(), CAMELLIA_HOT_KEY);
        String applicationName = ProxyDynamicConf.getString("hot.key.cache.applicationName",
                identityInfo.getBid(), identityInfo.getBgroup(), CAMELLIA_HOT_KEY_SERVER);
        ZkHotKeyServerDiscovery discovery = new ZkHotKeyServerDiscovery(zkUrl, basePath, applicationName);
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(discovery);

        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);
        //设置相关参数，按需设置，一般来说默认即可
        CamelliaHotKeyCacheSdkConfig camelliaHotKeyCacheSdkConfig = new CamelliaHotKeyCacheSdkConfig();
        camelliaHotKeyCacheSdkConfig.setCapacity(cacheMaxCapacity);//最多保留多少个热key的缓存，各个namespace之间是隔离的，独立计算容量
        camelliaHotKeyCacheSdkConfig.setCacheNull(cacheNull);//是否缓存null
        cacheSdk = new CamelliaHotKeyCacheSdk(sdk, camelliaHotKeyCacheSdkConfig);
    }

    public static CamelliaHotKeyCacheSdk getSdk(IdentityInfo identityInfo) {
        if (cacheSdk == null) {
            synchronized (CamelliaHotKeyCacheSdkFactory.class) {
                if (cacheSdk == null) {
                    init(identityInfo);
                }
            }
        }
        return cacheSdk;
    }

}
