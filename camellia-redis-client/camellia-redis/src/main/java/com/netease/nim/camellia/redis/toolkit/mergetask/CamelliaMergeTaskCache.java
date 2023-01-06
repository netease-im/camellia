package com.netease.nim.camellia.redis.toolkit.mergetask;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskCache {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMergeTaskCache.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>>> taskCache;
    private final Function<String, ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>>> mappingFunction;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(new CamelliaThreadFactory("camellia-merge-task-cache"));

    public CamelliaMergeTaskCache() {
        taskCache = new ConcurrentHashMap<>();
        mappingFunction = k -> new ConcurrentHashMap<>();
    }

    private ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>> getTaskMap(String taskKey) {
        return CamelliaMapUtils.computeIfAbsent(taskCache, taskKey, mappingFunction);
    }

    public void addTask(String taskKey, String taskId, CamelliaMergeTaskFuture<?> future) {
        exec.submit(() -> {
            try {
                ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>> map = getTaskMap(taskKey);
                map.put(taskId, future);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    public void removeTask(String taskKey, String taskId) {
        exec.submit(() -> {
            try {
                ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>> map = taskCache.get(taskKey);
                if (map != null) {
                    map.remove(taskId);
                    if (map.isEmpty()) {
                        taskCache.remove(taskKey);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    public int size(String taskKey) {
        ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>> map = taskCache.get(taskKey);
        if (map == null) return 0;
        return map.size();
    }

    public Map<String, CamelliaMergeTaskFuture<?>> getFutureMap(String taskKey) {
        ConcurrentHashMap<String, CamelliaMergeTaskFuture<?>> map = taskCache.get(taskKey);
        if (map != null) {
            return new HashMap<>(map);
        }
        return null;
    }
}
