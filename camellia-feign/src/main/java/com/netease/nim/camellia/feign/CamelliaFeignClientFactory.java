package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.model.Resource;
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
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> retryMap = new ConcurrentHashMap<>();
    private final LockMap lockMap = new LockMap();

    private final CamelliaFeignProps feignProps;
    private final CamelliaFeignEnv feignEnv;
    private final CamelliaApi camelliaApi;
    private final long checkIntervalMillis;
    private final CamelliaFeignDynamicOptionGetter dynamicOptionGetter;

    /**
     *
     * @param feignEnv 一些全局参数：包括camellia-core的一些基本参数、使用注册中心时需要的discovery实现以及对应的健康检查方法、使用fallback和熔断时过滤异常类型的FallbackExceptionChecker
     * @param camelliaApi 接入camellia-dashboard时需要本参数
     * @param checkIntervalMillis 接入camellia-dashboard时的规则检查周期，默认5000ms
     * @param feignProps 构造feign客户端时的参数，包括编解码类型、异常处理类等
     * @param dynamicOptionGetter 动态配置的回调接口
     */
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
        return getService(-1, "default", apiType, (CamelliaFeignFallbackFactory<T>) null);
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
     * @param apiType 类型
     * @param fallbackFactory fallback工厂
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType, CamelliaFeignFallbackFactory<T> fallbackFactory) {
        return getService(-1, "default", apiType, fallbackFactory);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param apiType 类型
     * @param fallbackFactory fallback工厂
     * @param failureListener 失败回调
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType, CamelliaFeignFallbackFactory<T> fallbackFactory, CamelliaFeignFailureListener failureListener) {
        return getService(-1, "default", apiType, fallbackFactory, failureListener);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @param apiType 类型
     * @return 客户端实例
     */
    public <T> T getService(long bid, String bgroup, Class<T> apiType) {
        return getService(bid, bgroup, apiType, (CamelliaFeignFallbackFactory<T>) null);
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
        return getService(bid, bgroup, apiType, new CamelliaFeignFallbackFactory.Default<>(fallback));
    }

    /**
     * 生成一个camellia-feign客户端
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @param apiType 类型
     * @param fallbackFactory fallback工厂
     * @return 客户端实例
     */
    public <T> T getService(long bid, String bgroup, Class<T> apiType, CamelliaFeignFallbackFactory<T> fallbackFactory) {
        return getService(bid, bgroup, apiType, fallbackFactory, null);
    }

    /**
     * 生成一个camellia-feign客户端
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @param apiType 类型
     * @param fallbackFactory fallback工厂
     * @param failureListener 失败回调
     * @return 客户端实例
     */
    public <T> T getService(long bid, String bgroup, Class<T> apiType, CamelliaFeignFallbackFactory<T> fallbackFactory, CamelliaFeignFailureListener failureListener) {
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
                            .failureListener(failureListener)
                            .checkIntervalMillis(checkIntervalMillis);
                    if (feignProps.isDecode404()) {
                        builder.decode404();
                    }
                    target = builder.target(apiType, fallbackFactory);
                    subMap.put(key, target);
                    logger.info("camellia feign service = {} init success, bid = {}, bgroup = {}", apiType.getName(), bid, bgroup);
                }
            }
        }
        return (T) target;
    }

    /**
     * 生成一个重试的service
     * @param failureContext failureContext
     * @return 实例
     */
    public <T> T getRetryService(CamelliaFeignFailureContext failureContext) {
        return getRetryService(failureContext, null, null);
    }

    /**
     * 生成一个重试的service
     * @param failureContext failureContext
     * @param fallbackFactory fallback工厂
     * @param failureListener 失败回调
     * @return 实例
     */
    public <T> T getRetryService(CamelliaFeignFailureContext failureContext, CamelliaFeignFallbackFactory<T> fallbackFactory, CamelliaFeignFailureListener failureListener) {
        Class<T> apiType = (Class<T>) failureContext.getApiType();
        Resource resource = failureContext.getResource();
        String lockKey = apiType.getName() + "|" + resource.getUrl();
        ConcurrentHashMap<String, Object> subMap = retryMap.get(apiType);
        if (subMap == null) {
            synchronized (lockMap.getLockObj(lockKey)) {
                subMap = retryMap.get(apiType);
                if (subMap == null) {
                    subMap = new ConcurrentHashMap<>();
                    retryMap.put(apiType, subMap);
                }
            }
        }
        Object target = subMap.get(resource.getUrl());
        if (target == null) {
            synchronized (lockMap.getLockObj(lockKey)) {
                target = subMap.get(resource.getUrl());
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
                            .resourceTable(resource.getUrl())
                            .dynamicOptionGetter(dynamicOptionGetter)
                            .bid(failureContext.getBid())
                            .bgroup(failureContext.getBgroup())
                            .camelliaApi(camelliaApi)
                            .checkIntervalMillis(checkIntervalMillis)
                            .failureListener(failureListener)
                            .feignEnv(feignEnv);
                    if (feignProps.isDecode404()) {
                        builder.decode404();
                    }
                    target = builder.target(apiType, fallbackFactory);
                    subMap.put(resource.getUrl(), target);
                    logger.info("camellia feign retry service = {} init success, resource = {}", apiType.getName(), resource.getUrl());
                }
            }
        }
        return (T) target;
    }
}
