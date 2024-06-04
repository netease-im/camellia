package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.KvWriteBufferStats;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBuffer;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2024/5/22
 */
public class KvWriterBufferMonitor {

    private static final Logger logger = LoggerFactory.getLogger(KvWriterBufferMonitor.class);

    private static final ConcurrentHashMap<String, LongAdder> cache = new ConcurrentHashMap<>();//命中缓存
    private static final ConcurrentHashMap<String, LongAdder> overflow = new ConcurrentHashMap<>();//buffer溢出
    private static final ConcurrentHashMap<String, LongAdder> start = new ConcurrentHashMap<>();//开始一个任务
    private static final ConcurrentHashMap<String, LongAdder> done = new ConcurrentHashMap<>();//任务完成

    private static final ConcurrentHashMap<String, WriteBuffer<?>> writeBufferMap = new ConcurrentHashMap<>();

    public static void register(String namespace, String type, WriteBuffer<?> writeBuffer) {
        writeBufferMap.put(namespace + "|" + type, writeBuffer);
    }

    public static void cache(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(cache, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void overflow(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(overflow, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void start(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(start, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void done(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(done, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static List<KvWriteBufferStats> collect() {
        try {
            Set<String> set = new HashSet<>();
            set.addAll(cache.keySet());
            set.addAll(overflow.keySet());
            set.addAll(start.keySet());
            set.addAll(done.keySet());
            set.addAll(writeBufferMap.keySet());
            List<KvWriteBufferStats> list = new ArrayList<>();
            for (String string : set) {
                int i = string.lastIndexOf("|");
                String namespace = string.substring(0, i);
                String type = string.substring(i+1);
                LongAdder c1 = cache.get(string);
                LongAdder c2 = overflow.get(string);
                LongAdder c3 = start.get(string);
                LongAdder c4 = done.get(string);
                WriteBuffer<?> writeBuffer = writeBufferMap.get(string);
                KvWriteBufferStats stats = new KvWriteBufferStats();
                stats.setNamespace(namespace);
                stats.setType(type);
                stats.setCache(c1 == null ? 0 : c1.sumThenReset());
                stats.setOverflow(c2 == null ? 0 : c2.sumThenReset());
                stats.setStart(c3 == null ? 0 : c3.sumThenReset());
                stats.setDone(c4 == null ? 0 : c4.sumThenReset());
                stats.setPending(writeBuffer == null ? 0 : writeBuffer.pending());
                list.add(stats);
            }
            return list;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

}
