package com.netease.nim.camellia.feign.naked;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ThreadContextSwitchStrategy;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaServerHealthChecker;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.RandomCamelliaServerSelector;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ExceptionUtils;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceChooser;
import com.netease.nim.camellia.feign.CamelliaFeignEnv;
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
import com.netease.nim.camellia.feign.naked.exception.ClientException;
import com.netease.nim.camellia.feign.naked.exception.ClientNoRetriableException;
import com.netease.nim.camellia.feign.naked.exception.ClientRetriableException;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreaker;
import com.netease.nim.camellia.tools.circuitbreaker.CamelliaCircuitBreakerException;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 一个简单的封装了重试逻辑的client
 * Created by yuanyuanjun on 2019/4/22.
 */
public class CamelliaNakedClient<R, W> {

    public static enum OperationType {
        READ,
        WRITE,
        ;
    }

    private CamelliaFeignEnv feignEnv = CamelliaFeignEnv.defaultFeignEnv();

    private CamelliaNakedRequestInvoker<R, W> camelliaNakedRequestInvoker;
    private int defaultMaxRetry = 0;

    private long bid = -1;
    private String bgroup = "default";
    private CamelliaApi camelliaApi;
    private ResourceTable resourceTable;
    private CamelliaFeignDynamicOptionGetter dynamicOptionGetter;

    private final Map<String, FeignResourcePool> map = new HashMap<>();

    private ResourceChooser resourceChooser;
    private final ConcurrentHashMap<String, ResourceChooser> resourceChooserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaCircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaDashboardFeignResourceTableUpdater> updaterMap = new ConcurrentHashMap<>();

    public static class Builder<R, W> {

        private final CamelliaNakedClient<R, W> client = new CamelliaNakedClient<>();

        public Builder() {
        }

        public Builder<R, W> bid(long bid) {
            client.bid = bid;
            return this;
        }

        public Builder<R, W> bgroup(String bgroup) {
            client.bgroup = bgroup;
            return this;
        }

        public Builder<R, W> camelliaApi(CamelliaApi camelliaApi) {
            client.camelliaApi = camelliaApi;
            return this;
        }

        public Builder<R, W> feignEnv(CamelliaFeignEnv feignEnv) {
            client.feignEnv = feignEnv;
            return this;
        }

        public Builder<R, W> dynamicOptionGetter(CamelliaFeignDynamicOptionGetter dynamicOptionGetter) {
            client.dynamicOptionGetter = dynamicOptionGetter;
            return this;
        }

        public Builder<R, W> resourceTable(String route) {
            if (route != null) {
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(route);
                FeignResourceUtils.checkResourceTable(resourceTable);
                client.resourceTable = resourceTable;
            }
            return this;
        }

        public Builder<R, W> defaultMaxRetry(int defaultMaxRetry) {
            client.defaultMaxRetry = defaultMaxRetry;
            return this;
        }

        public CamelliaNakedClient<R, W> build(CamelliaNakedRequestInvoker<R, W> camelliaNakedRequestInvoker) {
            client.camelliaNakedRequestInvoker = camelliaNakedRequestInvoker;
            client.init();
            return client;
        }
    }

    /**
     * 执行请求
     * @param operationType 读写类型
     * @param request 请求体
     * @return 响应
     * @throws ClientException 异常
     */
    public W sendRequest(OperationType operationType, R request) throws ClientException {
        return sendRequest(operationType, request, null, null, defaultMaxRetry);
    }

    /**
     * 执行请求
     * @param operationType 读写类型
     * @param request 请求体
     * @param routeKey 路由key
     * @param loadBalanceKey 负载均衡key
     * @param maxRetry 最大重试次数
     * @return 响应
     * @throws ClientException 异常
     */
    public W sendRequest(OperationType operationType, R request, Object routeKey, Object loadBalanceKey, int maxRetry) throws ClientException {
        try {
            int retry;
            if (maxRetry < 0) {
                retry = defaultMaxRetry;
            } else {
                retry = maxRetry;
            }
            Pair<ResourceChooser, String> pair = getResourceChooser(routeKey);
            ResourceChooser resourceChooser = pair.first;
            String bgroup = pair.second;
            if (operationType == OperationType.READ) {
                Resource resource = resourceChooser.getReadResource(ResourceChooser.EMPTY_ARRAY);
                return invoke(resource, request, retry, loadBalanceKey, bgroup);
            } else if (operationType == OperationType.WRITE) {
                List<Resource> writeResources = resourceChooser.getWriteResources(ResourceChooser.EMPTY_ARRAY);
                if (writeResources.size() == 1) {
                    Resource resource = writeResources.get(0);
                    return invoke(resource, request, retry, loadBalanceKey, bgroup);
                }
                MultiWriteType multiWriteType = feignEnv.getProxyEnv().getMultiWriteType();
                if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
                    W result = null;
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(0);
                        boolean first = i == 0;
                        if (first) {
                            result = invoke(resource, request, retry, loadBalanceKey, bgroup);
                        } else {
                            invoke(resource, request, retry, loadBalanceKey, bgroup);
                        }
                    }
                    return result;
                } else if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
                    ThreadContextSwitchStrategy strategy = feignEnv.getProxyEnv().getThreadContextSwitchStrategy();
                    List<Future<W>> futureList = new ArrayList<>();
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(0);
                        Future<W> future = feignEnv.getProxyEnv().getMultiWriteConcurrentExec()
                                .submit(strategy.wrapperCallable(() -> invoke(resource, request, retry, loadBalanceKey, bgroup)));
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
                    List<Future<W>> futureList = new ArrayList<>();
                    for (int i = 0; i < writeResources.size(); i++) {
                        Resource resource = writeResources.get(0);
                        Future<W> future = feignEnv.getProxyEnv().getMultiWriteAsyncExec()
                                .submit(strategy.wrapperCallable(() -> invoke(resource, request, retry, loadBalanceKey, bgroup)));
                        futureList.add(future);
                    }
                    return futureList.get(0).get();
                }
            }
            throw new ClientNoRetriableException("unknown operationType");
        } catch (Exception e) {
            Throwable ex = ExceptionUtils.onError(e);
            if (ex instanceof ClientException) {
                throw (ClientException) ex;
            }
            throw new ClientException(ex);
        }
    }

    private W invoke(Resource resource, R request, int maxRetry, Object loadBalanceKey, String bgroup) throws ClientException {
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
            ClientException throwException = null;
            FeignResource feignResource = null;
            FeignResourcePool resourcePool = getResourcePool(resource, bgroup);
            while (retry <= maxRetry) {
                try {
                    feignResource = resourcePool.getResource(loadBalanceKey);
                    return camelliaNakedRequestInvoker.doRequest(feignResource, request);
                } catch (ClientRetriableException e) {
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
        } catch (ClientException e) {
            success = false;
            throw e;
        } catch (Exception e) {
            success = false;
            throw new ClientNoRetriableException(e);
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
                        pool = new DiscoveryResourcePool((FeignDiscoveryResource) resource, discovery, serverSelector, healthChecker);
                    } else {
                        throw new IllegalArgumentException("not support resource");
                    }
                    map.put(key, pool);
                }
            }
        }
        return pool;

    }

    private Pair<ResourceChooser, String> getResourceChooser(Object routeKey) {
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
        if (resourceChooser == null && resourceTable != null) {
            resourceChooser = new ResourceChooser(resourceTable, feignEnv.getProxyEnv());
        }
        return new Pair<>(resourceChooser, bgroup);
    }

    private static class Pair<T, W> {
        T first;
        W second;

        public Pair(T first, W second) {
            this.first = first;
            this.second = second;
        }
    }

    private ResourceChooser getResourceChooserByBgroup(String bgroup) {
        ResourceChooser resourceChooser = resourceChooserMap.get(bgroup);
        if (resourceChooser == null) {
            FeignResourceTableUpdater updater = initUpdater(bgroup);
            resourceChooser = resourceChooserMap.get(bgroup);
            if (resourceChooser == null) {
                return new ResourceChooser(updater.getResourceTable(), feignEnv.getProxyEnv());
            }
        }
        return resourceChooser;
    }

    private FeignResourceTableUpdater initUpdater(String bgroup) {
        CamelliaDashboardFeignResourceTableUpdater updater = updaterMap.get(bgroup);
        if (updater == null) {
            synchronized (updaterMap) {
                updater = updaterMap.get(bgroup);
                if (updater == null) {
                    updater = new CamelliaDashboardFeignResourceTableUpdater(camelliaApi, bid, bgroup, 5000);
                    ResourceTable resourceTable = updater.getResourceTable();
                    ResourceChooser resourceChooser = new ResourceChooser(resourceTable, feignEnv.getProxyEnv());
                    updater.addCallback(table -> resourceChooserMap.put(bgroup, new ResourceChooser(table, feignEnv.getProxyEnv())));
                    resourceChooserMap.put(bgroup, resourceChooser);
                    updaterMap.put(bgroup, updater);
                }
            }
        }
        return updater;
    }
}
