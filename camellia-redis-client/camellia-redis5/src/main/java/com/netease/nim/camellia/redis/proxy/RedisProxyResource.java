package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.base.resource.RedisType;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
public class RedisProxyResource extends Resource {

    private final RedisProxyJedisPool pool;

    public RedisProxyResource(RedisProxyJedisPool pool) {
        this.pool = pool;
        setUrl(RedisType.RedisProxy.getPrefix() + pool.getId());
    }

    public RedisProxyJedisPool getJedisPool() {
        return pool;
    }
}
