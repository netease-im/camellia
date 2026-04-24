package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.config.SimpleConfigFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by caojiajun on 2026/2/11
 */
public class SimpleConfigRouteConfProvider extends MultiTenantsRouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigRouteConfProvider.class);

    private static final Pattern LEGACY_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)\\.password\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf\\.biz$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^username\\.([a-zA-Z0-9_]+)\\.password\\.([a-zA-Z0-9_]+)\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf\\.biz$");

    private final ConcurrentHashMap<String, SimpleConfigFetcher> fetcherMap = new ConcurrentHashMap<>();

    public SimpleConfigRouteConfProvider() {
        super();
        int checkIntervalMillis = ProxyDynamicConf.getInt("simple.config.route.conf.reload.interval.millis", 3000);
        scheduler.scheduleAtFixedRate(this::reload, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<MultiTenantConfig> getMultiTenantConfig() {
        try {
            List<MultiTenantConfig> list = new ArrayList<>();
            Map<String, String> map = ProxyDynamicConf.getAll();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String entryKey = entry.getKey();
                MultiTenantConfig config = tryBuildConfig(entryKey, entry.getValue());
                if (config != null) {
                    String biz = config.getRoute();

                    String url = ProxyDynamicConf.getString("simple.config.fetch.url", null);
                    String key = ProxyDynamicConf.getString("simple.config.fetch.key", null);
                    String secret = ProxyDynamicConf.getString("simple.config.fetch.secret", null);

                    String cacheKey = url + "|" + key + "|" + secret + "|" + biz;
                    SimpleConfigFetcher fetcher = fetcherMap.get(cacheKey);
                    if (fetcher == null) {
                        fetcher = fetcherMap.computeIfAbsent(cacheKey, k -> new SimpleConfigFetcher(url, biz, key, secret));
                    }
                    String route = fetcher.getConfig();

                    config.setRoute(route);
                    checkValid(config);
                    list.add(config);
                }
            }
            Collections.sort(list);
            return list;
        } catch (Exception e) {
            logger.error("multi config error", e);
            return null;
        }
    }

    private MultiTenantConfig tryBuildConfig(String entryKey, String biz) {
        Matcher usernameMatcher = USERNAME_PATTERN.matcher(entryKey);
        if (usernameMatcher.matches()) {
            MultiTenantConfig config = new MultiTenantConfig();
            config.setUsername(usernameMatcher.group(1));
            config.setPassword(usernameMatcher.group(2));
            config.setBid(Long.parseLong(usernameMatcher.group(3)));
            config.setBgroup(usernameMatcher.group(4));
            config.setRoute(biz);
            return config;
        }
        Matcher legacyMatcher = LEGACY_PATTERN.matcher(entryKey);
        if (legacyMatcher.matches()) {
            MultiTenantConfig config = new MultiTenantConfig();
            config.setPassword(legacyMatcher.group(1));
            config.setBid(Long.parseLong(legacyMatcher.group(2)));
            config.setBgroup(legacyMatcher.group(3));
            config.setRoute(biz);
            return config;
        }
        return null;
    }
}
