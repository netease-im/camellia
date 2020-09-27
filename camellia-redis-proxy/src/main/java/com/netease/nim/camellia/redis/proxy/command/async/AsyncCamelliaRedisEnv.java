package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.redis.proxy.conf.MultiWriteType;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class AsyncCamelliaRedisEnv {

    private AsyncNettyClientFactory clientFactory = AsyncNettyClientFactory.DEFAULT;

    private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();
    private MultiWriteType multiWriteType = MultiWriteType.FIRST_RESOURCE_ONLY;

    private AsyncCamelliaRedisEnv() {
    }

    private AsyncCamelliaRedisEnv(AsyncNettyClientFactory clientFactory, ProxyEnv proxyEnv) {
        this.clientFactory = clientFactory;
        this.proxyEnv = proxyEnv;
    }

    public static AsyncCamelliaRedisEnv defaultRedisEnv() {
        return new AsyncCamelliaRedisEnv();
    }

    public AsyncNettyClientFactory getClientFactory() {
        return clientFactory;
    }

    public ProxyEnv getProxyEnv() {
        return proxyEnv;
    }

    public MultiWriteType getMultiWriteType() {
        return multiWriteType;
    }

    public static class Builder {
        private final AsyncCamelliaRedisEnv redisEnv;
        public Builder() {
            redisEnv = new AsyncCamelliaRedisEnv();
        }

        public Builder(AsyncCamelliaRedisEnv redisEnv) {
            this.redisEnv = new AsyncCamelliaRedisEnv(redisEnv.clientFactory, redisEnv.proxyEnv);
        }

        public Builder clientFactory(AsyncNettyClientFactory clientFactory) {
            if (clientFactory != null) {
                redisEnv.clientFactory = clientFactory;
            }
            return this;
        }

        public Builder proxyEnv(ProxyEnv proxyEnv) {
            if (proxyEnv != null) {
                redisEnv.proxyEnv = proxyEnv;
            }
            return this;
        }

        public Builder multiWriteType(MultiWriteType multiWriteType) {
            if (multiWriteType != null) {
                redisEnv.multiWriteType = multiWriteType;
            }
            return this;
        }

        public AsyncCamelliaRedisEnv build() {
            return redisEnv;
        }
    }
}
