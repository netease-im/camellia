package com.netease.nim.camellia.redis.proxy.upstream.kv.buffer;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.KvWriterBufferMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/5/22
 */
public class WriteBuffer<T> {

    private static final Logger logger = LoggerFactory.getLogger(WriteBuffer.class);

    private final String namespace;
    private final String name;
    private final boolean enable;
    private final ConcurrentHashMap<BytesKey, Deque<WriteBufferValue<T>>> writeBuffer = new ConcurrentHashMap<>();
    private final LongAdder writeBufferSize = new LongAdder();
    private int maxWriteBufferSize;

    private WriteBuffer(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
        this.enable = RedisKvConf.getBoolean(namespace, "kv.write.buffer." + name + ".enable", true);
        this.rebuild();
        ProxyDynamicConf.registerCallback(this::rebuild);
    }

    public static <T> WriteBuffer<T> newWriteBuffer(String namespace, String name) {
        WriteBuffer<T> writeBuffer = new WriteBuffer<>(namespace, name);
        KvWriterBufferMonitor.register(namespace, name, writeBuffer);
        return writeBuffer;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public long pending() {
        return writeBufferSize.sum();
    }

    private void rebuild() {
        int maxWriteBufferSize = RedisKvConf.getInt(namespace, "kv.write.buffer." + name + ".max.size", 100000);
        if (this.maxWriteBufferSize != maxWriteBufferSize) {
            logger.info("kv.write.buffer.{}.max.size = {}", name, maxWriteBufferSize);
            this.maxWriteBufferSize = maxWriteBufferSize;
        }
    }

    public WriteBufferValue<T> get(byte[] key) {
        if (!enable) {
            return null;
        }
        BytesKey bytesKey = new BytesKey(key);
        ReentrantLock lock = BufferLock.getLock(key);
        lock.lock();
        try {
            Deque<WriteBufferValue<T>> writeBufferValues = writeBuffer.get(bytesKey);
            if (writeBufferValues != null) {
                WriteBufferValue<T> value = writeBufferValues.peekLast();
                if (value != null) {
                    KvWriterBufferMonitor.writeBufferCacheHit(namespace, name);
                    return value;
                } else {
                    writeBuffer.remove(bytesKey);
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public Result put(byte[] key, T value) {
        if (!enable) {
            return NoOpResult.INSTANCE;
        }
        BytesKey bytesKey = new BytesKey(key);
        ReentrantLock lock = BufferLock.getLock(key);
        lock.lock();
        try {
            Deque<WriteBufferValue<T>> deque = checkDeque(bytesKey);
            if (deque == null) {
                KvWriterBufferMonitor.syncWrite(namespace, name);
                return NoOpResult.INSTANCE;
            }
            KvWriterBufferMonitor.asyncWrite(namespace, name);
            deque.add(new WriteBufferValue<>(value));
            writeBufferSize.increment();
            return new KvWriteDelayResult(() -> {
                writeBufferSize.decrement();
                lock.lock();
                try {
                    deque.poll();
                    if (deque.isEmpty()) {
                        writeBuffer.remove(bytesKey);
                    }
                } finally {
                    lock.unlock();
                    KvWriterBufferMonitor.asyncWriteDone(namespace, name);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    private Deque<WriteBufferValue<T>> checkDeque(BytesKey bytesKey) {
        Deque<WriteBufferValue<T>> deque = writeBuffer.get(bytesKey);
        if (writeBufferSize.sum() > maxWriteBufferSize) {
            if (deque == null) {
                return null;
            }
            if (deque.isEmpty()) {
                writeBuffer.remove(bytesKey);
                return null;
            }
            return deque;
        }
        if (deque == null) {
            deque = new ArrayDeque<>();
        }
        writeBuffer.put(bytesKey, deque);
        return deque;
    }

}
