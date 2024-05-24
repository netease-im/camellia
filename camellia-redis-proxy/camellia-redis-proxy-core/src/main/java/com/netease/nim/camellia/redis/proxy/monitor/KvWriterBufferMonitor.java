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

    private static final ConcurrentHashMap<String, LongAdder> writeBufferCacheHit = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> syncWrite = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> asyncWrite = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> asyncWriteDone = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, WriteBuffer<?>> writeBufferMap = new ConcurrentHashMap<>();

    public static void register(String namespace, String type, WriteBuffer<?> writeBuffer) {
        writeBufferMap.put(namespace + "|" + type, writeBuffer);
    }

    public static void writeBufferCacheHit(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(writeBufferCacheHit, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void syncWrite(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(syncWrite, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void asyncWrite(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(asyncWrite, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static void asyncWriteDone(String namespace, String type) {
        CamelliaMapUtils.computeIfAbsent(asyncWriteDone, namespace + "|" + type, k -> new LongAdder()).increment();
    }

    public static List<KvWriteBufferStats> collect() {
        try {
            Set<String> set = new HashSet<>();
            set.addAll(writeBufferCacheHit.keySet());
            set.addAll(syncWrite.keySet());
            set.addAll(asyncWrite.keySet());
            set.addAll(asyncWriteDone.keySet());
            set.addAll(writeBufferMap.keySet());
            List<KvWriteBufferStats> list = new ArrayList<>();
            for (String string : set) {
                int i = string.lastIndexOf("|");
                String namespace = string.substring(0, i);
                String type = string.substring(i+1);
                LongAdder c1 = writeBufferCacheHit.get(string);
                LongAdder c2 = syncWrite.get(string);
                LongAdder c3 = asyncWrite.get(string);
                LongAdder c4 = asyncWriteDone.get(string);
                WriteBuffer<?> writeBuffer = writeBufferMap.get(string);
                KvWriteBufferStats stats = new KvWriteBufferStats();
                stats.setNamespace(namespace);
                stats.setType(type);
                stats.setWriteBufferCacheHit(c1 == null ? 0 : c1.sumThenReset());
                stats.setSyncWrite(c2 == null ? 0 : c2.sumThenReset());
                stats.setAsyncWrite(c3 == null ? 0 : c3.sumThenReset());
                stats.setAsyncWriteDone(c4 == null ? 0 : c4.sumThenReset());
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
