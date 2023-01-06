package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import com.netease.nim.camellia.feign.exception.CamelliaFeignException;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/7/5
 */
public class CamelliaFeignRetryClient<T> {

    private final CamelliaFeignClientFactory factory;
    private final Class<T> apiType;

    private final ConcurrentHashMap<String, Method> methodMap = new ConcurrentHashMap<>();

    public CamelliaFeignRetryClient(Class<T> apiType, CamelliaFeignClientFactory factory) {
        this.apiType = apiType;
        this.factory = factory;
        for (Method method : apiType.getMethods()) {
            methodMap.put(method.toGenericString(), method);
        }
    }

    /**
     * 发送一个失败重试请求
     * @param failureContext 上下文
     * @return 响应
     * @throws CamelliaFeignException 异常
     */
    public Object sendRetry(CamelliaFeignFailureContext failureContext) throws CamelliaFeignException {
        return sendRetry(failureContext, null, null);
    }

    /**
     * 发送一个失败重试请求
     * @param failureContext 上下文
     * @param fallbackFactory fallback工厂
     * @param failureListener 失败回调
     * @return 响应
     * @throws CamelliaFeignException 异常
     */
    public Object sendRetry(CamelliaFeignFailureContext failureContext,
                            CamelliaFeignFallbackFactory<T> fallbackFactory,
                            CamelliaFeignFailureListener failureListener) throws CamelliaFeignException {
        if (!failureContext.getApiType().equals(apiType)) {
            throw new IllegalArgumentException("apiType not match");
        }
        T retryService = factory.getRetryService(failureContext, fallbackFactory, failureListener);
        Method method = methodMap.get(failureContext.getMethod());
        if (method == null) {
            throw new IllegalArgumentException("unknown method = " + failureContext.getMethod());
        }
        try {
            return method.invoke(retryService, failureContext.getObjects());
        } catch (Exception e) {
            Throwable error = ExceptionUtils.onError(e);
            throw new CamelliaFeignException(error);
        }
    }
}
