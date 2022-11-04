package com.netease.nim.camellia.redis.toolkit.mergetask;

import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLockManager;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMergeTaskExecutor.class);

    private final CamelliaHashedExecutor executor;
    private final CamelliaRedisTemplate template;
    private final CamelliaMergeTaskCache taskCache;
    private CamelliaRedisLockManager lockManager;

    private final CamelliaLocalCache resultCache = new CamelliaLocalCache();

    public CamelliaMergeTaskExecutor() {
        this(new CamelliaHashedExecutor("camellia-merge-task",
                Runtime.getRuntime().availableProcessors() * 4, 100000, new CamelliaHashedExecutor.CallerRunsPolicy()));
    }

    public CamelliaMergeTaskExecutor(CamelliaHashedExecutor executor) {
        this(executor, null);
    }

    public CamelliaMergeTaskExecutor(CamelliaHashedExecutor executor, CamelliaRedisTemplate template) {
        this(executor, template, 1000, 2000);
    }

    /**
     * @param executor 任务执行线程池
     * @param template redis客户端，只有集群合并才需要
     * @param taskCapacity 任务
     */
    public CamelliaMergeTaskExecutor(CamelliaHashedExecutor executor, CamelliaRedisTemplate template, int taskCapacity, int taskIdCapacity) {
        this.executor = executor;
        this.template = template;
        if (template != null) {
            this.lockManager = new CamelliaRedisLockManager(template);
        }
        this.taskCache = new CamelliaMergeTaskCache(taskCapacity, taskIdCapacity);
    }


    /**
     * 任务合并执行器
     * 返回的是Future，大部分情况下都是异步执行的（非阻塞）；但是如果等待队列过长，为了避免延迟太久，还是可能同步执行（阻塞）
     * @param task 任务
     * @param <K> 任务的key
     * @param <V> 任务的结果
     * @return 任务执行结果，是一个future
     */
    public <K extends CamelliaMergeTaskKey, V> CamelliaMergeTaskFuture<V> submit(CamelliaMergeTask<K, V> task) {
        CamelliaMergeTaskType type = task.getType();
        String taskId = UUID.randomUUID().toString();
        String taskKey = new String(taskKey(task), StandardCharsets.UTF_8);
        taskCache.addTask(taskKey, taskId);
        CamelliaMergeTaskFuture<V> future = new CamelliaMergeTaskFuture<>(() -> taskCache.removeTask(taskKey, taskId));
        try {
            if (type == CamelliaMergeTaskType.STANDALONE) {
                //check task result cache
                V v = getLocalResultCache(task);
                if (v != null) {
                    future.complete(new CamelliaMergeTaskResult<>(CamelliaMergeTaskResult.Type.LOCAL_CACHE_HIT, v));
                    return future;
                }
                //check task pending count
                if (taskCache.size(taskKey) > task.taskQueueSizeThreshold() || executor.getQueueSize(taskKey) > task.executorQueueSizeThreshold()) {
                    try {
                        //execute direct
                        try {
                            v = task.exec(task.getKey());
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                            return future;
                        }
                        setLocalResultCache(task, v);
                        future.complete(new CamelliaMergeTaskResult<>(CamelliaMergeTaskResult.Type.EXEC, v));
                    } catch (Exception e) {
                        logger.error("merge task direct execute error, tag = {}, key = {}", task.getTag(), task.getKey().serialize(), e);
                        future.completeExceptionally(e);
                    }
                    return future;
                }
                //CamelliaHashedExecutor execute in single thread with same taskKey
                executor.submit(taskKey, () -> {
                    V result = null;
                    CamelliaMergeTaskResult.Type resultType = CamelliaMergeTaskResult.Type.LOCAL_CACHE_HIT;
                    try {
                        //check task result cache
                        result = getLocalResultCache(task);
                        if (result == null) {
                            //execute
                            try {
                                result = task.exec(task.getKey());
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                                return;
                            }
                            resultType = CamelliaMergeTaskResult.Type.EXEC;
                            //build task result cache
                            setLocalResultCache(task, result);
                        }
                        future.complete(new CamelliaMergeTaskResult<>(resultType, result));
                    } catch (Exception e) {
                        logger.error("merge task execute error, tag = {}, key = {}", task.getTag(), task.getKey().serialize(), e);
                        if (result != null) {
                            future.complete(new CamelliaMergeTaskResult<>(resultType, result));
                        } else {
                            future.completeExceptionally(e);
                        }
                    }
                });
            } else if (type == CamelliaMergeTaskType.CLUSTER) {
                if (template == null) {
                    throw new IllegalArgumentException("redis template is null");
                }
                CamelliaMergeTaskResultSerializer<V> resultSerializer = task.getResultSerializer();
                if (resultSerializer == null) {
                    throw new IllegalArgumentException("resultSerializer is null");
                }
                CamelliaMergeTaskResult.Type resultType = CamelliaMergeTaskResult.Type.LOCAL_CACHE_HIT;
                //check task result cache
                V v = getLocalResultCache(task);
                if (v == null) {
                    v = getRedisResultCache(task);
                    resultType = CamelliaMergeTaskResult.Type.REDIS_CACHE_HIT;
                }
                if (v != null) {
                    future.complete(new CamelliaMergeTaskResult<>(resultType, v));
                    return future;
                }
                //check task pending count
                if (taskCache.size(taskKey) > task.taskQueueSizeThreshold() || executor.getQueueSize(taskKey) > task.executorQueueSizeThreshold()) {
                    try {
                        //execute direct
                        try {
                            v = task.exec(task.getKey());
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                            return future;
                        }
                        //build task result cache
                        setLocalResultCache(task, v);
                        setRedisResultCache(task, v);
                        future.complete(new CamelliaMergeTaskResult<>(CamelliaMergeTaskResult.Type.EXEC, v));
                    } catch (Exception e) {
                        logger.error("merge task execute error, tag = {}, key = {}", task.getTag(), task.getKey().serialize(), e);
                        future.completeExceptionally(e);
                    }
                    return future;
                }
                //CamelliaHashedExecutor execute in single thread with same taskKey
                executor.submit(taskKey, () -> {
                    V result = null;
                    CamelliaMergeTaskResult.Type resultType1 = CamelliaMergeTaskResult.Type.LOCAL_CACHE_HIT;
                    try {
                        //check task result cache
                        result = getLocalResultCache(task);
                        if (result == null) {
                            result = getRedisResultCache(task);
                            resultType1 = CamelliaMergeTaskResult.Type.REDIS_CACHE_HIT;
                        }
                        if (result == null) {
                            //acquire lock in cluster mode
                            byte[] lockKey = lockKey(task);
                            boolean lockOk = lockManager.lock(lockKey, task.lockTimeoutMillis(), task.lockTimeoutMillis());
                            if (!lockOk) {
                                logger.warn("try lock fail after {}ms, merge task with tag = {}, taskKey = {} will execute direct",
                                        task.lockTimeoutMillis(), task.getTag(), task.getKey().serialize());
                            }
                            try {
                                //check task result cache again
                                result = getRedisResultCache(task);
                                if (result == null) {
                                    //execute
                                    try {
                                        result = task.exec(task.getKey());
                                    } catch (Exception e) {
                                        future.completeExceptionally(e);
                                        return;
                                    }
                                    resultType1 = CamelliaMergeTaskResult.Type.EXEC;
                                    //build task result cache
                                    setLocalResultCache(task, result);
                                    setRedisResultCache(task, result);
                                }
                            } finally {
                                lockManager.release(lockKey);
                            }
                        }
                        future.complete(new CamelliaMergeTaskResult<>(resultType1, result));
                    } catch (Exception e) {
                        logger.error("merge task execute error, tag = {}, key = {}", task, task.getKey().serialize(), e);
                        if (result != null) {
                            future.complete(new CamelliaMergeTaskResult<>(resultType1, result));
                        } else {
                            future.completeExceptionally(e);
                        }
                    }
                });
            } else {
                throw new IllegalArgumentException("unknown merge task type = " + type);
            }
        } catch (Throwable e) {
            future.completeExceptionally(e);
            logger.error("merge task execute error, tag = {}, key = {}", task, task.getKey().serialize(), e);
        }
        return future;
    }

    private <K extends CamelliaMergeTaskKey, V> V getLocalResultCache(CamelliaMergeTask<K, V> task) {
        V result = null;
        try {
            byte[] value = resultCache.get(task.getTag(), task.getKey().serialize(), byte[].class);
            if (value != null) {
                result = task.getResultSerializer().deserialize(value);
            }
        } catch (Exception e) {
            logger.error("merge task get local cache result error, tag = {}, key = {}", task, task.getKey().serialize());
        }
        return result;
    }

    private <K extends CamelliaMergeTaskKey, V> void setLocalResultCache(CamelliaMergeTask<K, V> task, V result) {
        byte[] data = task.getResultSerializer().serialize(result);
        resultCache.put(task.getTag(), task.getKey().serialize(), data, task.resultCacheMillis());
    }

    private <K extends CamelliaMergeTaskKey, V> byte[] taskKey(CamelliaMergeTask<K, V> task) {
        return CacheUtil.buildCacheKey(task.getTag(), task.getKey().serialize()).getBytes(StandardCharsets.UTF_8);
    }

    private <K extends CamelliaMergeTaskKey, V> byte[] lockKey(CamelliaMergeTask<K, V> task) {
        return CacheUtil.buildCacheKey(task.getTag(), task.getKey().serialize(), "~lock").getBytes(StandardCharsets.UTF_8);
    }

    private <K extends CamelliaMergeTaskKey, V> V getRedisResultCache(CamelliaMergeTask<K, V> task) {
        byte[] value = null;
        try {
            value = template.get(taskKey(task));
        } catch (Exception e) {
            logger.error("merge task get redis cache result error, tag = {}, key = {}", task, task.getKey().serialize());
        }
        V result = null;
        if (value != null) {
            try {
                result = task.getResultSerializer().deserialize(value);
            } catch (Exception e) {
                logger.error("merge task deserialize redis cache result error, tag = {}, key = {}", task, task.getKey().serialize());
            }
        }
        return result;
    }

    private <K extends CamelliaMergeTaskKey, V> void setRedisResultCache(CamelliaMergeTask<K, V> task, V result) {
        try {
            byte[] serialize = task.getResultSerializer().serialize(result);
            template.psetex(taskKey(task), task.resultCacheMillis(), serialize);
        } catch (Exception e) {
            logger.error("merge task set redis cache result error, tag = {}, key = {}", task, task.getKey().serialize());
        }
    }
}
