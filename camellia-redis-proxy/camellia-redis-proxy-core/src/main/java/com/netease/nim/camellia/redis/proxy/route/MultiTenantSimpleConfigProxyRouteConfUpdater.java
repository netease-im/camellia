package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfigUtils;
import com.netease.nim.camellia.redis.proxy.conf.MultiTenantSimpleConfig;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.config.SimpleConfigFetcher;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2025/9/8
 */
public class MultiTenantSimpleConfigProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantSimpleConfigProxyRouteConfUpdater.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(MultiTenantSimpleConfigProxyRouteConfUpdater.class));

    private final ReentrantLock lock = new ReentrantLock();

    private List<MultiTenantSimpleConfig> oldConfigs = null;
    private String oldFetchUrl = null;
    private String oldFetchKey = null;
    private String oldFetchSecret = null;
    private Map<String, RouteConfig> bizMap = new ConcurrentHashMap<>();

    public MultiTenantSimpleConfigProxyRouteConfUpdater() {
        update();
        ProxyDynamicConf.registerCallback(this::update);
        scheduler.scheduleAtFixedRate(this::update, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        RouteConfig config = bizMap.get(bgroup);
        if (config == null) {
            return null;
        }
        return config.getResourceTable();
    }

    private void update() {
        lock.lock();
        try {
            List<MultiTenantSimpleConfig> configs = MultiTenantConfigUtils.getMultiTenantSimpleConfig();
            String url = ProxyDynamicConf.getString("simple.config.fetch.url", null);
            String key = ProxyDynamicConf.getString("simple.config.fetch.key", null);
            String secret = ProxyDynamicConf.getString("simple.config.fetch.secret", null);
            if (url == null) {
                logger.warn("'simple.config.fetch.url' is null");
                return;
            }
            if (Objects.equals(configs, oldConfigs) && Objects.equals(url, oldFetchUrl)
                    && Objects.equals(key, oldFetchKey) && Objects.equals(secret, oldFetchSecret)) {
                for (Map.Entry<String, RouteConfig> entry : bizMap.entrySet()) {
                    RouteConfig routeConfig = entry.getValue();
                    try {
                        String newConfig = routeConfig.getFetcher().getConfig();
                        if (Objects.equals(routeConfig.getConfig(), newConfig)) {
                            continue;
                        }
                        ResourceTable resourceTable = parse(newConfig);
                        routeConfig.config = newConfig;
                        routeConfig.resourceTable = resourceTable;
                        invokeUpdateResourceTable(1L, entry.getKey(), resourceTable);
                    } catch (Exception e) {
                        logger.error("route config update error, biz = {}", routeConfig.getBiz(), e);
                    }
                }
                return;
            }
            Set<String> removedSet = bizMap == null ? new HashSet<>() : new HashSet<>(bizMap.keySet());
            Map<String, RouteConfig> newMap = new HashMap<>();
            for (MultiTenantSimpleConfig simpleConfig : configs) {
                SimpleConfigFetcher fetcher = new SimpleConfigFetcher(url, simpleConfig.getBiz(), key, secret);
                String config = fetcher.getConfig();
                ResourceTable resourceTable = parse(config);
                RouteConfig routeConfig = new RouteConfig(simpleConfig.getBiz(), config, resourceTable, fetcher);
                newMap.put(simpleConfig.getName(), routeConfig);
                removedSet.remove(simpleConfig.getName());
                String oldConfig = null;
                if (bizMap != null) {
                    RouteConfig oldRouteConfig = bizMap.get(simpleConfig.getName());
                    if (oldRouteConfig != null) {
                        oldConfig = oldRouteConfig.getConfig();
                    }
                }
                if (!Objects.equals(oldConfig, config)) {
                    invokeUpdateResourceTable(1L, simpleConfig.getName(), resourceTable);
                }
            }
            if (!removedSet.isEmpty()) {
                for (String name : removedSet) {
                    invokeRemoveResourceTable(1L, name);
                }
            }
            this.bizMap = newMap;
            this.oldConfigs = configs;
            this.oldFetchUrl = url;
            this.oldFetchKey = key;
            this.oldFetchSecret = secret;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private ResourceTable parse(String config) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config);
        checkResourceTable(resourceTable);
        return resourceTable;
    }

    private static class RouteConfig {
        private String biz;
        private String config;
        private ResourceTable resourceTable;
        private SimpleConfigFetcher fetcher;

        public RouteConfig(String biz, String config, ResourceTable resourceTable, SimpleConfigFetcher fetcher) {
            this.biz = biz;
            this.config = config;
            this.resourceTable = resourceTable;
            this.fetcher = fetcher;
        }

        public String getBiz() {
            return biz;
        }

        public void setBiz(String biz) {
            this.biz = biz;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public ResourceTable getResourceTable() {
            return resourceTable;
        }

        public void setResourceTable(ResourceTable resourceTable) {
            this.resourceTable = resourceTable;
        }

        public SimpleConfigFetcher getFetcher() {
            return fetcher;
        }

        public void setFetcher(SimpleConfigFetcher fetcher) {
            this.fetcher = fetcher;
        }
    }
}
