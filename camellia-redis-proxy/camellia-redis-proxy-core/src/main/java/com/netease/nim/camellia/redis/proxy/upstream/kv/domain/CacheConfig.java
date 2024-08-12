package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.KeyMetaLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/4/8
 */
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private final String namespace;
    private boolean metaLocalCacheEnable;
    private boolean hashLocalCacheEnable;
    private boolean zsetLocalCacheEnable;
    private boolean setLocalCacheEnable;
    private final KeyMetaLRUCache keyMetaLRUCache;
    private final HashLRUCache hashLRUCache;
    private final ZSetLRUCache zSetLRUCache;
    private final SetLRUCache setLRUCache;

    public CacheConfig(String namespace) {
        this.namespace = namespace;
        initCacheConfig();
        logger.info("namespace = {}, metaLocalCacheEnable = {}", namespace, metaLocalCacheEnable);
        logger.info("namespace = {}, hashLocalCacheEnable = {}", namespace, hashLocalCacheEnable);
        logger.info("namespace = {}, zsetLocalCacheEnable = {}", namespace, zsetLocalCacheEnable);
        logger.info("namespace = {}, setLocalCacheEnable = {}", namespace, setLocalCacheEnable);
        this.hashLRUCache = new HashLRUCache(namespace);
        this.keyMetaLRUCache = new KeyMetaLRUCache(namespace);
        this.zSetLRUCache = new ZSetLRUCache(namespace);
        this.setLRUCache = new SetLRUCache(namespace);
        ProxyDynamicConf.registerCallback(this::initCacheConfig);
    }

    private void initCacheConfig() {
        boolean metaLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.key.meta.local.cache.enable", true);
        if ((this.metaLocalCacheEnable && !metaLocalCacheEnable) || (!this.metaLocalCacheEnable && metaLocalCacheEnable)) {
            this.metaLocalCacheEnable = metaLocalCacheEnable;
            if (!metaLocalCacheEnable && keyMetaLRUCache != null) {
                keyMetaLRUCache.clear();
            }
            logger.info("kv.key.meta.local.cache.enable = {}", metaLocalCacheEnable);
        }
        boolean hashLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.hash.local.cache.enable", true);
        if ((this.hashLocalCacheEnable && !hashLocalCacheEnable) || (!this.hashLocalCacheEnable && hashLocalCacheEnable)) {
            this.hashLocalCacheEnable = hashLocalCacheEnable;
            if (!hashLocalCacheEnable && hashLRUCache != null) {
                hashLRUCache.clear();
            }
            logger.info("kv.hash.local.cache.enable = {}", hashLocalCacheEnable);
        }
        boolean zsetLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.zset.local.cache.enable", true);
        if ((this.zsetLocalCacheEnable && !zsetLocalCacheEnable) || (!this.zsetLocalCacheEnable && zsetLocalCacheEnable)) {
            this.zsetLocalCacheEnable = zsetLocalCacheEnable;
            if (!zsetLocalCacheEnable && zSetLRUCache != null) {
                zSetLRUCache.clear();
            }
            logger.info("kv.zset.local.cache.enable = {}", zsetLocalCacheEnable);
        }
        boolean setLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.set.local.cache.enable", true);
        if ((this.setLocalCacheEnable && !setLocalCacheEnable) || (!this.setLocalCacheEnable && setLocalCacheEnable)) {
            this.setLocalCacheEnable = setLocalCacheEnable;
            if (!setLocalCacheEnable && setLRUCache != null) {
                setLRUCache.clear();
            }
            logger.info("kv.set.local.cache.enable = {}", setLocalCacheEnable);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isMetaLocalCacheEnable() {
        return metaLocalCacheEnable;
    }

    public boolean isHashLocalCacheEnable() {
        return hashLocalCacheEnable;
    }

    public boolean isZSetLocalCacheEnable() {
        return zsetLocalCacheEnable;
    }

    public boolean isSetLocalCacheEnable() {
        return setLocalCacheEnable;
    }

    public KeyMetaLRUCache getKeyMetaLRUCache() {
        return keyMetaLRUCache;
    }

    public HashLRUCache getHashLRUCache() {
        return hashLRUCache;
    }

    public ZSetLRUCache getZSetLRUCache() {
        return zSetLRUCache;
    }

    public SetLRUCache getSetLRUCache() {
        return setLRUCache;
    }

    public long cacheTimeoutMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.timeout.millis", 2000L);
    }

    public long smembersCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.smembers.cache.millis", 5*60*1000L);
    }

    public long zsetMemberCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.zset.member.cache.millis", 5*60*1000L);
    }

}
