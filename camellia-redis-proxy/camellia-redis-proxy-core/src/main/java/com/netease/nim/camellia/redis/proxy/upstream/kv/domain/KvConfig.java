package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

/**
 * Created by caojiajun on 2024/4/8
 */
public class KvConfig {

    private final String namespace;

    public KvConfig(String namespace) {
        this.namespace = namespace;
    }

    public int scanBatch() {
        int scanBatch = ProxyDynamicConf.getInt(namespace + ".kv.scan.batch", -1);
        if (scanBatch > 0) {
            return scanBatch;
        }
        return ProxyDynamicConf.getInt("kv.scan.batch", 100);
    }

    public int hashMaxSize() {
        int hashMaxSize = ProxyDynamicConf.getInt(namespace + ".kv.hash.max.size", -1);
        if (hashMaxSize > 0) {
            return hashMaxSize;
        }
        return ProxyDynamicConf.getInt("kv.hash.max.size", Integer.MAX_VALUE);
    }

}
