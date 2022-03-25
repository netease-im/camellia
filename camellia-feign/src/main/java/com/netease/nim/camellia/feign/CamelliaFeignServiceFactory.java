package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.util.DynamicValueGetter;
import com.netease.nim.camellia.core.util.LockMap;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicOptionClient;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 管理camellia-feign客户端实例的工厂类
 * Created by caojiajun on 2022/3/24
 */
public class CamelliaFeignServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaFeignServiceFactory.class);

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> map = new ConcurrentHashMap<>();
    private final LockMap lockMap = new LockMap();

    private final CamelliaFeignProps feignProps;
    private final CamelliaFeignEnv feignEnv;
    private final CamelliaApi camelliaApi;
    private final DynamicOptionGetter dynamicOptionGetter;

    public CamelliaFeignServiceFactory(CamelliaFeignEnv feignEnv, CamelliaApi camelliaApi,
                                       CamelliaFeignProps feignProps, DynamicOptionGetter dynamicOptionGetter) {
        this.feignProps = feignProps;
        this.feignEnv = feignEnv;
        this.camelliaApi = camelliaApi;
        this.dynamicOptionGetter = dynamicOptionGetter;
    }

    public CamelliaFeignServiceFactory(CamelliaFeignEnv feignEnv, CamelliaApi camelliaApi) {
        this(feignEnv, camelliaApi, new CamelliaFeignProps(), new DefaultDynamicOptionGetter(3000L));
    }

    /**
     * 生成一个camellia-feign客户端
     * @param apiType 类型
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType) {
        return getService(-1, "default", apiType, null);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param apiType 类型
     * @param fallback fallback
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType, T fallback) {
        return getService(-1, "default", apiType, fallback);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @param apiType 类型
     * @return 客户端实例
     */
    public <T> T getService(long bid, String bgroup, Class<T> apiType) {
        return getService(bid, bgroup, apiType, null);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @param apiType 类型
     * @param fallback fallback
     * @return 客户端实例
     */
    public <T> T getService(long bid, String bgroup, Class<T> apiType, T fallback) {
        String key = bid + "|" + bgroup;
        String lockKey = key + "|" + apiType.getName();
        ConcurrentHashMap<String, Object> subMap = map.get(apiType);
        if (subMap == null) {
            synchronized (lockMap.getLockObj(lockKey)) {
                subMap = map.get(apiType);
                if (subMap == null) {
                    subMap = new ConcurrentHashMap<>();
                    map.put(apiType, subMap);
                }
            }
        }
        Object target = subMap.get(key);
        if (target == null) {
            synchronized (lockMap.getLockObj(lockKey)) {
                target = subMap.get(key);
                if (target == null) {
                    DynamicOption dynamicOption = dynamicOptionGetter.getDynamicOption(bid, bgroup);
                    CircuitBreakerConfig circuitBreakerConfig = dynamicOption.getCircuitBreakerConfig();
                    if (circuitBreakerConfig != null) {
                        circuitBreakerConfig.setName(circuitBreakerConfig.getName() + ",apiType=" + apiType.getName());
                    }
                    CamelliaFeign.Builder builder = CamelliaFeign.builder()
                            .encoder(feignProps.getEncoder())
                            .decoder(feignProps.getDecoder())
                            .errorDecoder(feignProps.getErrorDecoder())
                            .retryer(feignProps.getRetryer())
                            .logger(feignProps.getLogger())
                            .contract(feignProps.getContract())
                            .invocationHandlerFactory(feignProps.getInvocationHandlerFactory())
                            .options(feignProps.getOptions())
                            .client(new DynamicOptionClient(feignProps.getClient(), dynamicOption))
                            .requestInterceptors(feignProps.getRequestInterceptors())
                            .bid(bid)
                            .bgroup(bgroup)
                            .circuitBreakerConfig(circuitBreakerConfig)
                            .feignEnv(feignEnv)
                            .camelliaApi(camelliaApi);
                    if (feignProps.isDecode404()) {
                        builder.decode404();
                    }
                    target = builder.target(apiType, fallback);
                    subMap.put(key, target);
                    logger.info("camellia feign service = {} init success, bid = {}, bgroup = {}", apiType.getName(), bid, bgroup);
                }
            }
        }
        return (T) target;
    }

    public static class DefaultDynamicOptionGetter implements DynamicOptionGetter {
        private final long connectTimeout;
        private final TimeUnit connectTimeoutUnit;
        private final long readTimeout;
        private final TimeUnit readTimeoutUnit;
        private final boolean followRedirects;
        private final boolean circuitBreakerEnable;

        public DefaultDynamicOptionGetter(long connectTimeout, TimeUnit connectTimeoutUnit,
                                          long readTimeout, TimeUnit readTimeoutUnit, boolean followRedirects, boolean circuitBreakerEnable) {
            this.connectTimeout = connectTimeout;
            this.connectTimeoutUnit = connectTimeoutUnit;
            this.readTimeout = readTimeout;
            this.readTimeoutUnit = readTimeoutUnit;
            this.followRedirects = followRedirects;
            this.circuitBreakerEnable = circuitBreakerEnable;
        }

        public DefaultDynamicOptionGetter(long connectTimeoutMillis, long readTimeoutMillis, boolean circuitBreakerEnable) {
            this(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis, TimeUnit.MILLISECONDS, false, circuitBreakerEnable);
        }

        public DefaultDynamicOptionGetter(long connectTimeoutMillis, long readTimeoutMillis) {
            this(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis, TimeUnit.MILLISECONDS, false, false);
        }

        public DefaultDynamicOptionGetter(long timeoutMillis, boolean circuitBreakerEnable) {
            this(timeoutMillis, TimeUnit.MILLISECONDS, timeoutMillis, TimeUnit.MILLISECONDS, false, circuitBreakerEnable);
        }

        public DefaultDynamicOptionGetter(long timeoutMillis) {
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

    public static interface DynamicOptionGetter {
        DynamicOption getDynamicOption(long bid, String bgroup);
    }
}
