package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;

/**
 * Created by caojiajun on 2024/4/8
 */
public class KvConfig {

    private final String namespace;

    public KvConfig(String namespace) {
        this.namespace = namespace;
    }

    public int gcExecutorPoolSize() {
        return RedisKvConf.getInt(namespace, "kv.gc.executor.pool.size", 2);
    }

    public int gcExecutorQueueSize() {
        return RedisKvConf.getInt(namespace, "kv.gc.executor.queue.size", 100000);
    }

    public long gcBatchSleepMs() {
        return RedisKvConf.getLong(namespace, "kv.gc.executor.batch.sleep.ms", 5L);
    }

    public long gcDeleteKeyMetaTimeoutMillis() {
        return RedisKvConf.getLong(namespace, "kv.gc.delete.key.meta.timeout.millis", 10000L);
    }

    public int scanBatch() {
        return RedisKvConf.getInt(namespace, "kv.scan.batch", 100);
    }

    public int hashMaxSize() {
        return RedisKvConf.getInt(namespace, "kv.hash.max.size", Integer.MAX_VALUE);
    }

}
