package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.annotation.LoadBalanceKey;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaServerHealthChecker;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.RandomCamelliaServerSelector;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.discovery.DiscoveryResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.feign.discovery.SimpleResourcePool;
import com.netease.nim.camellia.feign.exception.CamelliaFeignException;
import com.netease.nim.camellia.feign.exception.CamelliaFeignFallbackErrorException;
import com.netease.nim.camellia.feign.resource.FeignDiscoveryResource;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreaker;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreakerException;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignCallback<T> implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignCallback.class);

    private final long bid;
    private final String bgroup;
    private final Map<String, FeignResourcePool> map = new ConcurrentHashMap<>();
    private final Map<String, CamelliaCircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    private ResourceSelector resourceSelector;
    private final CamelliaFeignEnv feignEnv;
    private final FeignClientFactory<T> factory;
    private final Class<T> apiType;
    private final CamelliaFeignFallbackFactory<T> fallbackFactory;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final CamelliaServerSelector<FeignResource> serverSelector;
    private final ReadWriteOperationCache readWriteOperationCache = new ReadWriteOperationCache();
    private final AnnotationValueGetterCache annotationValueGetterCache = new AnnotationValueGetterCache();
    private final DynamicOption dynamicOption;
    private final String className;
    private final Monitor monitor;
    private final CamelliaFeignFailureListener failureListener;
    private final int retry;
    private final RetryPolicy retryPolicy;

    public FeignCallback(CamelliaFeignBuildParam<T> buildParam) {
        this.bid = buildParam.getBid();
        this.bgroup = buildParam.getBgroup();
        this.apiType = buildParam.getApiType();
        this.className = apiType.getName();
        this.failureListener = buildParam.getFailureListener();
        this.monitor = buildParam.getMonitor();
        this.feignEnv = buildParam.getFeignEnv();
        this.dynamicOption = buildParam.getDynamicOption();
        this.factory = new FeignClientFactory.Default<>(apiType, buildParam.getFeignProps(), buildParam.getDynamicOption());
        this.fallbackFactory = buildParam.getFallbackFactory();
        this.circuitBreakerConfig = buildParam.getDynamicOption() == null ? null : buildParam.getDynamicOption().getCircuitBreakerConfig();
        if (circuitBreakerConfig != null) {
            String name = circuitBreakerConfig.getName();
            if (name == null) {
                name = "apiType=" + apiType.getSimpleName();
            } else {
                name = name + ",apiType=" + apiType.getSimpleName();
            }
            circuitBreakerConfig.setName(name);
        }
        this.retry = buildParam.getRetry();
        this.retryPolicy = buildParam.getRetryPolicy();
        this.serverSelector = buildParam.getDynamicOption() == null ? new RandomCamelliaServerSelector<>() : buildParam.getDynamicOption().getServerSelector();
        annotationValueGetterCache.preheatAnnotationValueByParameterField(apiType, LoadBalanceKey.class);
        readWriteOperationCache.preheat(apiType);
        FeignResourceTableUpdater updater = buildParam.getUpdater();
        refresh(updater.getResourceTable(), true);
        updater.addCallback(resourceTable -> refresh(resourceTable, false));
    }

    private void refresh(ResourceTable resourceTable, boolean throwError) {
        try {
            Set<Resource> resources = ResourceUtil.getAllResources(resourceTable);
            for (Resource r : resources) {
                FeignResourcePool pool = map.get(r.getUrl());
                if (pool != null) continue;
                Resource resource = FeignResourceUtils.parseResourceByUrl(r);
                if (resource instanceof FeignResource) {
                    pool = new SimpleResourcePool((FeignResource) resource);
                    GlobalCamelliaFeignEnv.register(resource, serverSelector, Collections.singletonList(pool.getResource(null)));
                } else if (resource instanceof FeignDiscoveryResource) {
                    CamelliaDiscovery<FeignServerInfo> discovery = feignEnv.getDiscoveryFactory().getDiscovery(((FeignDiscoveryResource) resource).getServiceName());
                    CamelliaServerHealthChecker<FeignServerInfo> healthChecker = feignEnv.getHealthChecker();
                    pool = new DiscoveryResourcePool((FeignDiscoveryResource) resource, discovery, serverSelector, healthChecker, feignEnv.getScheduledExecutor());
                } else {
                    throw new IllegalArgumentException("not support resource");
                }
                map.put(resource.getUrl(), pool);
            }
            if (circuitBreakerConfig != null) {
                for (Resource r : resources) {
                    CamelliaCircuitBreaker circuitBreaker = circuitBreakerMap.get(r.getUrl());
                    if (circuitBreaker != null) continue;
                    CircuitBreakerConfig duplicate = circuitBreakerConfig.duplicate();
                    duplicate.setName(duplicate.getName() + ",resource=" + r.getUrl());
                    circuitBreaker = new CamelliaCircuitBreaker(duplicate);
                    circuitBreakerMap.put(r.getUrl(), circuitBreaker);
                }
            }
            this.resourceSelector = new ResourceSelector(resourceTable, feignEnv.getProxyEnv());
            GlobalCamelliaFeignEnv.register(bid, bgroup, apiType, resourceSelector);
        } catch (Exception e) {
            logger.error("refresh error, class = {}", apiType.getName(), e);
            if (throwError) {
                throw e;
            }
        }
    }

    private CamelliaCircuitBreaker getCircuitBreaker(Resource resource) {
        if (circuitBreakerConfig == null) return null;
        return circuitBreakerMap.get(resource.getUrl());
    }

    private Object invoke(Resource resource, Object loadBalanceKey, Method method, Object[] objects, boolean checkFallback, byte operationType) throws Throwable {
        if (monitor != null) {
            if (dynamicOption == null || dynamicOption.isMonitorEnable()) {
                if (operationType == ReadWriteOperationCache.READ || operationType == ReadWriteOperationCache.UNKNOWN) {
                    monitor.incrRead(resource.getUrl(), className, readWriteOperationCache.getMethodName(method));
                } else if (operationType == ReadWriteOperationCache.WRITE) {
                    monitor.incrWrite(resource.getUrl(), className, readWriteOperationCache.getMethodName(method));
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("camellia-feign, service = {}, method = {}, resource = {}", className, readWriteOperationCache.getMethodName(method), resource.getUrl());
        }
        FeignResourcePool pool = map.get(resource.getUrl());
        FeignResource feignResource = pool.getResource(loadBalanceKey);
        T client = factory.get(feignResource);
        CamelliaCircuitBreaker circuitBreaker = getCircuitBreaker(resource);
        boolean success = true;
        try {
            if (circuitBreaker != null) {
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    success = false;
                    if (checkFallback && fallbackFactory != null) {
                        try {
                            T fallback = fallbackFactory.getFallback(CamelliaCircuitBreakerException.DEFAULT);
                            if (fallback != null) {
                                return method.invoke(fallback, objects);
                            }
                        } catch (Exception ex) {
                            Throwable error = ExceptionUtils.onError(ex);
                            boolean skip = feignEnv.getFallbackExceptionChecker().isSkipError(error);
                            if (skip) {
                                throw error;
                            }
                            throw new CamelliaFeignFallbackErrorException(ExceptionUtils.onError(ex));
                        }
                    }
                    throw new CamelliaCircuitBreakerException("camellia-circuit-breaker[" + circuitBreaker.getName() + "] short-circuit, and no fallback");
                }
            }
            if (retry <= 0 || retryPolicy == null) {
                return method.invoke(client, objects);
            }
            Throwable throwable = null;
            for (int i=0; i<retry; i++) {
                try {
                    return method.invoke(client, objects);
                } catch (Throwable e) {
                    Throwable error = ExceptionUtils.onError(e);
                    boolean skip = feignEnv.getFallbackExceptionChecker().isSkipError(error);
                    if (skip) {
                        throw e;
                    }
                    RetryPolicy.RetryInfo retryInfo = retryPolicy.retryError(error);
                    if (retryInfo == null || !retryInfo.isRetry()) {
                        throw e;
                    }
                    if (retryInfo.isNextServer()) {
                        feignResource = pool.getResource(loadBalanceKey);
                        client = factory.get(feignResource);
                        continue;
                    }
                    throwable = e;
                }
            }
            if (throwable != null) {
                throw throwable;
            }
            throw new CamelliaFeignException("exceed retry");
        } catch (Throwable e) {
            Throwable error = ExceptionUtils.onError(e);
            success = feignEnv.getFallbackExceptionChecker().isSkipError(error);
            if (!success && !(error instanceof CamelliaCircuitBreakerException)) {
                pool.onError(feignResource);
            }
            if (!success && failureListener != null) {
                try {
                    CamelliaFeignFailureContext failureContext = new CamelliaFeignFailureContext(bid, bgroup, apiType, operationType,
                            resource, loadBalanceKey, readWriteOperationCache.getGenericString(method), objects, error);
                    failureListener.onFailure(failureContext);
                } catch (Exception ex) {
                    logger.error("onFailure error", ex);
                }
            }
            throw error;
        } finally {
            if (circuitBreaker != null) {
                if (success) {
                    circuitBreaker.incrementSuccess();
                } else {
                    circuitBreaker.incrementFail();
                }
            }
        }
    }

    @Override
    public Object intercept(Object o, final Method method, final Object[] objects, MethodProxy methodProxy) throws Throwable {
        try {
            byte operationType = readWriteOperationCache.getOperationType(method);
            final Object loadBalanceKey = annotationValueGetterCache.getAnnotationValueByParameterField(LoadBalanceKey.class, method, objects);
            if (operationType == ReadWriteOperationCache.READ || operationType == ReadWriteOperationCache.UNKNOWN) {
                Resource resource = resourceSelector.getReadResource(ResourceSelector.EMPTY_ARRAY);
                return invoke(resource, loadBalanceKey, method, objects, true, operationType);
            } else if (operationType == ReadWriteOperationCache.WRITE) {
                List<Resource> list = resourceSelector.getWriteResources(ResourceSelector.EMPTY_ARRAY);
                if (list.size() == 1) {
                    Resource resource = list.get(0);
                    return invoke(resource, loadBalanceKey, method, objects, true, operationType);
                } else {
                    ProxyEnv env = feignEnv.getProxyEnv();
                    MultiWriteType multiWriteType = env.getMultiWriteType();
                    if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
                        ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                        List<Future<Object>> futureList = new ArrayList<>();
                        for (int i=0; i<list.size(); i++) {
                            Resource resource = list.get(i);
                            boolean first = i == 0;
                            Future<Object> future;
                            try {
                                future = env.getMultiWriteConcurrentExec().submit(strategy.wrapperCallable(() -> {
                                    try {
                                        return invoke(resource, loadBalanceKey, method, objects, first, operationType);
                                    } catch (Throwable e) {
                                        logger.error("multi thread concurrent invoke error, class = {}, method = {}, resource = {}",
                                                className, method.getName(), resource.getUrl(), e);
                                        throw new ExecutionException(e);
                                    }
                                }));
                            } catch (RejectedExecutionException e) {
                                try {
                                    if (failureListener != null) {
                                        CamelliaFeignFailureContext failureContext = new CamelliaFeignFailureContext(bid, bgroup, apiType, operationType,
                                                resource, loadBalanceKey, readWriteOperationCache.getGenericString(method), objects, e);
                                        failureListener.onFailure(failureContext);
                                    }
                                } catch (Exception ex) {
                                    logger.error("onFailure error", ex);
                                }
                                throw e;
                            }
                            futureList.add(future);
                        }
                        Object ret = null;
                        for (int i=0; i<futureList.size(); i++) {
                            boolean first = i == 0;
                            Future<Object> future = futureList.get(i);
                            Object ret1 = future.get();
                            if (first) {
                                ret = ret1;
                            }
                        }
                        return ret;
                    } else if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
                        Object ret = null;
                        for (int i=0; i<list.size(); i++) {
                            boolean first = i == 0;
                            Resource resource = list.get(i);
                            Object ret1 = invoke(resource, loadBalanceKey, method, objects, first, operationType);
                            if (first) {
                                ret = ret1;
                            }
                        }
                        return ret;
                    } else if (multiWriteType == MultiWriteType.ASYNC_MULTI_THREAD) {
                        ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                        Future<Object> targetFuture = null;
                        for (int i=0; i<list.size(); i++) {
                            Resource resource = list.get(i);
                            boolean first = i == 0;
                            Future<Object> future = null;
                            try {
                                future = env.getMultiWriteAsyncExec().submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                    try {
                                        return invoke(resource, loadBalanceKey, method, objects, first, operationType);
                                    } catch (Throwable e) {
                                        logger.error("async multi thread invoke error, class = {}, method = {}, resource = {}",
                                                className, method.getName(), resource.getUrl(), e);
                                        throw new ExecutionException(e);
                                    }
                                }));
                            } catch (Exception e) {
                                if (e instanceof RejectedExecutionException && failureListener != null) {
                                    try {
                                        CamelliaFeignFailureContext failureContext = new CamelliaFeignFailureContext(bid, bgroup, apiType, operationType,
                                                resource, loadBalanceKey, readWriteOperationCache.getGenericString(method), objects, e);
                                        failureListener.onFailure(failureContext);
                                    } catch (Exception ex) {
                                        logger.error("onFailure error", ex);
                                    }
                                }
                                if (!first) {
                                    logger.error("submit async multi thread task error, class = {}, method = {}, resource = {}",
                                            className, method.getName(), resource.getUrl(), e);
                                } else {
                                    throw e;
                                }
                            }
                            if (first) {
                                targetFuture = future;
                            }
                        }
                        if (targetFuture != null) {
                            return targetFuture.get();
                        } else {
                            throw new IllegalStateException("wil not invoke here");
                        }
                    } else if (multiWriteType == MultiWriteType.MISC_ASYNC_MULTI_THREAD) {
                        ThreadContextSwitchStrategy strategy = env.getThreadContextSwitchStrategy();
                        Object target = null;
                        for (int i=0; i<list.size(); i++) {
                            Resource resource = list.get(i);
                            boolean first = i == 0;
                            if (first) {
                                target = invoke(resource, loadBalanceKey, method, objects, true, operationType);
                            } else {
                                try {
                                    env.getMultiWriteAsyncExec().submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                        try {
                                            return invoke(resource, loadBalanceKey, method, objects, false, operationType);
                                        } catch (Throwable e) {
                                            logger.error("async multi thread invoke error, class = {}, method = {}, resource = {}",
                                                    className, method.getName(), resource.getUrl(), e);
                                            throw new ExecutionException(e);
                                        }
                                    }));
                                } catch (Exception e) {
                                    logger.error("submit async multi thread task error, class = {}, method = {}, resource = {}",
                                            className, method.getName(), resource.getUrl(), e);
                                    if (e instanceof RejectedExecutionException && failureListener != null) {
                                        try {
                                            CamelliaFeignFailureContext failureContext = new CamelliaFeignFailureContext(bid, bgroup, apiType, operationType,
                                                    resource, loadBalanceKey, readWriteOperationCache.getGenericString(method), objects, e);
                                            failureListener.onFailure(failureContext);
                                        } catch (Exception ex) {
                                            logger.error("onFailure error", ex);
                                        }
                                    }
                                }
                            }
                        }
                        return target;
                    } else {
                        throw new IllegalArgumentException("unknown multiWriteType");
                    }
                }
            }
            throw new IllegalStateException("wil not invoke here");
        } catch (Exception e) {
            Throwable t = ExceptionUtils.onError(e);
            if (fallbackFactory != null && !(e instanceof CamelliaFeignFallbackErrorException) && !feignEnv.getFallbackExceptionChecker().isSkipError(e)) {
                try {
                    T fallback = fallbackFactory.getFallback(t);
                    if (fallback != null) {
                        return method.invoke(fallback, objects);
                    }
                } catch (Exception ex) {
                    throw ExceptionUtils.onError(ex);
                }
            }
            if (t instanceof CamelliaFeignFallbackErrorException) {
                throw t.getCause();
            }
            throw t;
        }
    }



}
