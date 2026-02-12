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
    private final long defaultBid;
    private final String defaultBgroup;

    public MultiTenantsRouteConfProvider() {
        List<MultiTenantConfig> configList = getMultiTenantConfig();
        if (configList == null) {
            throw new IllegalArgumentException("multi config init error");
        }
        check(configList);
        defaultBid = ProxyDynamicConf.getInt("multi.tenant.route.default.bid", -1);
        defaultBgroup = ProxyDynamicConf.getString("multi.tenant.route.default.bgroup", "default");
        selector = new MultiTenantConfigSelector(configList);
        if (defaultBid > 0 && defaultBgroup != null) {
            selector.selectResourceTable(defaultBid, defaultBgroup);
        }
        ProxyDynamicConf.registerCallback(this::reload);
        int reloadIntervalMillis = ProxyDynamicConf.getInt("multi.tenant.route.conf.reload.interval.millis", 30000);
        scheduler.scheduleAtFixedRate(this::reload, reloadIntervalMillis, reloadIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getRouteConfig() {
        if (defaultBid > 0 && defaultBgroup != null) {
            return selector.selectResourceTable(defaultBid, defaultBgroup);
        }
        return null;
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
    public String getRouteConfig(long bid, String bgroup) {
        return selector.selectResourceTable(bid, bgroup);
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return true;
    }

    public abstract List<MultiTenantConfig> getMultiTenantConfig();

    protected synchronized final void reload() {
        try {
            List<MultiTenantConfig> configList = getMultiTenantConfig();
            if (configList == null) {
                return;
            }
            check(configList);
            update(configList);
        } catch (Exception e) {
            logger.error("reload multi tenant config error", e);
        }
    }

    private void check(List<MultiTenantConfig> configList) {
        Set<String> passwordSet = new HashSet<>();
        Set<String> bidBgroupSet = new HashSet<>();
        for (MultiTenantConfig config : configList) {
            checkValid(config);
            String password = config.getPassword();
            if (passwordSet.contains(password)) {
                throw new IllegalArgumentException("duplicate password");
            }
            passwordSet.add(password);
            String key = config.getBid() + "|" + config.getBgroup();
            if (bidBgroupSet.contains(key)) {
                throw new IllegalArgumentException("duplicate bid/bgroup");
            }
            bidBgroupSet.add(key);
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

    protected final void update(List<MultiTenantConfig> configList) {
        Collections.sort(configList);
        if (selector != null && configList.equals(selector.getConfigList())) {
            return;
        }
        MultiTenantConfigSelector newSelector = new MultiTenantConfigSelector(configList);
        Map<String, String> oldMap = this.selector.getResourceTableMap();
        Map<String, String> newMap = newSelector.getResourceTableMap();
        this.selector = newSelector;
        Set<String> set = new HashSet<>(oldMap.keySet());
        set.addAll(newMap.keySet());
        for (String key : set) {
            String[] split = key.split("\\|");
            long bid = Long.parseLong(split[0]);
            String bgroup = split[1];
            //
            String oldRoute = oldMap.get(key);
            String newRoute = newMap.get(key);
            if (newRoute == null && oldRoute != null) {
                boolean enable = ProxyDynamicConf.getBoolean("multi.tenant.route.conf.remove.enable", true);
                if (enable) {
                    invokeRemoveResourceTable(bid, bgroup);
                }
                continue;
            }
            if (newRoute != null && oldRoute != null) {
                if (!newRoute.equals(oldRoute)) {
                    invokeUpdateResourceTable(bid, bgroup, newRoute);
                }
            }
        }
    }

}
