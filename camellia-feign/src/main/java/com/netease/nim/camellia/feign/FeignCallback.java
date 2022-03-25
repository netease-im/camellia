package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.annotation.Key;
import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaServerHealthChecker;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ExceptionUtils;
import com.netease.nim.camellia.core.util.ResourceChooser;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.feign.discovery.DiscoveryResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.feign.discovery.SimpleResourcePool;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignCallback<T> implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignCallback.class);

    private final Map<String, FeignResourcePool> map = new HashMap<>();
    private ResourceChooser resourceChooser;
    private final CamelliaFeignEnv feignEnv;
    private final FeignClientFactory<T> factory;
    private final Class<T> clazz;
    private final T fallback;
    private CamelliaCircuitBreaker circuitBreaker;

    public FeignCallback(Class<T> clazz, T fallback, FeignResourceTableUpdater updater,
                         FeignClientFactory<T> factory, CamelliaFeignEnv feignEnv, CircuitBreakerConfig circuitBreakerConfig) {
        this.clazz = clazz;
        this.feignEnv = feignEnv;
        this.factory = factory;
        this.fallback = fallback;
        for (Method method : clazz.getMethods()) {
            operationType(method);
            keyParamIndex(method);
        }
        refresh(updater.getResourceTable());
        updater.addCallback(this::refresh);
        if (circuitBreakerConfig != null) {
            circuitBreaker = new CamelliaCircuitBreaker(circuitBreakerConfig);
        }
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
                    CamelliaServerSelector<FeignResource> serverSelector = feignEnv.getServerSelector();
                    pool = new DiscoveryResourcePool((FeignDiscoveryResource) resource, discovery, serverSelector, healthChecker);
                } else {
                    throw new IllegalArgumentException("not support resource");
                }
                map.put(resource.getUrl(), pool);
            }
            this.resourceChooser = new ResourceChooser(resourceTable, feignEnv.getProxyEnv());
        } catch (Exception e) {
            logger.error("refresh error, class = {}", clazz.getName(), e);
        }
    }

    private Object invoke(Resource resource, Object key, Method method, Object[] objects) throws Exception {
        FeignResourcePool pool = map.get(resource.getUrl());
        FeignResource feignResource = pool.getResource(key);
        T t = factory.get(feignResource);
        try {
            return method.invoke(t, objects);
        } catch (Exception e) {
            pool.onError(feignResource);
            throw e;
        }
    }

    @Override
    public Object intercept(Object o, final Method method, final Object[] objects, MethodProxy methodProxy) throws Throwable {
        boolean success = true;
        try {
            if (circuitBreaker != null) {
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    success = false;
                    if (fallback != null) {
                        try {
                            return method.invoke(fallback, objects);
                        } catch (Exception ex) {
                            throw ExceptionUtils.onError(ex);
                        }
                    }
                    throw new CamelliaCircuitBreakerException("camellia-circuit-breaker[" + circuitBreaker.getName() + "] short-circuit, and no fallback");
                }
            }
            byte operationType = operationType(method);
            Integer index = keyParamIndex(method);
            final Object key;
            if (index != null && index >= 0) {
                key = objects[index];
            } else {
                key = null;
            }
            if (operationType == READ || operationType == UNKNOWN) {
                Resource resource = resourceChooser.getReadResource(EMPTY);
                return invoke(resource, key, method, objects);
            } else if (operationType == WRITE) {
                List<Resource> list = resourceChooser.getWriteResources(EMPTY);
                if (list.size() == 1) {
                    Resource resource = list.get(0);
                    return invoke(resource, key, method, objects);
                } else {
                    ProxyEnv env = feignEnv.getProxyEnv();
                    if (env.isMultiWriteConcurrentEnable()) {
                        List<Future<Object>> futureList = new ArrayList<>();
                        for (final Resource resource : list) {
                            Future<Object> future = env.getMultiWriteConcurrentExec().submit(() -> invoke(resource, key, method, objects));
                            futureList.add(future);
                        }
                        Object ret = null;
                        boolean isRetSet = false;
                        for (Future<Object> future : futureList) {
                            Object ret1 = future.get();
                            if (!isRetSet) {
                                ret = ret1;
                                isRetSet = true;
                            }
                        }
                        return ret;
                    } else {
                        Object ret = null;
                        boolean isRetSet = false;
                        for (Resource resource : list) {
                            Object ret1 = invoke(resource, key, method, objects);
                            if (!isRetSet) {
                                ret = ret1;
                                isRetSet = true;
                            }
                        }
                        return ret;
                    }
                }
            }
            throw new IllegalStateException("wil not invoke here");
        } catch (Exception e) {
            success = false;
            if (fallback != null) {
                try {
                    return method.invoke(fallback, objects);
                } catch (Exception ex) {
                    throw ExceptionUtils.onError(ex);
                }
            }
            throw ExceptionUtils.onError(e);
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


    private byte operationType(Method method) {
        if (method == null) return UNKNOWN;
        Byte cache = annotationCache.get(method);
        if (cache != null) return cache;
        WriteOp writeOp = method.getAnnotation(WriteOp.class);
        if (writeOp != null) {
            annotationCache.put(method, WRITE);
            return WRITE;
        }
        ReadOp readOp = method.getAnnotation(ReadOp.class);
        if (readOp != null) {
            annotationCache.put(method, READ);
            return READ;
        }
        annotationCache.put(method, UNKNOWN);
        return UNKNOWN;
    }

    private Integer keyParamIndex(Method method) {
        if (method == null) return null;
        Integer index = keyParamIndexCache.get(method);
        if (index != null) return index;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i=0; i<parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (Key.class.isAssignableFrom(annotation.annotationType())) {
                    keyParamIndexCache.put(method, i);
                    return i;
                }
            }
        }
        keyParamIndexCache.put(method, -1);
        return -1;
    }

    private static final byte WRITE = 1;
    private static final byte READ = 2;
    private static final byte UNKNOWN = 3;
    private final Map<Method, Byte> annotationCache = new HashMap<>();
    private static final byte[] EMPTY = new byte[0];
    private final Map<Method, Integer> keyParamIndexCache = new HashMap<>();
}
