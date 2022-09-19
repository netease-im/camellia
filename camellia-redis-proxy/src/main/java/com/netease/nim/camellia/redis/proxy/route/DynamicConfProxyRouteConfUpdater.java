package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于ProxyDynamicConf的ProxyRouteConfUpdater实现
 * 该实现下所有配置均以k-v的形式保存在本地配置文件中，当配置文件发生变更时，可以调用ProxyDynamicConf.reload()来立即生效
 * Created by caojiajun on 2021/4/22
 */
public class DynamicConfProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public DynamicConfProxyRouteConfUpdater() {
        ProxyDynamicConf.registerCallback(this::reload);
    }

    private void reload() {
        for (String key : map.keySet()) {
            try {
                String[] split = key.split("\\|");
                long bid = Long.parseLong(split[0]);
                String bgroup = split[1];
                String oldConf = map.get(key);
                String newConf = getConf(bid, bgroup);
                if (newConf == null) {
                    invokeRemoveResourceTable(bid, bgroup);
                    continue;
                }
                if (!oldConf.equals(newConf)) {
                    ResourceTable resourceTable = getResourceTable(bid, bgroup);
                    //触发回调
                    invokeUpdateResourceTable(bid, bgroup, resourceTable);
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(DynamicConfProxyRouteConfUpdater.class, "reload error, key = " + key, e);
            }
        }
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        String string = getConf(bid, bgroup);
        if (string == null) return null;
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(string);
        RedisResourceUtil.checkResourceTable(resourceTable);
        map.put(bid + "|" + bgroup, string);
        return resourceTable;
    }

    private String getConf(long bid, String bgroup) {
        return ProxyDynamicConf.getString(bid + "." + bgroup + ".route.conf", null);
    }
}
