package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.util.LockMap;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.resource.RedisResource;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface AsyncNettyClientFactory {

    AsyncClient get(String url);

    public static AsyncNettyClientFactory DEFAULT = new Default();

    public static class Default implements AsyncNettyClientFactory {

        private final LockMap lockMap = new LockMap();
        private final Map<String, AsyncClient> map = new HashMap<>();
        private int maxAttempts = Constants.Transpond.redisClusterMaxAttempts;

        public Default() {
        }

        public Default(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public AsyncClient get(RedisResource redisResource) {
            AsyncClient client = map.get(redisResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisResource.getUrl(),
                        k -> new AsyncCamelliaRedisClient(redisResource));
            }
            return client;
        }

        public AsyncClient get(RedisClusterResource redisClusterResource) {
            AsyncClient client = map.get(redisClusterResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisClusterResource.getUrl(),
                        k -> new AsyncCamelliaRedisClusterClient(redisClusterResource, maxAttempts));
            }
            return client;
        }

        public AsyncClient get(RedisSentinelResource redisSentinelResource) {
            AsyncClient client = map.get(redisSentinelResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisSentinelResource.getUrl(),
                        k -> new AsyncCamelliaRedisSentinelClient(redisSentinelResource));
            }
            return client;
        }

        @Override
        public AsyncClient get(String url) {
            AsyncClient client = map.get(url);
            if (client == null) {
                synchronized (lockMap.getLockObj(url)) {
                    client = map.get(url);
                    if (client == null) {
                        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
                        if (resource instanceof RedisResource) {
                            client = get((RedisResource) resource);
                        } else if (resource instanceof RedisClusterResource) {
                            client = get((RedisClusterResource) resource);
                        } else if (resource instanceof RedisSentinelResource) {
                            client = get((RedisSentinelResource) resource);
                        } else {
                            throw new CamelliaRedisException("not support resource");
                        }
                    }
                }
            }
            return client;
        }
    }
}
