package com.netease.nim.camellia.tools.executor;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.utils.LockMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 保证顺序的资源初始化线程池
 *
 * 初始化只做一次，如果失败了会把队列中的任务先回调，然后再触发下一次
 * 如果初始化失败了，complete一个null，而不会回调exception
 *
 * Created by caojiajun on 2023/1/31
 */
public class CamelliaLinearInitializationExecutor<K, T> implements CamelliaExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaLinearInitializationExecutor.class);

    private final String name;
    private final CamelliaLinearInitializationExecutorConfig<K, T> config;
    private final ConcurrentHashMap<String, DynamicCapacityLinkedBlockingQueue<CompletableFuture<T>>> futureQueueMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
    private final DynamicValueGetter<Integer> pendingQueueSize;

    private final LockMap lockMap = new LockMap();

    private final Initializer<K, T> initializer;
    private final CamelliaHashedExecutor executor;

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
        this.executor = new CamelliaHashedExecutor("liner-initialization-" + name, config.getPoolSize(),
                config.getPendingQueueSize(), new CamelliaHashedExecutor.AbortPolicy());
        this.pendingQueueSize = config.getPendingQueueSize();

        CamelliaExecutorMonitor.register(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public CamelliaExecutorStats getStats() {
        return executor.getStats();
    }

    public CamelliaLinearInitializationExecutorConfig<K, T> getConfig() {
        return config;
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
            return wrapper(value);
        }
        synchronized (lockMap.getLockObj(keyStr)) {
            value = cache.get(keyStr);
            if (value != null) {
                return wrapper(value);
            }
            CompletableFuture<T> future = new CompletableFuture<>();
            boolean offer = offerFutureQueue(keyStr, future);
            if (!offer) {
                future.complete(null);//此时不保证complete顺序了
                return future;
            }

            try {
                executor.submit(keyStr, () -> {
                    T newValue = cache.get(keyStr);
                    if (newValue != null) {
                        synchronized (lockMap.getLockObj(keyStr)) {
                            clearFutureQueue(keyStr, newValue);
                        }
                        return;
                    }
                    try {
                        newValue = initializer.initialize(key);
                        synchronized (lockMap.getLockObj(keyStr)) {
                            clearFutureQueue(keyStr, newValue);
                            if (newValue != null) {
                                cache.put(keyStr, newValue);
                            } else {
                                logger.error("initialize fail, key = {}", keyStr);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("initialize error, key = {}", keyStr);
                        synchronized (lockMap.getLockObj(keyStr)) {
                            clearFutureQueue(keyStr, null);
                        }
                    }
                });
            } catch (Exception e) {
                future.complete(null);//此时不保证complete顺序了
            }
            return future;
        }
    }

    /**
     * 获取资源，如果有则返回，否则返回null
     * @param key 资源key
     * @return 资源
     */
    public T get(K key) {
        return cache.get(key.toString());
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
        clearFutureQueue(key.toString(), value);
    }

    /**
     * 获取所有资源
     * @return 资源列表
     */
    public List<T> getAll() {
        return new ArrayList<>(cache.values());
    }

    private boolean offerFutureQueue(String keyStr, CompletableFuture<T> future) {
        return getFutureQueue(keyStr).offer(future);
    }

    private void clearFutureQueue(String keyStr, T value) {
        DynamicCapacityLinkedBlockingQueue<CompletableFuture<T>> queue = getFutureQueue(keyStr);
        clear(keyStr, queue, value);
    }

    private void clear(String keyStr, DynamicCapacityLinkedBlockingQueue<CompletableFuture<T>> queue, T value) {
        if (queue == null || queue.isEmpty()) return;
        CompletableFuture<T> completableFuture = queue.poll();
        while (completableFuture != null) {
            try {
                completableFuture.complete(value);
            } catch (Exception e) {
                logger.error("complete error, key = {}", keyStr);
            }
            completableFuture = queue.poll();
        }
    }

    private DynamicCapacityLinkedBlockingQueue<CompletableFuture<T>> getFutureQueue(String keyStr) {
        DynamicCapacityLinkedBlockingQueue<CompletableFuture<T>> queue = futureQueueMap.get(keyStr);
        if (queue == null) {
            queue = futureQueueMap.computeIfAbsent(keyStr, k -> new DynamicCapacityLinkedBlockingQueue<>(pendingQueueSize));
        }
        return queue;
    }

    private CompletableFuture<T> wrapper(T value) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(value);
        return future;
    }

    public static interface Initializer<K, T> {
        T initialize(K key);
    }

}
