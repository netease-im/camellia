package com.netease.nim.camellia.redis.proxy.route;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigSelector;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantProxyRouteConfUpdater.class);

    private List<MultiTenantConfig> multiTenantConfig;
    private MultiTenantConfigSelector selector;

    public MultiTenantProxyRouteConfUpdater() {
        this.multiTenantConfig = MultiTenantConfigUtils.getMultiTenantConfig();
        if (multiTenantConfig == null) {
            throw new IllegalArgumentException("multiTenantConfig init error");
        }
        this.selector = new MultiTenantConfigSelector(multiTenantConfig);
        logger.info("MultiTenantProxyRouteConfUpdater init, config = {}", JSONObject.toJSONString(multiTenantConfig));
        ProxyDynamicConf.registerCallback(() -> {
            List<MultiTenantConfig> config = MultiTenantConfigUtils.getMultiTenantConfig();
            if (config == null) {
                logger.error("MultiTenantConfig refresh failed, skip reload route conf");
                return;
            }
            update(config);
            logger.info("MultiTenantProxyRouteConfUpdater update, config = {}", JSONObject.toJSONString(config));
        });
    }

    private void update(List<MultiTenantConfig> multiTenantConfig) {
        MultiTenantConfigSelector newSelector = new MultiTenantConfigSelector(multiTenantConfig);
        Map<String, ResourceTable> oldMap = this.selector.getResourceTableMap();
        Map<String, ResourceTable> newMap = newSelector.getResourceTableMap();
        this.selector = newSelector;
        this.multiTenantConfig = multiTenantConfig;
        Set<String> set = new HashSet<>(oldMap.keySet());
        set.addAll(newMap.keySet());
        for (String bgroup : set) {
            ResourceTable oldTable = oldMap.get(bgroup);
            ResourceTable newTable = newMap.get(bgroup);
            if (newTable == null && oldTable != null) {
                invokeRemoveResourceTable(1L, bgroup);
                continue;
            }
            if (newTable != null && oldTable != null) {
                String newJson = ReadableResourceTableUtil.readableResourceTable(newTable);
                String oldJson = ReadableResourceTableUtil.readableResourceTable(oldTable);
                if (newJson != null && oldJson != null && !newJson.equals(oldJson)) {
                    invokeUpdateResourceTable(1L, bgroup, newTable);
                }
            }
        }
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        return selector.selectResourceTable(bgroup);
    }

}
