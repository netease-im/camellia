package com.netease.nim.camellia.hot.key.server.monitor;

import com.netease.nim.camellia.hot.key.server.callback.HotKeyInfo;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 仅记录最近的几个热key
 * Created by caojiajun on 2023/5/11
 */
public class HotKeyCollector {

    private static NamespaceCamelliaLocalCache cache;

    public static void init(HotKeyServerProperties properties) {
        cache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getMonitorHotKeyMaxCount());
    }

    public static void update(HotKeyInfo hotKeyInfo) {
        if (cache == null) return;
        cache.put(hotKeyInfo.getNamespace(), hotKeyInfo.getKey() + "|" + hotKeyInfo.getAction().getValue(), hotKeyInfo, -1);
    }

    public static List<HotKeyInfo> collect() {
        if (cache == null) {
            return new ArrayList<>();
        }
        List<HotKeyInfo> list = new ArrayList<>();
        Set<String> set = cache.namespaceSet();
        for (String namespace : set) {
            List<Object> values = cache.values(namespace);
            for (Object value : values) {
                if (value instanceof HotKeyInfo) {
                    HotKeyInfo hotKeyInfo = (HotKeyInfo) value;
                    list.add(hotKeyInfo);
                    cache.evict(hotKeyInfo.getNamespace(), hotKeyInfo.getKey() + "|" + hotKeyInfo.getAction().getValue());
                }
            }
        }
        return list;
    }
}
