package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.util.ResourceSelector;
import com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class RedisProxyEnv {

    private UpstreamRedisClientFactory clientFactory = UpstreamRedisClientFactory.DEFAULT;
    private ResourceSelector.ResourceChecker resourceChecker;

    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();
    private MultiWriteMode multiWriteMode = MultiWriteMode.FIRST_RESOURCE_ONLY;

    private RedisProxyEnv() {
    }

    private RedisProxyEnv(UpstreamRedisClientFactory clientFactory, ResourceSelector.ResourceChecker resourceChecker, ProxyEnv proxyEnv) {
        this.clientFactory = clientFactory;
        this.resourceChecker = resourceChecker;
        this.proxyEnv = proxyEnv;
    }

    public static RedisProxyEnv defaultRedisEnv() {
        return new RedisProxyEnv();
    }

    public UpstreamRedisClientFactory getClientFactory() {
        return clientFactory;
    }

    public ResourceSelector.ResourceChecker getResourceChecker() {
        return resourceChecker;
    }

    public ProxyEnv getProxyEnv() {
        return proxyEnv;
    }

    public MultiWriteMode getMultiWriteMode() {
        return multiWriteMode;
    }

    public static class Builder {
        private final RedisProxyEnv redisEnv;
        public Builder() {
            redisEnv = new RedisProxyEnv();
        }

        public Builder(RedisProxyEnv redisEnv) {
            this.redisEnv = new RedisProxyEnv(redisEnv.clientFactory, redisEnv.resourceChecker, redisEnv.proxyEnv);
        }

        public Builder clientFactory(UpstreamRedisClientFactory clientFactory) {
            if (clientFactory != null) {
                redisEnv.clientFactory = clientFactory;
            }
            return this;
        }

        public Builder resourceChecker(ResourceSelector.ResourceChecker resourceChecker) {
            if (resourceChecker != null) {
                redisEnv.resourceChecker = resourceChecker;
            }
            return this;
        }

        public Builder proxyEnv(ProxyEnv proxyEnv) {
            if (proxyEnv != null) {
                redisEnv.proxyEnv = proxyEnv;
            }
            return this;
        }

        public Builder multiWriteMode(MultiWriteMode multiWriteMode) {
            if (multiWriteMode != null) {
                redisEnv.multiWriteMode = multiWriteMode;
            }
            return this;
        }

        public RedisProxyEnv build() {
            return redisEnv;
        }
    }
}
