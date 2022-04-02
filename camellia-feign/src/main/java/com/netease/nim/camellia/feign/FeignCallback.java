package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.annotation.LoadBalanceKey;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
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
import com.netease.nim.camellia.feign.exception.CamelliaFeignFallbackErrorException;
import com.netease.nim.camellia.feign.resource.FeignDiscoveryResource;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreaker;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreakerException;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignCallback<T> implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignCallback.class);

    private final Map<String, FeignResourcePool> map = new HashMap<>();
    private final Map<String, CamelliaCircuitBreaker> circuitBreakerMap = new HashMap<>();
    private ResourceChooser resourceChooser;
    private final CamelliaFeignEnv feignEnv;
    private final FeignClientFactory<T> factory;
    private final Class<T> clazz;
    private final T fallback;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final CamelliaServerSelector<FeignResource> serverSelector;
    private final ReadWriteOperationCache readWriteOperationCache = new ReadWriteOperationCache();
    private final AnnotationValueGetterCache annotationValueGetterCache = new AnnotationValueGetterCache();
    private final DynamicOption dynamicOption;
    private final String className;
    private final Monitor monitor;

    public FeignCallback(CamelliaFeignBuildParam<T> buildParam) {
        this.clazz = buildParam.getApiType();
        this.className = clazz.getName();
        this.monitor = buildParam.getMonitor();
        this.feignEnv = buildParam.getFeignEnv();
        this.dynamicOption = buildParam.getDynamicOption();
        this.factory = new FeignClientFactory.Default<>(clazz, buildParam.getFeignProps(), buildParam.getDynamicOption());
        this.fallback = buildParam.getFallback();
        this.circuitBreakerConfig = buildParam.getDynamicOption() == null ? null : buildParam.getDynamicOption().getCircuitBreakerConfig();
        if (circuitBreakerConfig != null) {
            String name = circuitBreakerConfig.getName();
            if (name == null) {
                name = "apiType=" + clazz.getSimpleName();
            } else {
                name = name + ",apiType=" + clazz.getSimpleName();
            }
            circuitBreakerConfig.setName(name);
        }
        this.serverSelector = buildParam.getDynamicOption() == null ? new RandomCamelliaServerSelector<>() : buildParam.getDynamicOption().getServerSelector();
        annotationValueGetterCache.preheatAnnotationValueByParameterField(clazz, LoadBalanceKey.class);
        readWriteOperationCache.preheat(clazz);
        FeignResourceTableUpdater updater = buildParam.getUpdater();
        refresh(updater.getResourceTable());
        updater.addCallback(this::refresh);
    }

    private void refresh(ResourceTable resourceTable) {
        try {
            Set<Resource> resources = ResourceUtil.getAllResources(resourceTable);
            for (Resource r : resources) {
                FeignResourcePool pool = map.get(r.getUrl());
                if (pool != null) continue;
                Resource resource = FeignResourceUtils.parseResourceByUrl(r);
                if (resource instanceof FeignResource) {
                    pool = new SimpleResourcePool((FeignResource) resource);
                } else if (resource instanceof FeignDiscoveryResource) {
                    CamelliaDiscovery<FeignServerInfo> discovery = feignEnv.getDiscoveryFactory().getDiscovery(((FeignDiscoveryResource) resource).getServiceName());
                    CamelliaServerHealthChecker<FeignServerInfo> healthChecker = feignEnv.getHealthChecker();
                    pool = new DiscoveryResourcePool((FeignDiscoveryResource) resource, discovery, serverSelector, healthChecker);
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
            this.resourceChooser = new ResourceChooser(resourceTable, feignEnv.getProxyEnv());
        } catch (Exception e) {
            logger.error("refresh error, class = {}", clazz.getName(), e);
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
        FeignResourcePool pool = map.get(resource.getUrl());
        FeignResource feignResource = pool.getResource(loadBalanceKey);
        T t = factory.get(feignResource);
        CamelliaCircuitBreaker circuitBreaker = getCircuitBreaker(resource);
        boolean success = true;
        try {
            if (circuitBreaker != null) {
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    success = false;
                    if (checkFallback && fallback != null) {
                        try {
                            return method.invoke(fallback, objects);
                        } catch (Exception ex) {
                            throw new CamelliaFeignFallbackErrorException(ExceptionUtils.onError(ex));
                        }
                    }
                    throw new CamelliaCircuitBreakerException("camellia-circuit-breaker[" + circuitBreaker.getName() + "] short-circuit, and no fallback");
                }
            }
            return method.invoke(t, objects);
        } catch (Throwable e) {
            success = feignEnv.getFallbackExceptionChecker().isSkipError(e);
            if (!(e instanceof CamelliaCircuitBreakerException)) {
                pool.onError(feignResource);
            }
            throw e;
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
                Resource resource = resourceChooser.getReadResource(ResourceChooser.EMPTY_ARRAY);
                return invoke(resource, loadBalanceKey, method, objects, true, operationType);
            } else if (operationType == ReadWriteOperationCache.WRITE) {
                List<Resource> list = resourceChooser.getWriteResources(ResourceChooser.EMPTY_ARRAY);
                if (list.size() == 1) {
                    Resource resource = list.get(0);
                    return invoke(resource, loadBalanceKey, method, objects, true, operationType);
                } else {
                    ProxyEnv env = feignEnv.getProxyEnv();
                    if (env.isMultiWriteConcurrentEnable()) {
                        List<Future<Object>> futureList = new ArrayList<>();
                        for (int i=0; i<list.size(); i++) {
                            Resource resource = list.get(i);
                            boolean first = i == 0;
                            Future<Object> future = env.getMultiWriteConcurrentExec().submit(() -> {
                                try {
                                    return invoke(resource, loadBalanceKey, method, objects, first, operationType);
                                } catch (Throwable e) {
                                    throw new ExecutionException(e);
                                }
                            });
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
                    } else {
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
                    }
                }
            }
            throw new IllegalStateException("wil not invoke here");
        } catch (Exception e) {
            Throwable t = ExceptionUtils.onError(e);
            if (fallback != null && !(e instanceof CamelliaFeignFallbackErrorException) && !feignEnv.getFallbackExceptionChecker().isSkipError(t)) {
                try {
                    return method.invoke(fallback, objects);
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
