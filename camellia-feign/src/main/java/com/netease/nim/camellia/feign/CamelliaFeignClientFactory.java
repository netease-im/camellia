package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.util.LockMap;
import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理camellia-feign客户端实例的工厂类
 * Created by caojiajun on 2022/3/24
 */
public class CamelliaFeignClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaFeignClientFactory.class);

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> map = new ConcurrentHashMap<>();
    private final LockMap lockMap = new LockMap();

    private final CamelliaFeignProps feignProps;
    private final CamelliaFeignEnv feignEnv;
    private final CamelliaApi camelliaApi;
    private final long checkIntervalMillis;
    private final CamelliaFeignDynamicOptionGetter dynamicOptionGetter;

    public CamelliaFeignClientFactory(CamelliaFeignEnv feignEnv, CamelliaApi camelliaApi, long checkIntervalMillis,
                                      CamelliaFeignProps feignProps, CamelliaFeignDynamicOptionGetter dynamicOptionGetter) {
        this.feignProps = feignProps;
        this.feignEnv = feignEnv;
        this.camelliaApi = camelliaApi;
        this.checkIntervalMillis = checkIntervalMillis;
        this.dynamicOptionGetter = dynamicOptionGetter;
    }

    public CamelliaFeignClientFactory(CamelliaFeignEnv feignEnv, CamelliaApi camelliaApi) {
        this(feignEnv, camelliaApi, 5000L, new CamelliaFeignProps(), new CamelliaFeignDynamicOptionGetter.DefaultCamelliaFeignDynamicOptionGetter(3000L));
    }

    public CamelliaFeignClientFactory() {
        this(CamelliaFeignEnv.defaultFeignEnv(), null);
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
                    CamelliaFeign.Builder builder = CamelliaFeign.builder()
                            .encoder(feignProps.getEncoder())
                            .decoder(feignProps.getDecoder())
                            .errorDecoder(feignProps.getErrorDecoder())
                            .retryer(feignProps.getRetryer())
                            .logger(feignProps.getLogger())
                            .contract(feignProps.getContract())
                            .invocationHandlerFactory(feignProps.getInvocationHandlerFactory())
                            .options(feignProps.getOptions())
                            .client(feignProps.getClient())
                            .requestInterceptors(feignProps.getRequestInterceptors())
                            .bid(bid)
                            .bgroup(bgroup)
                            .feignEnv(feignEnv)
                            .dynamicOptionGetter(dynamicOptionGetter)
                            .camelliaApi(camelliaApi)
                            .checkIntervalMillis(checkIntervalMillis);
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
}
