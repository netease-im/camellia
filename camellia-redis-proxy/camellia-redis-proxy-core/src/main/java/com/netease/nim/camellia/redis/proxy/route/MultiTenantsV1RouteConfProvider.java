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

    private static final Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)\\.password\\.(\\d+)\\.([a-zA-Z0-9_]+)\\.route\\.conf$");

    @Override
    public List<MultiTenantConfig> getMultiTenantConfig() {
        try {
            List<MultiTenantConfig> list = new ArrayList<>();
            Map<String, String> map = ProxyDynamicConf.getAll();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                try {
                    String key = entry.getKey();
                    Matcher matcher = pattern.matcher(key);
                    if (matcher.matches()) {
                        String password = matcher.group(1);
                        long bid = Long.parseLong(matcher.group(2));
                        String bgroup = matcher.group(3);
                        String route = entry.getValue();

                        MultiTenantConfig config = new MultiTenantConfig();
                        config.setBid(bid);
                        config.setBgroup(bgroup);
                        config.setPassword(password);
                        config.setRoute(route);
                        checkValid(config);
                        list.add(config);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("multi config parse error", e);
            return null;
        }
    }

}
