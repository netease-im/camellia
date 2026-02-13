package com.netease.nim.camellia.redis.proxy.hotkeyserver.cache;

import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.HotValue;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于hot-key-server发现机制的HotKeyCache
 * <p>Hot Key Cache based on hot-key-server discovery mechanism
 */
public class HotKeyCache {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyCache.class);

    private final IdentityInfo identityInfo;

    /**
     * 全局唯一的sdk
     * <p>globally unique sdk
     */
    private final ICamelliaHotKeyCacheSdk hotKeyCacheSdk;
    private final HotKeyCacheKeyChecker keyChecker;

    private boolean enable;
    private boolean cacheNull;
    private String namespace;


    /**
     * @param identityInfo tenant identity information，bid + bgroup can represent one tenant.
     */
    public HotKeyCache(IdentityInfo identityInfo, ICamelliaHotKeyCacheSdk hotKeyCacheSdk, HotKeyCacheKeyChecker hotKeyCacheKeyChecker) {
        this.identityInfo = identityInfo;
        this.keyChecker = hotKeyCacheKeyChecker;
        ProxyDynamicConf.registerCallback(this::reloadHotKeyCacheConfig);
        reloadHotKeyCacheConfig();
        this.hotKeyCacheSdk = hotKeyCacheSdk;
        logger.info("HotKeyCache init success, identityInfo = {}", identityInfo);
    }


    /**
     * 获取本地缓存
     *
     * @param key key
     * @return HotValue
     */
    public HotValue getCache(byte[] key) {
        if (!enable) {
            return null;
        }
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return null;
        }
        String keyStr = Utils.bytesToString(key);
        HotValue hotValue = hotKeyCacheSdk.getValue(namespace, keyStr);
        if (hotValue == null) {
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getCache of hotKey = {}", key);
        }
        return hotValue;
    }

    /**
     * Del cache
     *
     * @param key key
     */
    public void delCache(byte[] key) {
        if (!enable) {
            return;
        }
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return;
        }
        String keyStr = Utils.bytesToString(key);
        hotKeyCacheSdk.keyDelete(namespace, keyStr);
    }

    /**
     * Check if the key is hotKey.
     *
     * @param key key
     * @return true/false
     */
    public boolean isHotKey(byte[] key) {
        if (!enable) {
            return false;
        }
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return false;
        }
        String keyStr = Utils.bytesToString(key);
        return hotKeyCacheSdk.isHotKey(namespace, keyStr);
    }

    /**
     * Try build hot key.
     *
     * @param key   key
     * @param value value
     */
    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        if (!enable) {
            return;
        }
        if (value == null && !cacheNull) {
            return;
        }
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return;
        }
        String keyStr = Utils.bytesToString(key);
        hotKeyCacheSdk.setValue(namespace, keyStr, new HotValue(value));
        if (logger.isDebugEnabled()) {
            logger.debug("build hotKey's value success, key = {}", keyStr);
        }
    }

    //更新配置
    private void reloadHotKeyCacheConfig() {
        Long bid = identityInfo.bid();
        String bgroup = identityInfo.bgroup();
        this.enable = ProxyDynamicConf.getBoolean("hot.key.cache.enable", bid, bgroup, true);
        this.cacheNull = ProxyDynamicConf.getBoolean("hot.key.cache.null", bid, bgroup, Constants.Server.hotKeyCacheNeedCacheNull);
        this.namespace = ProxyDynamicConf.getString("hot.key.server.cache.namespace",
                identityInfo.bid(), identityInfo.bgroup(), Utils.getNamespaceOrSetDefault(identityInfo.bid(), identityInfo.bgroup()));
    }

}
