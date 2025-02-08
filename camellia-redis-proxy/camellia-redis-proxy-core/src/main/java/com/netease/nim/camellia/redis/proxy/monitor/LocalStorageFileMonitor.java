package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageFileStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.TimeStats;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageFileMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageFileMonitor.class);

    private static ConcurrentHashMap<String, LongAdder> readSizeMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, LongAdder> writeSizeMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, TimeCollector> readTimeMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, TimeCollector> writeTimeMap = new ConcurrentHashMap<>();

    public static void fileRead(String file, long size, long time) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        if (time < 0) return;
        CamelliaMapUtils.computeIfAbsent(readSizeMap, file, k -> new LongAdder()).add(size);
        CamelliaMapUtils.computeIfAbsent(readTimeMap, file, k -> new TimeCollector()).update(time);
    }

    public static void fileWrite(String file, long size, long time) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        if (time < 0) return;
        CamelliaMapUtils.computeIfAbsent(writeSizeMap, file, k -> new LongAdder()).add(size);
        CamelliaMapUtils.computeIfAbsent(writeTimeMap, file, k -> new TimeCollector()).update(time);
    }

    public static List<LocalStorageFileStats> collect() {
        try {
            ConcurrentHashMap<String, LongAdder> readSizeMap = LocalStorageFileMonitor.readSizeMap;
            ConcurrentHashMap<String, LongAdder> writeSizeMap = LocalStorageFileMonitor.writeSizeMap;
            ConcurrentHashMap<String, TimeCollector> readTimeMap = LocalStorageFileMonitor.readTimeMap;
            ConcurrentHashMap<String, TimeCollector> writeTimeMap = LocalStorageFileMonitor.writeTimeMap;

            LocalStorageFileMonitor.readSizeMap = new ConcurrentHashMap<>();
            LocalStorageFileMonitor.writeSizeMap = new ConcurrentHashMap<>();
            LocalStorageFileMonitor.readTimeMap = new ConcurrentHashMap<>();
            LocalStorageFileMonitor.writeTimeMap = new ConcurrentHashMap<>();

            Set<String> fileSet = new HashSet<>();
            fileSet.addAll(readSizeMap.keySet());
            fileSet.addAll(writeSizeMap.keySet());
            fileSet.addAll(readTimeMap.keySet());
            fileSet.addAll(writeTimeMap.keySet());

            List<LocalStorageFileStats> statsList = new ArrayList<>();

            for (String file : fileSet) {
                LocalStorageFileStats stats = new LocalStorageFileStats();
                stats.setFile(file);
                LongAdder readSize = readSizeMap.get(file);
                if (readSize != null) {
                    stats.setReadSize(readSize.sumThenReset());
                }
                LongAdder writeSize = writeSizeMap.get(file);
                if (writeSize != null) {
                    stats.setWriteSize(writeSize.sumThenReset());
                }
                TimeStats readTime;
                TimeCollector readTimeCollector = readTimeMap.get(file);
                if (readTimeCollector != null) {
                    readTime = readTimeCollector.getStats();
                } else {
                    readTime = new TimeStats();
                }
                stats.setReadTime(readTime);
                TimeStats writeTime;
                TimeCollector writeTimeCollector = writeTimeMap.get(file);
                if (writeTimeCollector != null) {
                    writeTime = writeTimeCollector.getStats();
                } else {
                    writeTime = new TimeStats();
                }
                stats.setWriteTime(writeTime);
                statsList.add(stats);
            }
            return statsList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
