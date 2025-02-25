package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvGcStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KvGcMonitor {

    private static final ConcurrentHashMap<String, LongAdder> map1 = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> map2 = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, LongAdder> map3 = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> map4 = new ConcurrentHashMap<>();

    public static void scanMetaKeys(String namespace, long scanMetaKeys) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(map3, namespace, k -> new LongAdder()).add(scanMetaKeys);
    }

    public static void deleteMetaKeys(String namespace, long deleteMetaKeys) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(map1, namespace, k -> new LongAdder()).add(deleteMetaKeys);
    }

    public static void scanSubKeys(String namespace, long scanSubKeys) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(map4, namespace, k -> new LongAdder()).add(scanSubKeys);
    }

    public static void deleteSubKeys(String namespace, long deleteSubKeys) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(map2, namespace, k -> new LongAdder()).add(deleteSubKeys);
    }

    public static List<KvGcStats> collect() {
        Set<String> namespace = new HashSet<>();
        namespace.addAll(map1.keySet());
        namespace.addAll(map2.keySet());
        namespace.addAll(map3.keySet());
        namespace.addAll(map4.keySet());
        List<KvGcStats> list = new ArrayList<>();
        for (String string : namespace) {
            LongAdder count1 = map1.get(string);
            LongAdder count2 = map2.get(string);
            LongAdder count3 = map3.get(string);
            LongAdder count4 = map4.get(string);
            KvGcStats stats = new KvGcStats();
            stats.setNamespace(string);
            stats.setDeleteMetaKeys(count1 == null ? 0 : count1.sumThenReset());
            stats.setDeleteSubKeys(count2 == null ? 0 : count2.sumThenReset());
            stats.setScanMetaKeys(count3 == null ? 0 : count3.sumThenReset());
            stats.setScanSubKeys(count4 == null ? 0 : count4.sumThenReset());
            list.add(stats);
        }
        return list;
    }
}
