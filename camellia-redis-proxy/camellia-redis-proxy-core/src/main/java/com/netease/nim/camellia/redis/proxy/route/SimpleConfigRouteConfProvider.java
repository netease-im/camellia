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

    private static final Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)\\.password\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf\\.biz");

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
                Matcher matcher = pattern.matcher(entryKey);
                if (matcher.matches()) {
                    String password = matcher.group(1);
                    long bid = Long.parseLong(matcher.group(2));
                    String bgroup = matcher.group(3);
                    String biz = entry.getValue();

                    String url = ProxyDynamicConf.getString("simple.config.fetch.url", null);
                    String key = ProxyDynamicConf.getString("simple.config.fetch.key", null);
                    String secret = ProxyDynamicConf.getString("simple.config.fetch.secret", null);

                    String cacheKey = url + "|" + key + "|" + secret + "|" + biz;
                    SimpleConfigFetcher fetcher = fetcherMap.get(cacheKey);
                    if (fetcher == null) {
                        fetcher = fetcherMap.computeIfAbsent(cacheKey, k -> new SimpleConfigFetcher(url, biz, key, secret));
                    }
                    String route = fetcher.getConfig();

                    MultiTenantConfig config = new MultiTenantConfig();
                    config.setBid(bid);
                    config.setBgroup(bgroup);
                    config.setPassword(password);
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
}
