package com.netease.nim.camellia.feign.naked;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.RemoteMonitor;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaServerHealthChecker;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.RandomCamelliaServerSelector;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceSelector;
import com.netease.nim.camellia.feign.CamelliaFeignEnv;
import com.netease.nim.camellia.feign.CamelliaFeignFallbackFactory;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicRouteConfGetter;
import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import com.netease.nim.camellia.feign.discovery.DiscoveryResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignResourcePool;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.feign.discovery.SimpleResourcePool;
import com.netease.nim.camellia.feign.resource.FeignDiscoveryResource;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;
import com.netease.nim.camellia.feign.route.CamelliaDashboardFeignResourceTableUpdater;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientException;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientNoRetriableException;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientRetriableException;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreaker;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreakerException;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class CamelliaNakedClient<R, W> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNakedClient.class);

    public static enum OperationType {
        READ,
        WRITE,
        ;
    }

    private CamelliaFeignEnv feignEnv = CamelliaFeignEnv.defaultFeignEnv();

    private CamelliaNakedRequestInvoker<R, W> invoker;
    private int defaultMaxRetry = 0;

    private String name = "default";

    private long bid = -1;
    private String bgroup = "default";
    private CamelliaApi camelliaApi;
    private long checkIntervalMillis = 5000;
    private ResourceTable resourceTable;
    private Monitor monitor;
    private String className;
    private CamelliaFeignDynamicOptionGetter dynamicOptionGetter;
    private CamelliaFeignFallbackFactory<W> fallbackFactory;
    private CamelliaNakedClientFailureListener<R> failureListener;

    private final Map<String, FeignResourcePool> map = new ConcurrentHashMap<>();

    private ResourceSelector resourceSelector;
    private final ConcurrentHashMap<String, ResourceSelector> resourceSelectorMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaCircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaDashboardFeignResourceTableUpdater> updaterMap = new ConcurrentHashMap<>();

    private CamelliaNakedClient() {
    }

    public static class Builder {

        private long bid = -1;
        private String bgroup = "default";
        private CamelliaApi camelliaApi;
        private long checkIntervalMillis = 5000;
        private ResourceTable resourceTable;
        private String name = "default";
        private int defaultMaxRetry = 0;
        private CamelliaFeignDynamicOptionGetter dynamicOptionGetter;
        private CamelliaFeignEnv feignEnv = CamelliaFeignEnv.defaultFeignEnv();

        public Builder() {
        }

        public Builder bid(long bid) {
            this.bid = bid;
            return this;
        }

        public Builder bgroup(String bgroup) {
            this.bgroup = bgroup;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder camelliaApi(CamelliaApi camelliaApi) {
            this.camelliaApi = camelliaApi;
            return this;
        }

        public Builder feignEnv(CamelliaFeignEnv feignEnv) {
            this.feignEnv = feignEnv;
            return this;
        }

        public Builder dynamicOptionGetter(CamelliaFeignDynamicOptionGetter dynamicOptionGetter) {
            this.dynamicOptionGetter = dynamicOptionGetter;
            return this;
        }

        public Builder resourceTable(String route) {
            if (route != null) {
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(route);
                FeignResourceUtils.checkResourceTable(resourceTable);
                this.resourceTable = resourceTable;
            }
            return this;
        }

        public Builder defaultMaxRetry(int defaultMaxRetry) {
            this.defaultMaxRetry = defaultMaxRetry;
            return this;
        }

        public Builder checkIntervalMillis(long checkIntervalMillis) {
            if (checkIntervalMillis < 0) {
                throw new IllegalArgumentException("illegal checkIntervalMillis");
            }
            this.checkIntervalMillis = checkIntervalMillis;
            return this;
        }

        public <R, W> CamelliaNakedClient<R, W> build(CamelliaNakedRequestInvoker<R, W> invoker) {
            return build(invoker, null, null);
        }

        public <R, W> CamelliaNakedClient<R, W> build(CamelliaNakedRequestInvoker<R, W> invoker,
                                                      CamelliaFeignFallbackFactory<W> fallbackFactory) {
            return build(invoker, fallbackFactory, null);
        }

        public <R, W> CamelliaNakedClient<R, W> build(CamelliaNakedRequestInvoker<R, W> invoker,
                                                      CamelliaNakedClientFailureListener<R> failureListener) {
            return build(invoker, null, failureListener);
        }

        public <R, W> CamelliaNakedClient<R, W> build(CamelliaNakedRequestInvoker<R, W> invoker,
                                                      CamelliaFeignFallbackFactory<W> fallbackFactory,
                                                      CamelliaNakedClientFailureListener<R> failureListener) {
            CamelliaNakedClient<R, W> client = new CamelliaNakedClient<>();
            client.bid = bid;
            client.bgroup = bgroup;
            client.camelliaApi = camelliaApi;
            client.checkIntervalMillis = checkIntervalMillis;
            client.defaultMaxRetry = defaultMaxRetry;
            client.feignEnv = feignEnv;
            client.name = name;
            client.resourceTable = resourceTable;
            client.dynamicOptionGetter = dynamicOptionGetter;
            client.invoker = invoker;
            client.fallbackFactory = fallbackFactory;
            client.failureListener = failureListener;
            client.init();
            return client;
        }
    }

    /**
     * 别名
     */
    public String getName() {
        return name;
    }

    /**
     * 执行请求
     * @param operationType 读写类型
     * @param request 请求体
     * @return 响应
     * @throws CamelliaNakedClientException 异常
     */
    public W sendRequest(OperationType operationType, R request) throws CamelliaNakedClientException {
        return sendRequest(operationType, request, null, null, defaultMaxRetry);
    }

    /**
     * 执行请求
     * @param operationType 读写类型
     * @param request 请求体
     * @param routeKey 路由key
     * @param loadBalanceKey 负载均衡key
     * @return 响应
     * @throws CamelliaNakedClientException 异常
     */
    public W sendRequest(OperationType operationType, R request, Object routeKey, Object loadBalanceKey) throws CamelliaNakedClientException {
        return sendRequest(operationType, request, routeKey, loadBalanceKey, defaultMaxRetry);
    }

    /**
     * 发送失败重试请求
     * @param context 失败上下文
     * @return 响应
     */
    public W sendRetry(CamelliaNakedClientFailureContext<R> context) {
        return sendRetry(context, defaultMaxRetry);
    }

    /**
     * 发送失败重试请求
     * @param context 失败上下文
     * @param maxRetry 最大重试次数
     * @return 响应
     */
    public W sendRetry(CamelliaNakedClientFailureContext<R> context, int maxRetry) {
        return invoke(context.getOperationType(), context.getResource(), context.getRequest(), maxRetry, context.getLoadBalanceKey(), context.getBgroup());
    }

    /**
     * 执行请求
     * @param operationType 读写类型
     * @param request 请求体
     * @param routeKey 路由key
     * @param loadBalanceKey 负载均衡key
     * @param maxRetry 最大重试次数
     * @return 响应
     * @throws CamelliaNakedClientException 异常
     */
    public W sendRequest(OperationType operationType, R request, Object routeKey, Object loadBalanceKey, int maxRetry) throws CamelliaNakedClientException {
        try {
            int retry;
            if (maxRetry < 0) {
                retry = defaultMaxRetry;
            } else {
                retry = maxRetry;
            }
            Pair<ResourceSelector, String> pair = getResourceSelector(routeKey);
            ResourceSelector resourceSelector = pair.first;
            String bgroup = pair.second;
            if (operationType == OperationType.READ) {
                Resource resource = resourceSelector.getReadResource(ResourceSelector.EMPTY_ARRAY);
                return invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
            } else if (operationType == OperationType.WRITE) {
                List<Resource> writeResources = resourceSelector.getWriteResources(ResourceSelector.EMPTY_ARRAY);
                if (writeResources.size() == 1) {
                    Resource resource = writeResources.get(0);
                    return invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                }
                MultiWriteType multiWriteType = feignEnv.getProxyEnv().getMultiWriteType();
                if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
                    W result = null;
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(i);
                        boolean first = i == 0;
                        if (first) {
                            result = invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                        } else {
                            invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                        }
                    }
                    return result;
                } else if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
                    ThreadContextSwitchStrategy strategy = feignEnv.getProxyEnv().getThreadContextSwitchStrategy();
                    List<Future<W>> futureList = new ArrayList<>();
                    for (Resource resource : writeResources) {
                        Future<W> future;
                        try {
                            future = feignEnv.getProxyEnv().getMultiWriteConcurrentExec()
                                    .submit(strategy.wrapperCallable(() -> {
                                        try {
                                            return invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                                        } catch (Exception e) {
                                            logger.error("multi thread concurrent invoke error, bid = {}, bgroup = {}, resource = {}", bid, bgroup, resource.getUrl(), e);
                                            throw e;
                                        }
                                    }));
                        } catch (RejectedExecutionException e) {
                            try {
                                if (failureListener != null) {
                                    failureListener.onFailure(new CamelliaNakedClientFailureContext<>(bid, bgroup, operationType, request, loadBalanceKey, resource, e));
                                }
                            } catch (Exception ex) {
                                logger.error("onFailure error", ex);
                            }
                            throw e;
                        }
                        futureList.add(future);
                    }
                    W result = null;
                    for (int i=0; i<futureList.size(); i++) {
                        boolean first = i == 0;
                        if (first) {
                            result = futureList.get(i).get();
                        }
                    }
                    return result;
                } else if (multiWriteType == MultiWriteType.ASYNC_MULTI_THREAD) {
                    ThreadContextSwitchStrategy strategy = feignEnv.getProxyEnv().getThreadContextSwitchStrategy();
                    Future<W> targetFuture = null;
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(i);
                        boolean first = i == 0;
                        Future<W> future = null;
                        try {
                            future = feignEnv.getProxyEnv().getMultiWriteAsyncExec()
                                    .submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                        try {
                                            return invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                                        } catch (Exception e) {
                                            logger.error("async multi thread invoke error, bid = {}, bgroup = {}, resource = {}",
                                                    bid, bgroup, resource.getUrl(), e);
                                            throw e;
                                        }
                                    }));
                        } catch (Exception e) {
                            if (e instanceof RejectedExecutionException && failureListener != null) {
                                try {
                                    failureListener.onFailure(new CamelliaNakedClientFailureContext<>(bid, bgroup, operationType, request, loadBalanceKey, resource, e));
                                } catch (Exception ex) {
                                    logger.error("onFailure error", ex);
                                }
                            }
                            if (!first) {
                                logger.error("submit async multi thread task error, bid = {}, bgroup = {}, resource = {}",
                                        bid, bgroup, resource.getUrl(), e);
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
                    ThreadContextSwitchStrategy strategy = feignEnv.getProxyEnv().getThreadContextSwitchStrategy();
                    W target = null;
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(i);
                        boolean first = i == 0;
                        if (first) {
                            target = invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                        } else {
                            try {
                                feignEnv.getProxyEnv().getMultiWriteAsyncExec()
                                        .submit(String.valueOf(Thread.currentThread().getId()), strategy.wrapperCallable(() -> {
                                            try {
                                                return invoke(operationType, resource, request, retry, loadBalanceKey, bgroup);
                                            } catch (Exception e) {
                                                logger.error("async multi thread invoke error, bid = {}, bgroup = {}, resource = {}",
                                                        bid, bgroup, resource.getUrl(), e);
                                                throw e;
                                            }
                                        }));
                            } catch (Exception e) {
                                logger.error("submit async multi thread task error, bid = {}, bgroup = {}, resource = {}",
                                        bid, bgroup, resource.getUrl(), e);
                                if (e instanceof RejectedExecutionException && failureListener != null) {
                                    try {
                                        failureListener.onFailure(new CamelliaNakedClientFailureContext<>(bid, bgroup, operationType, request, loadBalanceKey, resource, e));
                                    } catch (Exception ex) {
                                        logger.error("onFailure error", ex);
                                    }
                                }
                            }
                        }
                    }
                    return target;
                }
            }
            throw new CamelliaNakedClientNoRetriableException("unknown operationType");
        } catch (Exception e) {
            Throwable ex = ExceptionUtils.onError(e);
            if (fallbackFactory != null) {
                W fallback = fallbackFactory.getFallback(ex);
                if (fallback != null) {
                    return fallback;
                }
            }
            if (ex instanceof CamelliaNakedClientException) {
                throw (CamelliaNakedClientException) ex;
            }
            throw new CamelliaNakedClientException(ex);
        }
    }

    private W invoke(OperationType operationType, Resource resource, R request, int maxRetry, Object loadBalanceKey, String bgroup) throws CamelliaNakedClientException {
        if (monitor != null) {
            if (operationType == OperationType.READ) {
                monitor.incrRead(resource.getUrl(), className, "read");
            } else if (operationType == OperationType.WRITE) {
                monitor.incrWrite(resource.getUrl(), className, "write");
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("camellia-naked-client, bid = {}, bgroup = {}, name = {}, method = {}, resource = {}",
                    bid, bgroup, name, operationType, resource);
        }
        if (maxRetry <= 0) maxRetry = 0;
        int retry = 0;
        boolean success = true;
        CamelliaCircuitBreaker circuitBreaker = getCircuitBreaker(resource, bgroup);
        try {
            if (circuitBreaker != null) {
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    success = false;
                    throw new CamelliaCircuitBreakerException("camellia-circuit-breaker[" + circuitBreaker.getName() + "] short-circuit, and no fallback");
                }
            }
            CamelliaNakedClientException throwException = null;
            FeignResource feignResource = null;
            FeignResourcePool resourcePool = getResourcePool(resource, bgroup);
            while (retry <= maxRetry) {
                try {
                    feignResource = resourcePool.getResource(loadBalanceKey);
                    return invoker.invoke(feignResource, request);
                } catch (CamelliaNakedClientRetriableException e) {
                    retry ++;
                    throwException = e;
                    if (feignResource != null) {
                        resourcePool.onError(feignResource);
                    }
                } catch (Exception e) {
                    if (feignResource != null) {
                        resourcePool.onError(feignResource);
                    }
                    throw e;
                }
            }
            throw throwException;
        } catch (CamelliaNakedClientException e) {
            success = feignEnv.getFallbackExceptionChecker().isSkipError(e);
            if (!success && failureListener != null) {
                try {
                    failureListener.onFailure(new CamelliaNakedClientFailureContext<>(bid, bgroup, operationType, request, loadBalanceKey, resource, e));
                } catch (Exception ex) {
                    logger.error("onFailure error", ex);
                }
            }
            throw e;
        } catch (Exception e) {
            success = feignEnv.getFallbackExceptionChecker().isSkipError(e);
            if (!success && failureListener != null) {
                try {
                    failureListener.onFailure(new CamelliaNakedClientFailureContext<>(bid, bgroup, operationType, request, loadBalanceKey, resource, e));
                } catch (Exception ex) {
                    logger.error("onFailure error", ex);
                }
            }
            throw new CamelliaNakedClientNoRetriableException(e);
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

    private CamelliaCircuitBreaker getCircuitBreaker(Resource resource, String bgroup) {
        if (dynamicOptionGetter == null) return null;
        String key = resource.getUrl() + "|" + bgroup;
        CamelliaCircuitBreaker circuitBreaker = circuitBreakerMap.get(key);
        if (circuitBreaker != null) return circuitBreaker;
        DynamicOption dynamicOption = dynamicOptionGetter.getDynamicOption(bid, bgroup);
        if (dynamicOption == null) return null;
        CircuitBreakerConfig circuitBreakerConfig = dynamicOption.getCircuitBreakerConfig();
        if (circuitBreakerConfig == null) return null;
        synchronized (circuitBreakerMap) {
            circuitBreaker = circuitBreakerMap.get(key);
            if (circuitBreaker == null) {
                circuitBreaker = new CamelliaCircuitBreaker(circuitBreakerConfig);
                circuitBreakerMap.put(key, circuitBreaker);
            }
        }
        return circuitBreaker;
    }

    private void init() {
        if (dynamicOptionGetter != null) {
            String bgroup = dynamicOptionGetter.getDefaultBgroup(bid);
            if (bgroup != null) {
                this.bgroup = bgroup;
            }
        }

        //监控类，优先使用proxyEnv中指定的Monitor
        Monitor monitor = feignEnv.getProxyEnv().getMonitor();
        if (dynamicOptionGetter != null) {
            DynamicOption dynamicOption = dynamicOptionGetter.getDynamicOption(bid, bgroup);
            if (dynamicOption != null) {
                Boolean monitorEnable = dynamicOption.isMonitorEnable();
                if (monitorEnable != null && monitorEnable) {
                    if (monitor == null && bid > 0 && camelliaApi != null) {
                        monitor = new RemoteMonitor(bid, bgroup, camelliaApi);
                    }
                } else {
                    monitor = null;
                }
            }
        }
        this.monitor = monitor;

        this.className = this.getClass().getSimpleName() + "-" + name;
    }

    private FeignResourcePool getResourcePool(Resource resource, String bgroup) {
        String key = resource.getUrl() + "|" + bgroup;
        FeignResourcePool pool = map.get(key);
        if (pool == null) {
            synchronized (map) {
                pool = map.get(key);
                if (pool == null) {
                    resource = FeignResourceUtils.parseResourceByUrl(resource);
                    if (resource instanceof FeignResource) {
                        pool = new SimpleResourcePool((FeignResource) resource);
                    } else if (resource instanceof FeignDiscoveryResource) {
                        CamelliaDiscovery<FeignServerInfo> discovery = feignEnv.getDiscoveryFactory().getDiscovery(((FeignDiscoveryResource) resource).getServiceName());
                        CamelliaServerHealthChecker<FeignServerInfo> healthChecker = feignEnv.getHealthChecker();
                        CamelliaServerSelector<FeignResource> serverSelector = new RandomCamelliaServerSelector<>();
                        if (dynamicOptionGetter != null) {
                            DynamicOption dynamicOption = dynamicOptionGetter.getDynamicOption(bid, bgroup);
                            if (dynamicOption != null) {
                                serverSelector = dynamicOption.getServerSelector();
                            }
                        }
                        pool = new DiscoveryResourcePool((FeignDiscoveryResource) resource, discovery, serverSelector, healthChecker, feignEnv.getScheduledExecutor());
                    } else {
                        throw new IllegalArgumentException("not support resource");
                    }
                    map.put(key, pool);
                }
            }
        }
        return pool;

    }

    private Pair<ResourceSelector, String> getResourceSelector(Object routeKey) {
        if (bid > 0 && routeKey != null && dynamicOptionGetter != null && camelliaApi != null) {
            DynamicRouteConfGetter dynamicRouteConfGetter = dynamicOptionGetter.getDynamicRouteConfGetter(bid);
            String bgroup = dynamicRouteConfGetter.bgroup(routeKey);
            if (bgroup == null) {
                bgroup = this.bgroup;
            }
            return new Pair<>(getResourceChooserByBgroup(bgroup), bgroup);
        }
        if (bid > 0 && camelliaApi != null) {
            return new Pair<>(getResourceChooserByBgroup(bgroup), bgroup);
        }
        if (resourceSelector == null && resourceTable != null) {
            resourceSelector = new ResourceSelector(resourceTable, feignEnv.getProxyEnv());
        }
        return new Pair<>(resourceSelector, bgroup);
    }

    private static class Pair<T, W> {
        T first;
        W second;

        public Pair(T first, W second) {
            this.first = first;
            this.second = second;
        }
    }

    private ResourceSelector getResourceChooserByBgroup(String bgroup) {
        ResourceSelector resourceSelector = resourceSelectorMap.get(bgroup);
        if (resourceSelector == null) {
            FeignResourceTableUpdater updater = initUpdater(bgroup);
            resourceSelector = resourceSelectorMap.get(bgroup);
            if (resourceSelector == null) {
                return new ResourceSelector(updater.getResourceTable(), feignEnv.getProxyEnv());
            }
        }
        return resourceSelector;
    }

    private FeignResourceTableUpdater initUpdater(String bgroup) {
        CamelliaDashboardFeignResourceTableUpdater updater = updaterMap.get(bgroup);
        if (updater == null) {
            synchronized (updaterMap) {
                updater = updaterMap.get(bgroup);
                if (updater == null) {
                    updater = new CamelliaDashboardFeignResourceTableUpdater(camelliaApi, bid, bgroup, resourceTable, checkIntervalMillis);
                    ResourceTable resourceTable = updater.getResourceTable();
                    ResourceSelector resourceSelector = new ResourceSelector(resourceTable, feignEnv.getProxyEnv());
                    updater.addCallback(table -> resourceSelectorMap.put(bgroup, new ResourceSelector(table, feignEnv.getProxyEnv())));
                    resourceSelectorMap.put(bgroup, resourceSelector);
                    updaterMap.put(bgroup, updater);
                }
            }
        }
        return updater;
    }
}
