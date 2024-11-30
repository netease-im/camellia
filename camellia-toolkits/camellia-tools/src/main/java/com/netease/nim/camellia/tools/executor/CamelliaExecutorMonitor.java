package com.netease.nim.camellia.tools.executor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/1/4
 */
public class CamelliaExecutorMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaExecutorMonitor.class);

    private static final ConcurrentHashMap<String, CamelliaDynamicExecutor> dynamicExecutorMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CamelliaDynamicIsolationExecutor> dynamicIsolationExecutorMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CamelliaHashedExecutor> hashedExecutorMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CamelliaLinearInitializationExecutor<?, ?>> linerInitializationExecutorMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Boolean> nameMap = new ConcurrentHashMap<>();

    public static void init(int intervalSeconds) {
        Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("camellia-executor-monitor"))
                .scheduleAtFixedRate(CamelliaExecutorMonitor::calc, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取一个唯一的name
     */
    public static String genExecutorName(String name) {
        synchronized (nameMap) {
            if (!nameMap.containsKey(name)) {
                nameMap.put(name, true);
                return name;
            }
            String originalName = name;
            int i=1;
            while (true) {
                name = originalName + "-" + i;
                if (!nameMap.containsKey(name)) {
                    nameMap.put(name, true);
                    return name;
                }
                i++;
            }
        }
    }

    /**
     * 注册一个ThreadPoolExecutor
     */
    public static void register(String name, ThreadPoolExecutor executor) {
        executorMap.put(genExecutorName(name), executor);
    }

    /**
     * 注册一个CamelliaDynamicExecutor
     */
    public static void register(CamelliaDynamicExecutor executor) {
        dynamicExecutorMap.put(executor.getName(), executor);
    }

    /**
     * 注册一个CamelliaDynamicIsolationExecutor
     */
    public static void register(CamelliaDynamicIsolationExecutor executor) {
        dynamicIsolationExecutorMap.put(executor.getName(), executor);
    }

    /**
     * 注册一个CamelliaHashedExecutor
     */
    public static void register(CamelliaHashedExecutor executor) {
        hashedExecutorMap.put(executor.getName(), executor);
    }

    /**
     * 注册一个CamelliaLinearInitializationExecutor
     */
    public static void register(CamelliaLinearInitializationExecutor<?, ?> executor) {
        linerInitializationExecutorMap.put(executor.getName(), executor);
    }

    private static CamelliaExecutorStatistics statistics = new CamelliaExecutorStatistics();

    /**
     * 获取统计数据
     */
    public static CamelliaExecutorStatistics getStatistics() {
        return statistics;
    }

    /**
     * 获取统计数据的json
     */
    public static JSONObject getStatisticsJson() {
        JSONObject json = new JSONObject();
        JSONArray json1 = new JSONArray();
        List<CamelliaExecutorStatistics.ExecutorStats> executorStatsList = statistics.getExecutorStatsList();
        for (CamelliaExecutorStatistics.ExecutorStats stats : executorStatsList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", stats.getName());
            jsonObject.put("type", stats.getExecutorType());
            jsonObject.putAll(toJson(stats.getStats()));
            json1.add(jsonObject);
        }
        json.put("executorStats", json1);

        JSONArray json2 = new JSONArray();
        List<CamelliaExecutorStatistics.DynamicIsolationExecutorStats> dynamicIsolationExecutorStatsList = statistics.getDynamicIsolationExecutorStatsList();
        for (CamelliaExecutorStatistics.DynamicIsolationExecutorStats stats : dynamicIsolationExecutorStatsList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", stats.getName());
            CamelliaExecutorStatistics.Stats fastExecutorStats = stats.getFastExecutorStats();
            jsonObject.put("fastThread", fastExecutorStats.getThread());
            jsonObject.put("fastActiveThread", fastExecutorStats.getActiveThread());
            jsonObject.put("fastPendingTask", fastExecutorStats.getPendingTask());
            jsonObject.put("fastTaskCount", fastExecutorStats.getTaskCount());
            CamelliaExecutorStatistics.Stats fastBackupExecutorStats = stats.getFastBackupExecutorStats();
            jsonObject.put("fastBackupThread", fastBackupExecutorStats.getThread());
            jsonObject.put("fastBackupActiveThread", fastBackupExecutorStats.getActiveThread());
            jsonObject.put("fastBackupPendingTask", fastBackupExecutorStats.getPendingTask());
            jsonObject.put("fastBackupTaskCount", fastBackupExecutorStats.getTaskCount());
            CamelliaExecutorStatistics.Stats slowExecutorStats = stats.getSlowExecutorStats();
            jsonObject.put("slowThread", slowExecutorStats.getThread());
            jsonObject.put("slowActiveThread", slowExecutorStats.getActiveThread());
            jsonObject.put("slowPendingTask", slowExecutorStats.getPendingTask());
            jsonObject.put("slowTaskCount", slowExecutorStats.getTaskCount());
            CamelliaExecutorStatistics.Stats slowBackupExecutorStats = stats.getSlowBackupExecutorStats();
            jsonObject.put("slowBackupThread", slowBackupExecutorStats.getThread());
            jsonObject.put("slowBackupActiveThread", slowBackupExecutorStats.getActiveThread());
            jsonObject.put("slowBackupPendingTask", slowBackupExecutorStats.getPendingTask());
            jsonObject.put("slowBackupTaskCount", slowBackupExecutorStats.getTaskCount());
            CamelliaExecutorStatistics.Stats whiteListExecutorStats = stats.getWhiteListExecutorStats();
            jsonObject.put("whiteListThread", whiteListExecutorStats.getThread());
            jsonObject.put("whiteListActiveThread", whiteListExecutorStats.getActiveThread());
            jsonObject.put("whiteListPendingTask", whiteListExecutorStats.getPendingTask());
            jsonObject.put("whiteListTaskCount", whiteListExecutorStats.getTaskCount());
            CamelliaExecutorStatistics.Stats isolationExecutorStats = stats.getIsolationExecutorStats();
            jsonObject.put("isolationThread", isolationExecutorStats.getThread());
            jsonObject.put("isolationActiveThread", isolationExecutorStats.getActiveThread());
            jsonObject.put("isolationPendingTask", isolationExecutorStats.getPendingTask());
            jsonObject.put("isolationTaskCount", isolationExecutorStats.getTaskCount());
            json2.add(jsonObject);
        }
        json.put("dynamicIsolationExecutorStats", json2);
        return json;
    }

    private static JSONObject toJson(CamelliaExecutorStatistics.Stats stats) {
        JSONObject json = new JSONObject();
        json.put("thread", stats.getThread());
        json.put("activeThread", stats.getActiveThread());
        json.put("pendingTask", stats.getPendingTask());
        json.put("taskCount", stats.getTaskCount());
        return json;
    }

    private static final ConcurrentHashMap<String, Long> lastCompletedTaskCountMap = new ConcurrentHashMap<>();

    private static void calc() {
        try {
            List<CamelliaExecutorStatistics.ExecutorStats> executorStatsList = new ArrayList<>();
            for (Map.Entry<String, ThreadPoolExecutor> entry : executorMap.entrySet()) {
                CamelliaExecutorStatistics.ExecutorStats stats = new CamelliaExecutorStatistics.ExecutorStats();
                stats.setName(entry.getKey());
                stats.setExecutorType(CamelliaExecutorStatistics.ExecutorType.ThreadPoolExecutor);
                stats.setStats(toStats(entry.getKey(), entry.getValue()));
                executorStatsList.add(stats);
            }

            for (Map.Entry<String, CamelliaDynamicExecutor> entry : dynamicExecutorMap.entrySet()) {
                CamelliaExecutorStatistics.ExecutorStats stats = new CamelliaExecutorStatistics.ExecutorStats();
                stats.setName(entry.getKey());
                stats.setExecutorType(CamelliaExecutorStatistics.ExecutorType.CamelliaDynamicExecutor);
                stats.setStats(toStats(entry.getKey(), entry.getValue().getStats()));
                executorStatsList.add(stats);
            }

            for (Map.Entry<String, CamelliaHashedExecutor> entry : hashedExecutorMap.entrySet()) {
                CamelliaExecutorStatistics.ExecutorStats stats = new CamelliaExecutorStatistics.ExecutorStats();
                stats.setName(entry.getKey());
                stats.setExecutorType(CamelliaExecutorStatistics.ExecutorType.CamelliaHashedExecutor);
                stats.setStats(toStats(entry.getKey(), entry.getValue().getStats()));
                executorStatsList.add(stats);
            }

            for (Map.Entry<String, CamelliaLinearInitializationExecutor<?, ?>> entry : linerInitializationExecutorMap.entrySet()) {
                CamelliaExecutorStatistics.ExecutorStats stats = new CamelliaExecutorStatistics.ExecutorStats();
                stats.setName(entry.getKey());
                stats.setExecutorType(CamelliaExecutorStatistics.ExecutorType.CamelliaLinearInitializationExecutor);
                stats.setStats(toStats(entry.getKey(), entry.getValue().getStats()));
                executorStatsList.add(stats);
            }

            for (Map.Entry<String, CamelliaDynamicIsolationExecutor> entry : dynamicIsolationExecutorMap.entrySet()) {
                CamelliaExecutorStatistics.ExecutorStats stats = new CamelliaExecutorStatistics.ExecutorStats();
                stats.setName(entry.getKey());
                stats.setExecutorType(CamelliaExecutorStatistics.ExecutorType.CamelliaDynamicIsolationExecutor);
                stats.setStats(toStats(entry.getKey(), entry.getValue().getStats()));
                executorStatsList.add(stats);
            }

            List<CamelliaExecutorStatistics.DynamicIsolationExecutorStats> dynamicIsolationExecutorStatsList = new ArrayList<>();
            for (Map.Entry<String, CamelliaDynamicIsolationExecutor> entry : dynamicIsolationExecutorMap.entrySet()) {
                CamelliaExecutorStatistics.DynamicIsolationExecutorStats stats = new CamelliaExecutorStatistics.DynamicIsolationExecutorStats();
                stats.setName(entry.getKey());
                stats.setFastExecutorStats(toStats(entry.getKey() + "-fast", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.FAST)));
                stats.setFastBackupExecutorStats(toStats(entry.getKey() + "-fast-backup", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.FAST_BACKUP)));
                stats.setSlowExecutorStats(toStats(entry.getKey() + "-slow", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.SLOW)));
                stats.setSlowBackupExecutorStats(toStats(entry.getKey() + "-slow-backup", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.SLOW_BACKUP)));
                stats.setWhiteListExecutorStats(toStats(entry.getKey() + "-white-list", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.WHITE_LIST)));
                stats.setIsolationExecutorStats(toStats(entry.getKey() + "-isolation", entry.getValue().getExecutorStats(CamelliaDynamicIsolationExecutor.Type.ISOLATION)));
                dynamicIsolationExecutorStatsList.add(stats);
            }

            CamelliaExecutorStatistics statistics = new CamelliaExecutorStatistics();
            statistics.setExecutorStatsList(executorStatsList);
            statistics.setDynamicIsolationExecutorStatsList(dynamicIsolationExecutorStatsList);
            CamelliaExecutorMonitor.statistics = statistics;
        } catch (Exception e) {
            logger.error("CamelliaExecutorMonitor calc error", e);
        }
    }

    private static CamelliaExecutorStatistics.Stats toStats(String name, ThreadPoolExecutor executor) {
        CamelliaExecutorStatistics.Stats stats = new CamelliaExecutorStatistics.Stats();
        stats.setActiveThread(executor.getActiveCount());
        stats.setThread(executor.getPoolSize());
        stats.setPendingTask(executor.getQueue().size());

        long completedTaskCount = executor.getCompletedTaskCount();
        Long lastCompletedTaskCount = lastCompletedTaskCountMap.get(name);
        if (lastCompletedTaskCount == null) {
            stats.setTaskCount(completedTaskCount);
        } else {
            stats.setTaskCount(completedTaskCount - lastCompletedTaskCount);
        }
        lastCompletedTaskCountMap.put(name, completedTaskCount);
        return stats;
    }

    private static CamelliaExecutorStatistics.Stats toStats(String name, CamelliaExecutorStats stats) {
        CamelliaExecutorStatistics.Stats stats2 = new CamelliaExecutorStatistics.Stats();
        stats2.setActiveThread(stats.getActiveThread());
        stats2.setThread(stats.getThread());
        stats2.setPendingTask(stats.getPendingTask());

        Long lastCompletedTaskCount = lastCompletedTaskCountMap.get(name);
        if (lastCompletedTaskCount == null) {
            stats2.setTaskCount(stats.getCompletedTaskCount());
        } else {
            stats2.setTaskCount(stats.getCompletedTaskCount() - lastCompletedTaskCount);
        }
        lastCompletedTaskCountMap.put(name, stats.getCompletedTaskCount());
        return stats2;
    }
}
