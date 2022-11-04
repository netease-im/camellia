package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/4
 */
public interface CamelliaMergeTask<K extends CamelliaMergeTaskKey, V> {

    /**
     * 任务合并类型
     * 支持单机模式和集群模式
     */
    CamelliaMergeTaskType getType();

    /**
     * 任务合并后，任务结果缓存时长，单位ms
     */
    long resultCacheMillis();

    /**
     * 相同key的任务，堆积达到多少会同步执行（可能task非merge）
     */
    default int taskQueueSizeThreshold() {
        return 1000;
    }

    /**
     * 整个线程池的任务队列（不同key的任务一起计算），堆积达到多少会同步执行（可能task非merge）
     */
    default int executorQueueSizeThreshold() {
        return 10000;
    }

    /**
     * 任务合并时的并发控制锁的超时时间，仅集群任务合并时有效
     * 当不同业务节点执行同一个任务时，会尝试获取分布式锁（从而整体并发=1），如果在lockTimeoutMillis时间内还没有获取到锁，则会不等待而直接执行
     * 单机内的并发只有1
     */
    default long lockTimeoutMillis() {
        return 3000;
    }

    /**
     * 任务结果的序列化和反序列化工具
     */
    CamelliaMergeTaskResultSerializer<V> getResultSerializer();

    /**
     * 任务的tag
     */
    String getTag();

    /**
     * 任务的输入key
     */
    K getKey();

    /**
     * 实际任务执行的方法
     * @param key 通过getKey方法获取输入
     * @return 任务执行结果
     */
    V exec(K key);

}
