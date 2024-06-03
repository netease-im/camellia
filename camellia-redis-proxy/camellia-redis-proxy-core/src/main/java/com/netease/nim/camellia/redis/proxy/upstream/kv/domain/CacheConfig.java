package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.KeyMetaLRUCache;
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
    private final boolean metaCacheEnable;
    private boolean metaLocalCacheEnable;
    private boolean hashLocalCacheEnable;
    private boolean zsetLocalCacheEnable;
    private final KeyMetaLRUCache keyMetaLRUCache;
    private final HashLRUCache hashLRUCache;
    private final ZSetLRUCache zSetLRUCache;

    public CacheConfig(String namespace, boolean metaCacheEnable) {
        this.namespace = namespace;
        this.metaCacheEnable = metaCacheEnable;
        initCacheConfig();
        this.hashLRUCache = new HashLRUCache(namespace);
        this.keyMetaLRUCache = new KeyMetaLRUCache(namespace);
        this.zSetLRUCache = new ZSetLRUCache(namespace);
        ProxyDynamicConf.registerCallback(this::initCacheConfig);
    }

    private void initCacheConfig() {
        boolean metaLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.key.mete.local.cache.enable", true);
        if ((this.metaLocalCacheEnable && !metaLocalCacheEnable) || (!this.metaLocalCacheEnable && metaLocalCacheEnable)) {
            this.metaLocalCacheEnable = metaLocalCacheEnable;
            if (!metaLocalCacheEnable) {
                keyMetaLRUCache.clear();
            }
            logger.info("kv.key.mete.local.cache.enable = {}", metaLocalCacheEnable);
        }
        boolean hashLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.hash.local.cache.enable", true);
        if ((this.hashLocalCacheEnable && !hashLocalCacheEnable) || (!this.hashLocalCacheEnable && hashLocalCacheEnable)) {
            this.hashLocalCacheEnable = hashLocalCacheEnable;
            if (!hashLocalCacheEnable) {
                hashLRUCache.clear();
            }
            logger.info("kv.hash.local.cache.enable = {}", hashLocalCacheEnable);
        }
        boolean zsetLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.zset.local.cache.enable", true);
        if ((this.zsetLocalCacheEnable && !zsetLocalCacheEnable) || (!this.zsetLocalCacheEnable && zsetLocalCacheEnable)) {
            this.zsetLocalCacheEnable = zsetLocalCacheEnable;
            if (!zsetLocalCacheEnable) {
                zSetLRUCache.clear();
            }
            logger.info("kv.zset.local.cache.enable = {}", zsetLocalCacheEnable);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isMetaCacheEnable() {
        return metaCacheEnable;
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

    public KeyMetaLRUCache getKeyMetaLRUCache() {
        return keyMetaLRUCache;
    }

    public HashLRUCache getHashLRUCache() {
        return hashLRUCache;
    }

    public ZSetLRUCache getZSetLRUCache() {
        return zSetLRUCache;
    }

    public long metaCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.key.meta.cache.millis", 1000L * 60 * 10);
    }

    public int keyMetaCacheDelayMapSize() {
        return RedisKvConf.getInt(namespace, "kv.key.meta.cache.delay.map.size", 100000);
    }

    public long keyMetaCacheKeyDelayMinIntervalSeconds() {
        return RedisKvConf.getLong(namespace, "kv.key.meta.cache.delay.min.interval.seconds", 10);
    }

    public long keyMetaTimeoutMillis() {
        return RedisKvConf.getLong(namespace, "kv.key.meta.timeout.millis", 2000L);
    }

    public long cacheTimeoutMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.timeout.millis", 2000L);
    }

    public long hgetCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.hget.cache.millis", 5*60*1000L);
    }

    public long hgetallCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.hgetall.cache.millis", 5*60*1000L);
    }

    public long zsetMemberCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.zset.member.cache.millis", 5*60*1000L);
    }

    public long zsetRangeCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.zset.range.cache.millis", 5*60*1000L);
    }

}
