package com.netease.nim.camellia.redis.base.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2024/4/17
 */
public class RedisKvResource extends Resource {

    private final String namespace;

    public RedisKvResource(String namespace) {
        this.namespace = namespace;
        setUrl(RedisType.RedisKV.getPrefix() + namespace);
    }

    public String getNamespace() {
        return namespace;
    }
}
