package com.netease.nim.camellia.redis.toolkit.mergetask;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;

import java.util.function.Function;

/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskCache {

    private final ConcurrentLinkedHashMap<String, ConcurrentLinkedHashMap<String, Boolean>> taskCache;

    private final Function<String, ConcurrentLinkedHashMap<String, Boolean>> mappingFunction;

    public CamelliaMergeTaskCache(int taskCapacity, int taskIdCapacity) {
        taskCache = new ConcurrentLinkedHashMap.Builder<String, ConcurrentLinkedHashMap<String, Boolean>>()
                .initialCapacity(taskCapacity)
                .maximumWeightedCapacity(taskCapacity)
                .build();
        mappingFunction = k -> new ConcurrentLinkedHashMap.Builder<String, Boolean>().initialCapacity(taskIdCapacity).maximumWeightedCapacity(taskIdCapacity).build();
    }

    private ConcurrentLinkedHashMap<String, Boolean> getTaskMap(String taskKey) {
        return CamelliaMapUtils.computeIfAbsent(taskCache, taskKey, mappingFunction);
    }

    public void addTask(String taskKey, String taskId) {
        ConcurrentLinkedHashMap<String, Boolean> map = getTaskMap(taskKey);
        map.put(taskId, true);
    }

    public void removeTask(String taskKey, String taskId) {
        ConcurrentLinkedHashMap<String, Boolean> map = taskCache.get(taskKey);
        if (map != null) {
            map.remove(taskId);
            if (map.isEmpty()) {
                ConcurrentLinkedHashMap<String, Boolean> remove = taskCache.remove(taskKey);
                if (remove != null && !remove.isEmpty()) {
                    map = getTaskMap(taskKey);
                    map.putAll(remove);
                }
            }
        }
    }

    public int size(String taskKey) {
        ConcurrentLinkedHashMap<String, Boolean> map = taskCache.get(taskKey);
        if (map == null) return 0;
        if (map.isEmpty()) {
            ConcurrentLinkedHashMap<String, Boolean> remove = taskCache.remove(taskKey);
            if (remove != null && !remove.isEmpty()) {
                map = getTaskMap(taskKey);
                map.putAll(remove);
            }
            return 0;
        }
        return map.size();
    }
}
