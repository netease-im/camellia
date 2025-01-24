package com.netease.nim.camellia.redis.proxy.monitor;

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

    private static final Time compactTime = new Time();
    private static final Time flushTime = new Time();
    private static final Time keyFlushTime = new Time();
    private static final Time valueFlushTime = new Time();
    private static final Time walFlushTime = new Time();
    private static final Time walAppendTime = new Time();
    private static final Time valueWaitFlushTime = new Time();
    private static final Time keyWaitFlushTime = new Time();

    private static final Time fileReadTime = new Time();
    private static final Time fileWriteTime = new Time();

    private static final ConcurrentHashMap<String, LongAdder> fileReadMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> fileWriteMap = new ConcurrentHashMap<>();

    private static final LongAdder slotInit= new LongAdder();
    private static final LongAdder slotExpand = new LongAdder();

    public static void compactTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        compactTime.update(time);
    }

    public static void flushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        flushTime.update(time);
    }

    public static void keyFlushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        keyFlushTime.update(time);
    }

    public static void valueFlushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        valueFlushTime.update(time);
    }

    public static void walFlushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        walFlushTime.update(time);
    }

    public static void walAppendTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        walAppendTime.update(time);
    }

    public static void valueWaitFlushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        valueWaitFlushTime.update(time);
    }

    public static void keyWaitFlushTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        keyWaitFlushTime.update(time);
    }

    public static void fileReadTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        fileReadTime.update(time);
    }

    public static void fileWriteTime(long time) {
        if (time < 0) return;
        time = time / 10000;
        fileWriteTime.update(time);
    }

    public static void fileRead(String file, long size) {
        CamelliaMapUtils.computeIfAbsent(fileReadMap, file, k -> new LongAdder()).add(size);
    }

    public static void fileWrite(String file, long size) {
        CamelliaMapUtils.computeIfAbsent(fileWriteMap, file, k -> new LongAdder()).add(size);
    }

    public static void slotInit() {
        slotInit.increment();
    }

    public static void slotExpand() {
        slotExpand.increment();
    }

    static {
        ExecutorUtils.scheduleAtFixedRate(() -> {
            {
                Stats stats = compactTime.getStats();
                logger.info("compact stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = flushTime.getStats();
                logger.info("flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = keyFlushTime.getStats();
                logger.info("key flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = valueFlushTime.getStats();
                logger.info("value flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = walFlushTime.getStats();
                logger.info("wal flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = walAppendTime.getStats();
                logger.info("wal append stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = keyWaitFlushTime.getStats();
                logger.info("key wait flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = valueWaitFlushTime.getStats();
                logger.info("value wait flush stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = fileReadTime.getStats();
                logger.info("file read stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                Stats stats = fileWriteTime.getStats();
                logger.info("file write stats, count = {}, avg = {}", stats.count, stats.avg);
            }
            {
                for (Map.Entry<String, LongAdder> entry : fileWriteMap.entrySet()) {
                    long size = entry.getValue().sumThenReset();
                    if (size == 0) {
                        continue;
                    }
                    logger.info("file write, file = {}, size = {}", entry.getKey(), Utils.humanReadableByteCountBin(size));
                }
            }
            {
                for (Map.Entry<String, LongAdder> entry : fileReadMap.entrySet()) {
                    long size = entry.getValue().sumThenReset();
                    if (size == 0) {
                        continue;
                    }
                    logger.info("file read, file = {}, size = {}", entry.getKey(), Utils.humanReadableByteCountBin(size));
                }
            }
            {
                logger.info("slot init, count = {}", slotInit.sumThenReset());
                logger.info("slot expand, count = {}", slotExpand.sumThenReset());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private static class Time {
        LongAdder time = new LongAdder();
        LongAdder count = new LongAdder();

        void update(long time) {
            this.time.add(time);
            this.count.increment();
        }

        Stats getStats() {
            long time = this.time.sumThenReset();
            long count = this.count.sumThenReset();
            if (count == 0) {
                return new Stats();
            }
            Stats stats = new Stats();
            stats.avg = ((double) time / count) / 100;
            stats.count = count;
            return stats;
        }
    }

    private static class Stats {
        long count;
        double avg;
    }
}
