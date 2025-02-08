package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.CountStats;
import com.netease.nim.camellia.redis.proxy.monitor.model.LocalStorageKeyBucketStats;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.CompressType;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by caojiajun on 2025/2/8
 */
public class LocalStorageKeyBucketMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageKeyBucketMonitor.class);

    private static final ConcurrentHashMap<CompressType, CountCollector> keyCountCollector = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<CompressType, CountCollector> decompressSizeCollector = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<CompressType, CountCollector> compressSizeCollector = new ConcurrentHashMap<>();

    public static void update(CompressType compressType, int keyCount, int decompressSize, int compressSize) {
        if (!ProxyMonitorCollector.isMonitorEnable()) {
            return;
        }
        CamelliaMapUtils.computeIfAbsent(keyCountCollector, compressType, k -> new CountCollector()).update(keyCount);
        CamelliaMapUtils.computeIfAbsent(decompressSizeCollector, compressType, k -> new CountCollector()).update(decompressSize);
        CamelliaMapUtils.computeIfAbsent(compressSizeCollector, compressType, k -> new CountCollector()).update(compressSize);
    }

    public static List<LocalStorageKeyBucketStats> collect() {
        try {
            List<LocalStorageKeyBucketStats> list = new ArrayList<>();
            for (CompressType compressType : CompressType.values()) {
                LocalStorageKeyBucketStats stats = new LocalStorageKeyBucketStats();
                stats.setCompressType(compressType);
                CountCollector collector1 = keyCountCollector.get(compressType);
                CountCollector collector2 = decompressSizeCollector.get(compressType);
                CountCollector collector3 = compressSizeCollector.get(compressType);
                if (collector1 != null) {
                    CountStats stats1 = collector1.getStats();
                    stats.setCount(stats1.getCount());
                    stats.setKeyCountAvg(stats1.getAvg());
                    stats.setKeyCountMax(stats1.getMax());
                }
                if (collector2 != null) {
                    CountStats stats2 = collector2.getStats();
                    stats.setCount(stats2.getCount());
                    stats.setDecompressSizeAvg(stats2.getAvg());
                    stats.setDecompressSizeMax(stats2.getMax());
                }
                if (collector3 != null) {
                    CountStats stats3 = collector3.getStats();
                    stats.setCount(stats3.getCount());
                    stats.setCompressSizeAvg(stats3.getAvg());
                    stats.setCompressSizeMax(stats3.getMax());
                }
                list.add(stats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

}
