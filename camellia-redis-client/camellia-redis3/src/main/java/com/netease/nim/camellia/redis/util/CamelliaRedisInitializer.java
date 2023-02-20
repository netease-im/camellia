package com.netease.nim.camellia.redis.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.jedis.CamelliaJedis;
import com.netease.nim.camellia.redis.jediscluster.CamelliaJedisCluster;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;
import com.netease.nim.camellia.redis.resource.RedisClientResourceUtil;

/**
 *
 * Created by caojiajun on 2020/7/31
 */
public class CamelliaRedisInitializer {

    public static ICamelliaRedis init(Resource resource, CamelliaRedisEnv env) {
        ICamelliaRedis redis;
        Resource originalResource = RedisClientResourceUtil.parseResourceByUrl(resource);
        if (originalResource instanceof RedisResource) {
            redis = new CamelliaJedis((RedisResource) originalResource, env);
        } else if (originalResource instanceof RedisSentinelResource) {
            redis = new CamelliaJedis((RedisSentinelResource) originalResource, env);
        } else if (originalResource instanceof RedisClusterResource) {
            redis = new CamelliaJedisCluster((RedisClusterResource) originalResource, env);
        } else if (originalResource instanceof RedisProxyResource) {
            redis = new CamelliaJedis((RedisProxyResource) originalResource, env);
        } else if (originalResource instanceof CamelliaRedisProxyResource) {
            redis = new CamelliaJedis((CamelliaRedisProxyResource) originalResource, env);
        } else if (originalResource instanceof RedisSentinelSlavesResource) {
            redis = new CamelliaJedis((RedisSentinelSlavesResource) originalResource, env);
        } else if (originalResource instanceof RedisProxiesResource) {
            redis = new CamelliaJedis((RedisProxiesResource) originalResource, env);
        } else if (originalResource instanceof RedisProxiesDiscoveryResource) {
            redis = new CamelliaJedis((RedisProxiesDiscoveryResource) originalResource, env);
        } else {
            throw new UnsupportedOperationException();
        }
        return redis;
    }
}
