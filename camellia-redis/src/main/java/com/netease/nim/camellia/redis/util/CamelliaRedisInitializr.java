package com.netease.nim.camellia.redis.util;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.jedis.CamelliaJedis;
import com.netease.nim.camellia.redis.jediscluster.CamelliaJedisCluster;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;
import com.netease.nim.camellia.redis.resource.*;

/**
 *
 * Created by caojiajun on 2020/7/31
 */
public class CamelliaRedisInitializr {

    public static ICamelliaRedis init(Resource resource, CamelliaRedisEnv env) {
        ICamelliaRedis redis;
        Resource originalResource = RedisResourceUtil.parseResourceByUrl(resource);
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
        } else {
            throw new UnsupportedOperationException();
        }
        return redis;
    }
}
