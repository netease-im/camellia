package com.netease.nim.camellia.redis.proxy.util;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/11/19
 */
public class LRUCounter {

    private final ConcurrentLinkedHashMap<BytesKey, Counter> cache;
    private final long expireMillis;

    public LRUCounter(int initialCapacity, long capacity, long expireMillis) {
        this.cache = new ConcurrentLinkedHashMap.Builder<BytesKey, Counter>().initialCapacity(initialCapacity)
                .maximumWeightedCapacity(capacity).build();
        this.expireMillis = expireMillis;
    }

    public long incrementAndGet(BytesKey bytesKey) {
        Counter counter = cache.get(bytesKey);
        if (counter != null) {
            if (TimeCache.currentMillis - counter.timestamp > expireMillis) {
                counter.reset();
            }
        }
        if (counter == null) {
            counter = new Counter();
            Counter old = cache.putIfAbsent(bytesKey, counter);
            if (old != null) {
                counter = old;
            }
        }
        return counter.count.incrementAndGet();
    }

    public Long get(BytesKey bytesKey) {
        Counter counter = cache.get(bytesKey);
        if (counter != null) {
            if (TimeCache.currentMillis - counter.timestamp > expireMillis) {
                cache.remove(bytesKey);
                return null;
            }
            return counter.count.get();
        }
        return null;
    }

    public TreeSet<SortedBytesKey> getSortedCacheValue(long threshold) {
        if (cache.isEmpty()) return null;
        TreeSet<SortedBytesKey> treeSet = new TreeSet<>();
        for (Map.Entry<BytesKey, Counter> entry : cache.entrySet()) {
            if (TimeCache.currentMillis - entry.getValue().timestamp > expireMillis) {
                cache.remove(entry.getKey());
                continue;
            }
            long count = entry.getValue().count.get();
            if (count >= threshold) {
                byte[] key = entry.getKey().getKey();
                treeSet.add(new SortedBytesKey(key, count));
            }
        }
        return treeSet;
    }

    public static class SortedBytesKey implements Comparable<SortedBytesKey> {
        private final byte[] key;
        private final long count;

        public SortedBytesKey(byte[] key, long count) {
            this.key = key;
            this.count = count;
        }

        public byte[] getKey() {
            return key;
        }

        public long getCount() {
            return count;
        }

        @Override
        public int compareTo(SortedBytesKey o) {
            return Long.compare(o.count, count);
        }
    }

    private static class Counter {
        private long timestamp = TimeCache.currentMillis;
        private final AtomicLong count = new AtomicLong();
        private final AtomicBoolean lock = new AtomicBoolean();

        void reset() {
            if (lock.compareAndSet(false, true)) {
                timestamp = TimeCache.currentMillis;
                count.set(0);
            }
        }
    }
}
