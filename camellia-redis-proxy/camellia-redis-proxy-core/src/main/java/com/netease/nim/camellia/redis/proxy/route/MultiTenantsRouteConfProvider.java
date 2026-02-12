package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigSelector;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2026/2/11
 */
public abstract class MultiTenantsRouteConfProvider extends RouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantsRouteConfProvider.class);

    protected static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-multi-tenants-route"));

    private MultiTenantConfigSelector selector;

    public MultiTenantsRouteConfProvider() {
        List<MultiTenantConfig> multiTenantConfig = getMultiTenantConfig();
        if (multiTenantConfig == null) {
            throw new IllegalArgumentException("multi config init error");
        }
        selector = new MultiTenantConfigSelector(multiTenantConfig);
        ProxyDynamicConf.registerCallback(this::reload);
        int reloadIntervalMillis = ProxyDynamicConf.getInt("multi.tenant.route.conf.reload.interval.millis", 30000);
        scheduler.scheduleAtFixedRate(this::reload, reloadIntervalMillis, reloadIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = selector.selectClientIdentity(password);
        if (clientIdentity == null) {
            return ClientIdentity.AUTH_FAIL;
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }

    @Override
    public ResourceTable getRouteConfig(long bid, String bgroup) {
        return selector.selectResourceTable(bid, bgroup);
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return true;
    }

    public abstract List<MultiTenantConfig> getMultiTenantConfig();

    protected synchronized final void reload() {
        try {
            List<MultiTenantConfig> config = getMultiTenantConfig();
            if (config == null) {
                return;
            }
            update(config);
        } catch (Exception e) {
            logger.error("reload multi tenant config error", e);
        }
    }

    protected final void checkValid(MultiTenantConfig config) {
        try {
            if (config == null) {
                throw new IllegalArgumentException("multi tenant config is null");
            }
            if (config.getBid() <= 0) {
                throw new IllegalArgumentException("multi tenant config illegal bid");
            }
            if (config.getBgroup() == null) {
                throw new IllegalArgumentException("multi tenant config illegal bgroup");
            }
            if (config.getPassword() == null) {
                throw new IllegalArgumentException("multi tenant config illegal password");
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config.getRoute());
            RedisResourceUtil.checkResourceTable(resourceTable);
        } catch (Exception e) {
            throw new IllegalArgumentException("multi tenant config error", e);
        }
    }

    protected final void update(List<MultiTenantConfig> multiTenantConfig) {
        Collections.sort(multiTenantConfig);
        if (selector != null && multiTenantConfig.equals(selector.getConfigList())) {
            return;
        }
        MultiTenantConfigSelector newSelector = new MultiTenantConfigSelector(multiTenantConfig);
        Map<String, ResourceTable> oldMap = this.selector.getResourceTableMap();
        Map<String, ResourceTable> newMap = newSelector.getResourceTableMap();
        this.selector = newSelector;
        Set<String> set = new HashSet<>(oldMap.keySet());
        set.addAll(newMap.keySet());
        for (String bgroup : set) {
            ResourceTable oldTable = oldMap.get(bgroup);
            ResourceTable newTable = newMap.get(bgroup);
            if (newTable == null && oldTable != null) {
                boolean enable = ProxyDynamicConf.getBoolean("route.config.remove.enable", true);
                if (enable) {
                    invokeRemoveResourceTable(1L, bgroup);
                }
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

}
