package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.upstream.cluster.RedisClusterClient;
import com.netease.nim.camellia.redis.proxy.upstream.proxies.RedisProxiesClient;
import com.netease.nim.camellia.redis.proxy.upstream.proxies.RedisProxiesDiscoveryClient;
import com.netease.nim.camellia.redis.proxy.upstream.sentinel.RedisSentinelClient;
import com.netease.nim.camellia.redis.proxy.upstream.sentinel.RedisSentinelSlavesClient;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.RedisStandaloneClient;
import com.netease.nim.camellia.redis.proxy.upstream.uds.RedisUnixDomainSocketClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface UpstreamRedisClientFactory {

    IUpstreamClient get(String url);

    IUpstreamClient remove(String url);

    List<IUpstreamClient> getAll();

    UpstreamRedisClientFactory DEFAULT = new Default();

    class Default implements UpstreamRedisClientFactory {

        private final Object lock = new Object();
        private final ConcurrentHashMap<String, IUpstreamClient> map = new ConcurrentHashMap<>();
        private int maxAttempts = Constants.Transpond.redisClusterMaxAttempts;

        public Default() {
        }

        public Default(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public IUpstreamClient get(RedisResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisStandaloneClient client0 = new RedisStandaloneClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisStandaloneClient client0 = new RedisStandaloneClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisUnixDomainSocketResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisUnixDomainSocketClient client0 = new RedisUnixDomainSocketClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisClusterResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisClusterClient client0 = new RedisClusterClient(resource, maxAttempts);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissClusterResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisClusterClient client0 = new RedisClusterClient(resource, maxAttempts);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisClusterSlavesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisClusterClient client0 = new RedisClusterClient(resource, maxAttempts);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissClusterSlavesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisClusterClient client0 = new RedisClusterClient(resource, maxAttempts);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisSentinelResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisSentinelClient client0 = new RedisSentinelClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissSentinelResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisSentinelClient client0 = new RedisSentinelClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisSentinelSlavesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisSentinelSlavesClient client0 = new RedisSentinelSlavesClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissSentinelSlavesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisSentinelSlavesClient client0 = new RedisSentinelSlavesClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisProxiesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisProxiesClient client0 = new RedisProxiesClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissProxiesResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisProxiesClient client0 = new RedisProxiesClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedisProxiesDiscoveryResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisProxiesDiscoveryClient client0 = new RedisProxiesDiscoveryClient(resource);
                            client0.start();
                            return client0;
                        });
            }
            return client;
        }

        public IUpstreamClient get(RedissProxiesDiscoveryResource resource) {
            IUpstreamClient client = map.get(resource.getUrl());
            if (client == null) {
                client = map.computeIfAbsent(resource.getUrl(),
                        u -> {
                            RedisProxiesDiscoveryClient client0 = new RedisProxiesDiscoveryClient(resource);
                            client0.start();
                            return client0;
                        });
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
                        } else if (resource instanceof RedissResource) {
                            client = get((RedissResource) resource);
                        } else if (resource instanceof RedisClusterResource) {
                            client = get((RedisClusterResource) resource);
                        } else if (resource instanceof RedissClusterResource) {
                            client = get((RedissClusterResource) resource);
                        } else if (resource instanceof RedisSentinelResource) {
                            client = get((RedisSentinelResource) resource);
                        } else if (resource instanceof RedissSentinelResource) {
                            client = get((RedissSentinelResource) resource);
                        } else if (resource instanceof RedisSentinelSlavesResource) {
                            client = get((RedisSentinelSlavesResource) resource);
                        } else if (resource instanceof RedissSentinelSlavesResource) {
                            client = get((RedissSentinelSlavesResource) resource);
                        } else if (resource instanceof RedisClusterSlavesResource) {
                            client = get((RedisClusterSlavesResource) resource);
                        } else if (resource instanceof RedissClusterSlavesResource) {
                            client = get((RedissClusterSlavesResource) resource);
                        } else if (resource instanceof RedisProxiesResource) {
                            client = get((RedisProxiesResource) resource);
                        } else if (resource instanceof RedissProxiesResource) {
                            client = get((RedissProxiesResource) resource);
                        } else if (resource instanceof RedisProxiesDiscoveryResource) {
                            client = get((RedisProxiesDiscoveryResource) resource);
                        } else if (resource instanceof RedissProxiesDiscoveryResource) {
                            client = get((RedissProxiesDiscoveryResource) resource);
                        } else if (resource instanceof RedisUnixDomainSocketResource) {
                            client = get((RedisUnixDomainSocketResource) resource);
                        } else {
                            throw new CamelliaRedisException("not support resource");
                        }
                    }
                }
            }
            return client;
        }

        @Override
        public IUpstreamClient remove(String url) {
            return map.remove(url);
        }

        @Override
        public List<IUpstreamClient> getAll() {
            return new ArrayList<>(map.values());
        }
    }
}
