package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteConfMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RouteConfMonitor.class);

    private static final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> templateMap = new ConcurrentHashMap<>();

    public static void registerRedisTemplate(Long bid, String bgroup, AsyncCamelliaRedisTemplate template) {
        try {
            templateMap.put(bid + "|" + bgroup, template);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> getTemplateMap() {
        return templateMap;
    }

    public static List<Stats.RouteConf> calc() {
        List<Stats.RouteConf> routeConfList = new ArrayList<>();
        try {
            for (Map.Entry<String, AsyncCamelliaRedisTemplate> entry : templateMap.entrySet()) {
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
                Stats.RouteConf routeConf = new Stats.RouteConf();
                routeConf.setBid(bid);
                routeConf.setBgroup(bgroup);
                routeConf.setResourceTable(resourceTable);
                routeConf.setUpdateTime(entry.getValue().getResourceTableUpdateTime());
                routeConfList.add(routeConf);
            }
            return routeConfList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return routeConfList;
        }
    }
}
