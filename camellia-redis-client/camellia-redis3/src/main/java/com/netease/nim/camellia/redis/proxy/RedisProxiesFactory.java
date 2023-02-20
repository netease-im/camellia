package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.redis.base.resource.RedisProxiesDiscoveryResource;
import redis.clients.jedis.JedisPool;

/**
 * Created by caojiajun on 2023/2/20
 */
public interface RedisProxiesFactory {

    JedisPool initOrGet(RedisProxiesDiscoveryResource resource);
}
