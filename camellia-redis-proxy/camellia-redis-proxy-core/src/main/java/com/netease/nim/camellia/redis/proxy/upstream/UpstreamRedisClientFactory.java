package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.cluster.RedisClusterClient;
import com.netease.nim.camellia.redis.proxy.upstream.proxies.RedisProxiesClient;
import com.netease.nim.camellia.redis.proxy.upstream.proxies.RedisProxiesDiscoveryClient;
import com.netease.nim.camellia.redis.proxy.upstream.sentinel.RedisSentinelClient;
import com.netease.nim.camellia.redis.proxy.upstream.sentinel.RedisSentinelSlavesClient;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.RedisStandaloneClient;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CachedResourceChecker;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface UpstreamRedisClientFactory {

    IUpstreamClient get(String url);

    UpstreamRedisClientFactory DEFAULT = new Default();

    class Default implements UpstreamRedisClientFactory {

        private final Object lock = new Object();
        private final ConcurrentHashMap<String, IUpstreamClient> map = new ConcurrentHashMap<>();
        private int maxAttempts = Constants.Transpond.redisClusterMaxAttempts;

        public Default() {
            schedule();
        }

        public Default(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            schedule();
        }

        private void schedule() {
            int intervalSeconds = ProxyDynamicConf.getInt("check.redis.resource.valid.interval.seconds", 5);
            ExecutorUtils.scheduleAtFixedRate(() -> {
                for (Map.Entry<String, IUpstreamClient> entry : map.entrySet()) {
                    CachedResourceChecker.getInstance().updateCache(new Resource(entry.getKey()), entry.getValue().isValid());
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }

        public IUpstreamClient get(RedisResource redisResource) {
            IUpstreamClient client = map.get(redisResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisResource.getUrl(),
                        k -> new RedisStandaloneClient(redisResource));
            }
            return client;
        }

        public IUpstreamClient get(RedisClusterResource redisClusterResource) {
            IUpstreamClient client = map.get(redisClusterResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisClusterResource.getUrl(),
                        k -> new RedisClusterClient(redisClusterResource, maxAttempts));
            }
            return client;
        }

        public IUpstreamClient get(RedisClusterSlavesResource redisClusterSlavesResource) {
            IUpstreamClient client = map.get(redisClusterSlavesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisClusterSlavesResource.getUrl(),
                        k -> new RedisClusterClient(redisClusterSlavesResource, maxAttempts));
            }
            return client;
        }

        public IUpstreamClient get(RedisSentinelResource redisSentinelResource) {
            IUpstreamClient client = map.get(redisSentinelResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisSentinelResource.getUrl(),
                        k -> new RedisSentinelClient(redisSentinelResource));
            }
            return client;
        }

        public IUpstreamClient get(RedisSentinelSlavesResource redisSentinelSlavesResource) {
            IUpstreamClient client = map.get(redisSentinelSlavesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisSentinelSlavesResource.getUrl(),
                        k -> new RedisSentinelSlavesClient(redisSentinelSlavesResource));
            }
            return client;
        }

        public IUpstreamClient get(RedisProxiesResource redisProxiesResource) {
            IUpstreamClient client = map.get(redisProxiesResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisProxiesResource.getUrl(),
                        k -> new RedisProxiesClient(redisProxiesResource));
            }
            return client;
        }

        public IUpstreamClient get(RedisProxiesDiscoveryResource redisProxiesDiscoveryResource) {
            IUpstreamClient client = map.get(redisProxiesDiscoveryResource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(redisProxiesDiscoveryResource.getUrl(),
                        k -> new RedisProxiesDiscoveryClient(redisProxiesDiscoveryResource));
            }
            return client;
        }

        @Override
        public IUpstreamClient get(String url) {
            IUpstreamClient client = map.get(url);
            if (client == null) {
                synchronized (lock) {
                    client = map.get(url);
                    if (client == null) {
                        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
                        if (resource instanceof RedisResource) {
                            client = get((RedisResource) resource);
                        } else if (resource instanceof RedisClusterResource) {
                            client = get((RedisClusterResource) resource);
                        } else if (resource instanceof RedisSentinelResource) {
                            client = get((RedisSentinelResource) resource);
                        } else if (resource instanceof RedisSentinelSlavesResource) {
                            client = get((RedisSentinelSlavesResource) resource);
                        } else if (resource instanceof RedisClusterSlavesResource) {
                            client = get((RedisClusterSlavesResource) resource);
                        } else if (resource instanceof RedisProxiesResource) {
                            client = get((RedisProxiesResource) resource);
                        } else if (resource instanceof RedisProxiesDiscoveryResource) {
                            client = get((RedisProxiesDiscoveryResource) resource);
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
