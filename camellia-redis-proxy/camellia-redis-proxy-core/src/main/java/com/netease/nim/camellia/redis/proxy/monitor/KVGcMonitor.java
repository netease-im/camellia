package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KVGcStats;
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
public class KVGcMonitor {

    private static final ConcurrentHashMap<String, LongAdder> map1 = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> map2 = new ConcurrentHashMap<>();

    public static void deleteMetaKeys(String namespace, long deleteMetaKeys) {
        CamelliaMapUtils.computeIfAbsent(map1, namespace, k -> new LongAdder()).add(deleteMetaKeys);
    }

    public static void deleteSubKeys(String namespace, long deleteSubKeys) {
        CamelliaMapUtils.computeIfAbsent(map2, namespace, k -> new LongAdder()).add(deleteSubKeys);
    }

    public static List<KVGcStats> collect() {
        Set<String> namespace = new HashSet<>();
        namespace.addAll(map1.keySet());
        namespace.addAll(map2.keySet());
        List<KVGcStats> list = new ArrayList<>();
        for (String string : namespace) {
            LongAdder count1 = map1.get(string);
            LongAdder count2 = map2.get(string);
            KVGcStats stats = new KVGcStats();
            stats.setNamespace(string);
            stats.setDeleteMetaKeys(count1 == null ? 0 : count1.sumThenReset());
            stats.setDeleteSubKeys(count2 == null ? 0 : count2.sumThenReset());
            list.add(stats);
        }
        return list;
    }
}
