package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.monitor.model.RouteConf;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteConfMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RouteConfMonitor.class);

    private static final ConcurrentHashMap<String, UpstreamRedisClientTemplate> templateMap = new ConcurrentHashMap<>();

    public static void registerRedisClientTemplate(Long bid, String bgroup, UpstreamRedisClientTemplate template) {
        try {
            templateMap.put(Utils.getCacheKey(bid, bgroup), template);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void deregisterRedisClientTemplate(Long bid, String bgroup) {
        try {
            templateMap.remove(Utils.getCacheKey(bid, bgroup));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static ConcurrentHashMap<String, UpstreamRedisClientTemplate> getTemplateMap() {
        return templateMap;
    }

    public static List<RouteConf> collect() {
        List<RouteConf> routeConfList = new ArrayList<>();
        for (Map.Entry<String, UpstreamRedisClientTemplate> entry : templateMap.entrySet()) {
            String key = entry.getKey();
            String[] split = key.split("\\|");
            Long bid = null;
            if (!split[0].equals("null")) {
                bid = Long.parseLong(split[0]);
            }
            String bgroup = null;
            if (!split[1].equals("null")) {
                bgroup = split[1];
            }
            String resourceTable = ReadableResourceTableUtil.readableResourceTable(PasswordMaskUtils.maskResourceTable(entry.getValue().getResourceTable()));
            RouteConf routeConf = new RouteConf();
            routeConf.setBid(bid);
            routeConf.setBgroup(bgroup);
            routeConf.setResourceTable(resourceTable);
            routeConf.setUpdateTime(entry.getValue().getResourceTableUpdateTime());
            routeConfList.add(routeConf);
        }
        return routeConfList;
    }
}
