package com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin;

import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyCacheSdk;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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

    private boolean enable;

    /**
     * @param identityInfo tenant identity information，bid + bgroup can represent one tenant.
     */
    public HotKeyCache(IdentityInfo identityInfo, HotKeyCacheConfig config) {
        this.identityInfo = identityInfo;
        ProxyDynamicConf.registerCallback(this::reloadHotKeyCacheConfig);
        reloadHotKeyCacheConfig();

        this.hotKeyCacheSdk = CamelliaHotKeyCacheSdkFactory.getSdk(config);
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
        String namespace = Utils.getNamespaceOrSetDefault(identityInfo);
        String keyStr = Utils.bytesToString(key);
        HotValueWrapper value = hotKeyCacheSdk.getValue(namespace, keyStr);
        if (value == null) {
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getCache of hotKey = {}", key);
        }
        return value.hotValue;
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
        String namespace = Utils.getNamespaceOrSetDefault(identityInfo);
        String keyStr = Utils.bytesToString(key);
        hotKeyCacheSdk.keyDelete(namespace, keyStr);
    }

    /**
     * Check if the key is hotKey.
     *
     * @param key key
     * @return true/false
     */
    public boolean check(byte[] key) {
        if (!enable) {
            return false;
        }
        String namespace = Utils.getNamespaceOrSetDefault(identityInfo);
        String keyStr = Utils.bytesToString(key);
        return hotKeyCacheSdk.checkKey(namespace, keyStr);
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
        String namespace = Utils.getNamespaceOrSetDefault(identityInfo);
        String keyStr = Utils.bytesToString(key);
        hotKeyCacheSdk.tryBuildCache(namespace, keyStr, key1 -> new HotValueWrapper(new HotValue(value)));
        if (logger.isDebugEnabled()) {
            logger.debug("build hotKey's value success, key = {}", keyStr);
        }
    }

    private void reloadHotKeyCacheConfig() {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        this.enable = ProxyDynamicConf.getBoolean("hot.key.cache.enable", bid, bgroup, true);
    }


    private static class HotValueWrapper {
        private final HotValue hotValue;

        public HotValueWrapper(HotValue hotValue) {
            this.hotValue = hotValue;
        }
    }

}
