package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.LockMap;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 保证顺序的资源初始化线程池
 *
 * 只保证同线程内的任务提交下，先提交的肯定先complete，后提交的后complete
 * 初始化只做一次，如果失败了会把队列中的任务先回调，然后再触发下一次
 * 如果初始化失败了，complete一个null，而不会回调exception
 *
 * Created by caojiajun on 2023/1/31
 */
public class CamelliaLinearInitializationExecutor<K, T> implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaLinearInitializationExecutor.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("camellia-liner-initialization-schedule"));

    private final String name;
    private final CamelliaLinearInitializationExecutorConfig<K, T> config;
    private final ConcurrentHashMap<String, AtomicBoolean> tag = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>>> totalFutureQueueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FastThreadLocal<DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>>>> futureQueueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> willClearKeys = new ConcurrentHashMap<>();
    private final DynamicValueGetter<Integer> pendingQueueSize;

    private final LockMap lockMap = new LockMap();

    private final Initializer<K, T> initializer;
    private final CamelliaDynamicExecutor executor;

    private final AtomicBoolean scheduleLock = new AtomicBoolean(false);

    public CamelliaLinearInitializationExecutor(String name, Initializer<K, T> initializer) {
        this(new CamelliaLinearInitializationExecutorConfig<>(name, initializer));
    }

    public CamelliaLinearInitializationExecutor(String name, Initializer<K, T> initializer, DynamicValueGetter<Integer> pendingQueueSize) {
        this(new CamelliaLinearInitializationExecutorConfig<>(name, initializer, pendingQueueSize));
    }

    public CamelliaLinearInitializationExecutor(CamelliaLinearInitializationExecutorConfig<K, T> config) {
        this.initializer = config.getInitializer();
        this.config = config;
        this.name = CamelliaExecutorMonitor.genExecutorName(config.getName());
        this.executor = config.getExecutor();
        this.pendingQueueSize = config.getPendingQueueSize();
        scheduler.scheduleAtFixedRate(this::schedule, config.getClearScheduleIntervalSeconds(), config.getClearScheduleIntervalSeconds(), TimeUnit.SECONDS);

        CamelliaExecutorMonitor.register(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public CamelliaExecutorStats getStats() {
        return executor.getStats();
    }

    /**
     * 获取资源或者初始化
     * @param key 资源key
     * @return 资源future
     */
    public CompletableFuture<T> getOrInitialize(K key) {
        String keyStr = key.toString();
        T value = cache.get(keyStr);
        if (value != null) {
            //初始化已经完成了，则先清空之前的future，再处理自己，确保有序
            clearFutureQueue(keyStr, value);
            return wrapper(value);
        }
        //走到这里，说明初始化没有完成
        CompletableFuture<T> future = new CompletableFuture<>();
        boolean offer = offer(keyStr, future);
        if (!offer) {
            //入队失败，则直接complete一个null，不应该出现，如果出现了，则不保证顺序
            future.complete(null);
            return future;
        }
        AtomicBoolean tag = getTag(keyStr);
        if (tag.compareAndSet(false, true)) {//控制初始化的并发
            try {
                //提交异步任务
                executor.submit(() -> {
                    T value1 = cache.get(keyStr);
                    boolean fail = false;
                    try {
                        if (value1 != null) {
                            clearTotalFutureQueue(keyStr, value1);
                            return;
                        }
                        value1 = initializer.initialize(key);
                        if (value1 != null) {
                            synchronized (lockMap.getLockObj(keyStr)) {
                                cache.put(keyStr, value1);
                                clearTotalFutureQueue(keyStr, value1);
                            }
                        } else {
                            fail = true;
                        }
                    } catch (Exception e) {
                        fail = true;
                    }
                    tag.compareAndSet(true, false);
                    if (fail) {
                        clearTotalFutureQueue(keyStr, null, getTotalFutureQueue(keyStr).size());
                    }
                });
            } catch (Exception e) {
                //无法提交初始化任务，则直接complete一个null
                clearFutureQueue(keyStr, null);
            }
        }
        return future;
    }

    /**
     * 删除资源
     * @param key 资源key
     */
    public void remove(K key) {
        cache.remove(key.toString());
    }

    /**
     * 添加资源
     * @param key 资源key
     * @param value 资源
     */
    public void put(K key, T value) {
        cache.put(key.toString(), value);
        clearTotalFutureQueue(key.toString(), value);
    }

    private boolean offer(String keyStr, CompletableFuture<T> future) {
        MyCompletableFuture<T> future1 = new MyCompletableFuture<>(future);
        boolean offer = getTotalFutureQueue(keyStr).offer(future1);
        if (offer) {
            getFutureQueue(keyStr).offer(future1);
        }
        return offer;
    }

    private void clearTotalFutureQueue(String keyStr, T value, int size) {
        synchronized (lockMap.getLockObj(keyStr)) {
            DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = getTotalFutureQueue(keyStr);
            clear(keyStr, queue, value, size);
        }
    }

    private void clearTotalFutureQueue(String keyStr, T value) {
        synchronized (lockMap.getLockObj(keyStr)) {
            DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = getTotalFutureQueue(keyStr);
            clear(keyStr, queue, value, -1);
        }
    }

    private void clearFutureQueue(String keyStr, T value) {
        synchronized (lockMap.getLockObj(keyStr)) {
            DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = getFutureQueue(keyStr);
            clear(keyStr, queue, value, -1);
        }
    }

    private void clear(String keyStr, DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue, T value, int size) {
        if (queue == null || queue.isEmpty()) return;
        int i=0;
        MyCompletableFuture<T> completableFuture = queue.poll();
        while (completableFuture != null) {
            try {
                completableFuture.complete(value);
                i ++;
            } catch (Exception e) {
                logger.error("complete error, key = {}", keyStr);
            }
            if (size > 0 && i >= size) {
                break;
            }
            completableFuture = queue.poll();
        }
    }

    //正在初始化过程中的时候进来的请求，进入这个队列
    private DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> getFutureQueue(String keyStr) {
        FastThreadLocal<DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>>> threadLocal = futureQueueMap.get(keyStr);
        if (threadLocal == null) {
            threadLocal = futureQueueMap.computeIfAbsent(keyStr, k -> new FastThreadLocal<>());
        }
        DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = threadLocal.get();
        if (queue == null) {
            queue = new DynamicCapacityLinkedBlockingQueue<>(pendingQueueSize);
            threadLocal.set(queue);
        }
        return queue;
    }

    private DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> getTotalFutureQueue(String keyStr) {
        DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = totalFutureQueueMap.get(keyStr);
        if (queue == null) {
            queue = totalFutureQueueMap.computeIfAbsent(keyStr, k -> new DynamicCapacityLinkedBlockingQueue<>(pendingQueueSize));
        }
        return queue;
    }

    //这个tag表示正在初始化过程中
    private AtomicBoolean getTag(String keyStr) {
        return CamelliaMapUtils.computeIfAbsent(tag, keyStr, k -> new AtomicBoolean(false));
    }

    private CompletableFuture<T> wrapper(T value) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(value);
        return future;
    }

    private void schedule() {
        if (scheduleLock.compareAndSet(false, true)) {
            try {
                if (!willClearKeys.isEmpty()) {
                    for (Map.Entry<String, Long> entry : willClearKeys.entrySet()) {
                        String key = entry.getKey();
                        Long time = entry.getValue();
                        T value = cache.get(key);
                        if (value != null && System.currentTimeMillis() - time > config.getClearExpireSeconds().get() * 1000L) {
                            DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = totalFutureQueueMap.get(key);
                            if (!queue.isEmpty()) {
                                clearTotalFutureQueue(key, value);
                            }
                        }
                    }
                    willClearKeys.clear();
                }
                for (String key : cache.keySet()) {
                    DynamicCapacityLinkedBlockingQueue<MyCompletableFuture<T>> queue = totalFutureQueueMap.get(key);
                    if (!queue.isEmpty()) {
                        willClearKeys.put(key, System.currentTimeMillis());
                    }
                }
            } finally {
                scheduleLock.compareAndSet(true, false);
            }
        }
    }

    public static interface Initializer<K, T> {
        T initialize(K key);
    }

    private static class MyCompletableFuture<T> {
        private final AtomicBoolean callback = new AtomicBoolean(false);

        private final CompletableFuture<T> future;

        public MyCompletableFuture(CompletableFuture<T> future) {
            this.future = future;
        }

        public boolean complete(T value) {
            if (callback.compareAndSet(false, true)) {
                return future.complete(value);
            }
            return false;
        }

        public boolean isComplete() {
            return callback.get();
        }
    }
}
