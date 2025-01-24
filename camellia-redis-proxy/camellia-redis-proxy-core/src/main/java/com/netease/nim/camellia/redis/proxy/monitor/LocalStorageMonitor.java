package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageExecutors;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2025/1/23
 */
public class LocalStorageMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageMonitor.class);

    private static final ConcurrentHashMap<String, FileReadWriteCollector> fileMap = new ConcurrentHashMap<>();

    private static final LongAdder slotInit = new LongAdder();
    private static final LongAdder slotExpand = new LongAdder();

    private static final ConcurrentHashMap<String, TimeCollector> timeMap = new ConcurrentHashMap<>();

    public static void fileRead(String file, long size, long time) {
        if (time < 0) return;
        time = time / 10000;
        CamelliaMapUtils.computeIfAbsent(fileMap, file, k -> new FileReadWriteCollector()).updateRead(size, time);
    }

    public static void fileWrite(String file, long size, long time) {
        if (time < 0) return;
        time = time / 10000;
        CamelliaMapUtils.computeIfAbsent(fileMap, file, k -> new FileReadWriteCollector()).updateWrite(size, time);
    }

    public static void slotInit() {
        slotInit.increment();
    }

    public static void slotExpand() {
        slotExpand.increment();
    }

    public static void time(String item, long time) {
        if (time < 0) return;
        time = time / 10000;
        CamelliaMapUtils.computeIfAbsent(timeMap, item, k -> new TimeCollector()).update(time);
    }

    static {
        ExecutorUtils.scheduleAtFixedRate(() -> {
            logger.info("##############");
            {
                logger.info("slot init, count = {}", slotInit.sumThenReset());
                logger.info("slot expand, count = {}", slotExpand.sumThenReset());
                logger.info("flush executor, queue.size = {}", LocalStorageExecutors.getInstance().getFlushExecutor().size());
            }
            {
                for (Map.Entry<String, TimeCollector> entry : timeMap.entrySet()) {
                    TimeCollector time = entry.getValue();
                    TimeStats stats = time.getStats();
                    if (stats.count == 0) {
                        continue;
                    }
                    logger.info("time monitor, item = {}, count = {}, avg = {}", entry.getKey(), stats.count, stats.avg);
                }
            }
            for (Map.Entry<String, FileReadWriteCollector> entry : fileMap.entrySet()) {
                FileReadWriteCollector collector = entry.getValue();
                FileReadWriteStats stats = collector.getStats();
                if (stats.readSize == 0 && stats.writeSize == 0) {
                    continue;
                }
                logger.info("file read, file = {}, read.count = {}, read.size = {}, read.time.avg = {}",
                        entry.getKey(), stats.readTime.count, Utils.humanReadableByteCountBin(stats.readSize), stats.readTime.avg);
                logger.info("file write, file = {}, write.count = {}, write.size = {}, write.time.avg = {}",
                        entry.getKey(), stats.writeTime.count, Utils.humanReadableByteCountBin(stats.writeSize), stats.writeTime.avg);
            }
            logger.info("##############");
        }, 10, 10, TimeUnit.SECONDS);
    }

    private static class TimeCollector {
        LongAdder time = new LongAdder();
        LongAdder count = new LongAdder();

        void update(long time) {
            this.time.add(time);
            this.count.increment();
        }

        TimeStats getStats() {
            long time = this.time.sumThenReset();
            long count = this.count.sumThenReset();
            if (count == 0) {
                return new TimeStats();
            }
            TimeStats stats = new TimeStats();
            stats.avg = ((double) time / count) / 100;
            stats.count = count;
            return stats;
        }
    }

    private static class FileReadWriteCollector {
        TimeCollector readTime = new TimeCollector();
        LongAdder readSize = new LongAdder();

        TimeCollector writeTime = new TimeCollector();
        LongAdder writeSize = new LongAdder();

        void updateRead(long size, long time) {
            readTime.update(time);
            readSize.add(size);
        }

        void updateWrite(long size, long time) {
            writeTime.update(time);
            writeSize.add(size);
        }

        FileReadWriteStats getStats() {
            FileReadWriteStats stats = new FileReadWriteStats();
            stats.readTime = readTime.getStats();
            stats.writeTime = writeTime.getStats();
            stats.readSize = readSize.sumThenReset();
            stats.writeSize = writeSize.sumThenReset();
            return stats;
        }
    }

    private static class TimeStats {
        long count;
        double avg;
    }

    private static class FileReadWriteStats {
        TimeStats readTime;
        long readSize;
        TimeStats writeTime;
        long writeSize;
    }
}
