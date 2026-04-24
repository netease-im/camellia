package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.redis.proxy.conf.MultiTenantConfig;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对properties格式的配置友好
 * Created by caojiajun on 2026/2/11
 */
public class MultiTenantsV1RouteConfProvider extends MultiTenantsRouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantsV1RouteConfProvider.class);

    private static final Pattern LEGACY_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)\\.password\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^username\\.([a-zA-Z0-9_]+)\\.password\\.([a-zA-Z0-9_]+)\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf$");

    @Override
    public List<MultiTenantConfig> getMultiTenantConfig() {
        try {
            List<MultiTenantConfig> list = new ArrayList<>();
            Map<String, String> map = ProxyDynamicConf.getAll();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                try {
                    String key = entry.getKey();
                    MultiTenantConfig config = tryBuildConfig(key, entry.getValue());
                    if (config != null) {
                        checkValid(config);
                        list.add(config);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("multi config parse error", e);
            return null;
        }
    }

    private MultiTenantConfig tryBuildConfig(String key, String route) {
        Matcher usernameMatcher = USERNAME_PATTERN.matcher(key);
        if (usernameMatcher.matches()) {
            MultiTenantConfig config = new MultiTenantConfig();
            config.setUsername(usernameMatcher.group(1));
            config.setPassword(usernameMatcher.group(2));
            config.setBid(Long.parseLong(usernameMatcher.group(3)));
            config.setBgroup(usernameMatcher.group(4));
            config.setRoute(route);
            return config;
        }
        Matcher legacyMatcher = LEGACY_PATTERN.matcher(key);
        if (legacyMatcher.matches()) {
            MultiTenantConfig config = new MultiTenantConfig();
            config.setPassword(legacyMatcher.group(1));
            config.setBid(Long.parseLong(legacyMatcher.group(2)));
            config.setBgroup(legacyMatcher.group(3));
            config.setRoute(route);
            return config;
        }
        return null;
    }

}
