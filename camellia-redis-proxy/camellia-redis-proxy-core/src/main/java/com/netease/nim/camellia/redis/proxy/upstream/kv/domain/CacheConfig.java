package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/4/8
 */
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("kv-lru-cache-estimate-size"));

    private final String namespace;
    private boolean metaLocalCacheEnable;
    private boolean hashLocalCacheEnable;
    private boolean zsetLocalCacheEnable;
    private boolean setLocalCacheEnable;
    private final KeyMetaLRUCache keyMetaLRUCache;
    private final HashLRUCache hashLRUCache;
    private final ZSetLRUCache zSetLRUCache;
    private final SetLRUCache setLRUCache;
    private final ZSetIndexLRUCache zSetIndexLRUCache;

    private boolean zsetVersion1KvWriteAsyncEnable;

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
        this.zSetIndexLRUCache = new ZSetIndexLRUCache(namespace);
        this.setLRUCache = new SetLRUCache(namespace);
        ProxyDynamicConf.registerCallback(this::initCacheConfig);
        scheduler.scheduleAtFixedRate(this::lruCacheEstimateSize, 10, 10, TimeUnit.SECONDS);
    }

    private long lastCheckLruCacheEstimateSizeTime = 0;

    private void lruCacheEstimateSize() {
        try {
            int intervalSeconds = RedisKvConf.getInt(namespace, "kv.lru.cache.estimate.size.interval.seconds", 60);
            if (intervalSeconds < 0) {
                return;
            }
            if (TimeCache.currentMillis - lastCheckLruCacheEstimateSizeTime < intervalSeconds * 1000L) {
                return;
            }
            lastCheckLruCacheEstimateSizeTime = TimeCache.currentMillis;
            logger.info("lru.cache.estimate.size, namespace = {}, type = key.meta, size.info = {}", namespace, keyMetaLRUCache.info());
            logger.info("lru.cache.estimate.size, namespace = {}, type = hash, size.info = {}", namespace, hashLRUCache.info());
            logger.info("lru.cache.estimate.size, namespace = {}, type = set, size.info = {}", namespace, setLRUCache.info());
            logger.info("lru.cache.estimate.size, namespace = {}, type = zset, size.info = {}", namespace, zSetLRUCache.info());
            logger.info("lru.cache.estimate.size, namespace = {}, type = zset.index, size.info = {}", namespace, zSetIndexLRUCache.info());
        } catch (Exception e) {
            logger.error("lru cache estimate size error", e);
        }
    }

    private void initCacheConfig() {
        boolean metaLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.key.meta.local.cache.enable", true);
        if ((this.metaLocalCacheEnable && !metaLocalCacheEnable) || (!this.metaLocalCacheEnable && metaLocalCacheEnable)) {
            this.metaLocalCacheEnable = metaLocalCacheEnable;
            if (!metaLocalCacheEnable && keyMetaLRUCache != null) {
                keyMetaLRUCache.clear();
            }
            logger.info("kv.key.meta.local.cache.enable = {}, namespace = {}", metaLocalCacheEnable, namespace);
        }
        boolean hashLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.hash.local.cache.enable", true);
        if ((this.hashLocalCacheEnable && !hashLocalCacheEnable) || (!this.hashLocalCacheEnable && hashLocalCacheEnable)) {
            this.hashLocalCacheEnable = hashLocalCacheEnable;
            if (!hashLocalCacheEnable && hashLRUCache != null) {
                hashLRUCache.clear();
            }
            logger.info("kv.hash.local.cache.enable = {}, namespace = {}", hashLocalCacheEnable, namespace);
        }
        boolean zsetLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.zset.local.cache.enable", true);
        if ((this.zsetLocalCacheEnable && !zsetLocalCacheEnable) || (!this.zsetLocalCacheEnable && zsetLocalCacheEnable)) {
            this.zsetLocalCacheEnable = zsetLocalCacheEnable;
            if (!zsetLocalCacheEnable && zSetLRUCache != null) {
                zSetLRUCache.clear();
            }
            if (!zsetLocalCacheEnable && zSetIndexLRUCache != null) {
                zSetIndexLRUCache.clear();
            }
            logger.info("kv.zset.local.cache.enable = {}, namespace = {}", zsetLocalCacheEnable, namespace);
        }
        boolean setLocalCacheEnable = RedisKvConf.getBoolean(namespace, "kv.set.local.cache.enable", true);
        if ((this.setLocalCacheEnable && !setLocalCacheEnable) || (!this.setLocalCacheEnable && setLocalCacheEnable)) {
            this.setLocalCacheEnable = setLocalCacheEnable;
            if (!setLocalCacheEnable && setLRUCache != null) {
                setLRUCache.clear();
            }
            logger.info("kv.set.local.cache.enable = {}, namespace = {}", setLocalCacheEnable, namespace);
        }
        boolean zsetVersion1KvWriteAsyncEnable = RedisKvConf.getBoolean(namespace, "kv.zset.version1.kv.async.write.enable", false);
        if ((this.zsetVersion1KvWriteAsyncEnable && !zsetVersion1KvWriteAsyncEnable) || (!this.zsetVersion1KvWriteAsyncEnable && zsetVersion1KvWriteAsyncEnable)) {
            this.zsetVersion1KvWriteAsyncEnable = zsetVersion1KvWriteAsyncEnable;
            logger.info("kv.zset.version1.kv.async.write.enable = {}, namespace = {}",zsetVersion1KvWriteAsyncEnable, namespace);
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

    public boolean isZSetVersion1KvWriteAsyncEnable() {
        return zsetVersion1KvWriteAsyncEnable;
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

    public ZSetIndexLRUCache getZSetIndexLRUCache() {
        return zSetIndexLRUCache;
    }

    public long cacheTimeoutMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.timeout.millis", 2000L);
    }

    public long zsetMemberCacheMillis() {
        return RedisKvConf.getLong(namespace, "kv.cache.zset.member.cache.millis", 5*60*1000L);
    }

}
