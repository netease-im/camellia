package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.redis.base.resource.CamelliaRedisProxyResource;
import redis.clients.jedis.JedisPool;

/**
 * 该方法应该实现JedisPool的缓存和动态更新，每次redis调用都会请求本方法获取JedisPool
 * Created by caojiajun on 2020/3/6.
 */
public interface CamelliaRedisProxyFactory {

    JedisPool initOrGet(CamelliaRedisProxyResource resource);

}
