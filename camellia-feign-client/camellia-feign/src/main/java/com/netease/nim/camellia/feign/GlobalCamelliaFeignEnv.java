package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.HashCamelliaServerSelector;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceSelector;
import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import com.netease.nim.camellia.feign.resource.FeignResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只有全局仅有一个CamelliaFeignClientFactory实例的情况下，才能使用该类
 * Created by caojiajun on 2023/10/26
 */
public class GlobalCamelliaFeignEnv {

    private static final Logger logger = LoggerFactory.getLogger(GlobalCamelliaFeignEnv.class);

    private static CamelliaFeignDynamicOptionGetter dynamicOptionGetter;

    //bid|bgroup -> ResourceSelector
    private static final ConcurrentHashMap<String, ResourceSelector> resourceSelectorMap = new ConcurrentHashMap<>();

    //url -> SelectorInfo
    private static final ConcurrentHashMap<String, SelectorInfo> selectorInfoMap = new ConcurrentHashMap<>();

    /**
     * 获取负载均衡code
     * @param bid bid
     * @param apiType apiType
     * @param operationType 类型
     * @param routeKey 路由key
     * @param loadBalanceKey 负载均衡key
     * @return 如果-1表示未知，否则表示具体的负载均衡code，相同code的目标主机应该是一样的
     */
    public static long selectLoadBalanceCode(Long bid, Class<?> apiType, OperationType operationType, Object routeKey, Object loadBalanceKey) {
        try {
            if (dynamicOptionGetter == null) {
                return -1;
            }
            if (loadBalanceKey == null) {
                return -1;
            }
            String bgroup = dynamicOptionGetter.getDynamicRouteConfGetter(bid).bgroup(routeKey);
            String key = bid + "|" + bgroup + "|" + apiType.getName();
            ResourceSelector resourceSelector = resourceSelectorMap.get(key);
            if (resourceSelector == null) {
                return -1;
            }
            if (operationType == OperationType.READ || operationType == OperationType.UNKNOWN) {
                Resource resource = resourceSelector.getReadResource(ResourceSelector.EMPTY_ARRAY);
                SelectorInfo selectorInfo = selectorInfoMap.get(resource.getUrl());
                if (selectorInfo == null) {
                    return -1;
                }
                return Math.abs(code(resource, selectorInfo.selectLoadBalanceCode(loadBalanceKey)));
            } else if (operationType == OperationType.WRITE) {
                List<Resource> resources = resourceSelector.getWriteResources(ResourceSelector.EMPTY_ARRAY);
                long code = 0;
                for (Resource resource : resources) {
                    SelectorInfo selectorInfo = selectorInfoMap.get(resource.getUrl());
                    if (selectorInfo == null) {
                        return -1;
                    }
                    long simpleCode = code(resource, selectorInfo.selectLoadBalanceCode(loadBalanceKey));
                    if (simpleCode == -1) {
                        return -1;
                    }
                    code += simpleCode;
                }
                return Math.abs(code);
            }
            return -1;
        } catch (Exception e) {
            logger.error("selectLoadBalanceCode error, bid = {}, apiType = {}, operationType = {}, routeKey = {}, loadBalanceKey = {}",
                    bid, apiType.getName(), operationType, routeKey, loadBalanceKey, e);
            return -1;
        }
    }

    public static void register(CamelliaFeignDynamicOptionGetter dynamicOptionGetter) {
        try {
            if (GlobalCamelliaFeignEnv.dynamicOptionGetter != null) {
                logger.warn("GlobalCamelliaFeignEnv already registered");
                return;
            }
            GlobalCamelliaFeignEnv.dynamicOptionGetter = dynamicOptionGetter;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void register(Long bid, String bgroup, Class<?> apiType, ResourceSelector resourceSelector) {
        try {
            String key = bid + "|" + bgroup + "|" + apiType.getName();
            resourceSelectorMap.put(key, resourceSelector);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void register(Resource resource, CamelliaServerSelector<FeignResource> serverSelector, List<FeignResource> resourceList) {
        try {
            SelectorInfo selectorInfo = new SelectorInfo(serverSelector, resourceList);
            selectorInfoMap.put(resource.getUrl(), selectorInfo);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static long code(Resource resource, int code) {
        return (((long) Math.abs(resource.hashCode())) << 16) | code;
    }

    public static enum OperationType {
        READ,
        WRITE,
        UNKNOWN;
    }

    public static class SelectorInfo {

        private final CamelliaServerSelector<FeignResource> serverSelector;
        private final List<FeignResource> resourceList;

        public SelectorInfo(CamelliaServerSelector<FeignResource> serverSelector, List<FeignResource> resourceList) {
            this.serverSelector = serverSelector;
            this.resourceList = resourceList;
        }

        public int selectLoadBalanceCode(Object loadBalanceKey) {
            if (serverSelector instanceof HashCamelliaServerSelector) {
                if (loadBalanceKey == null) {
                    return -1;
                }
                if (resourceList == null || resourceList.isEmpty()) {
                    return -1;
                }
                return Math.abs(loadBalanceKey.hashCode()) % resourceList.size();
            }
            return -1;
        }
    }
}
