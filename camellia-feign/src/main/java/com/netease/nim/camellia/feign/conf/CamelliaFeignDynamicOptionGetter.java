package com.netease.nim.camellia.feign.conf;

import com.netease.nim.camellia.core.util.DynamicValueGetter;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicRouteConfGetter;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/28
 */
public interface CamelliaFeignDynamicOptionGetter {

    DynamicOption getDynamicOption(long bid, String bgroup);

    default DynamicRouteConfGetter getDynamicRouteConfGetter(long bid) {
        return null;
    }

    public static class DefaultCamelliaFeignDynamicOptionGetter implements CamelliaFeignDynamicOptionGetter {
        private final long connectTimeout;
        private final TimeUnit connectTimeoutUnit;
        private final long readTimeout;
        private final TimeUnit readTimeoutUnit;
        private final boolean followRedirects;
        private final boolean circuitBreakerEnable;

        public DefaultCamelliaFeignDynamicOptionGetter(long connectTimeout, TimeUnit connectTimeoutUnit,
                                                       long readTimeout, TimeUnit readTimeoutUnit, boolean followRedirects, boolean circuitBreakerEnable) {
            this.connectTimeout = connectTimeout;
            this.connectTimeoutUnit = connectTimeoutUnit;
            this.readTimeout = readTimeout;
            this.readTimeoutUnit = readTimeoutUnit;
            this.followRedirects = followRedirects;
            this.circuitBreakerEnable = circuitBreakerEnable;
        }

        public DefaultCamelliaFeignDynamicOptionGetter(long connectTimeoutMillis, long readTimeoutMillis, boolean circuitBreakerEnable) {
            this(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis, TimeUnit.MILLISECONDS, false, circuitBreakerEnable);
        }

        public DefaultCamelliaFeignDynamicOptionGetter(long connectTimeoutMillis, long readTimeoutMillis) {
            this(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis, TimeUnit.MILLISECONDS, false, false);
        }

        public DefaultCamelliaFeignDynamicOptionGetter(long timeoutMillis, boolean circuitBreakerEnable) {
            this(timeoutMillis, TimeUnit.MILLISECONDS, timeoutMillis, TimeUnit.MILLISECONDS, false, circuitBreakerEnable);
        }

        public DefaultCamelliaFeignDynamicOptionGetter(long timeoutMillis) {
            this(timeoutMillis, TimeUnit.MILLISECONDS, timeoutMillis, TimeUnit.MILLISECONDS, false, false);
        }

        @Override
        public DynamicOption getDynamicOption(long bid, String bgroup) {
            DynamicValueGetter<Long> connectTimeoutGetter = () -> connectTimeout;
            DynamicValueGetter<TimeUnit> connectTimeoutUnitGetter = () -> connectTimeoutUnit;
            DynamicValueGetter<Long> readTimeoutGetter = () -> readTimeout;
            DynamicValueGetter<TimeUnit> readTimeoutUnitGetter = () -> readTimeoutUnit;
            DynamicValueGetter<Boolean> followRedirectsGetter = () -> followRedirects;
            CircuitBreakerConfig circuitBreakerConfig = null;
            if (circuitBreakerEnable) {
                circuitBreakerConfig = new CircuitBreakerConfig();
                circuitBreakerConfig.setName("bid=" + bid + ",bgroup=" + bgroup);
            }
            return new DynamicOption.Builder()
                    .readTimeoutTimeout(readTimeoutGetter, readTimeoutUnitGetter)
                    .connectTimeout(connectTimeoutGetter, connectTimeoutUnitGetter)
                    .followRedirects(followRedirectsGetter)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .build();
        }
    }
}
