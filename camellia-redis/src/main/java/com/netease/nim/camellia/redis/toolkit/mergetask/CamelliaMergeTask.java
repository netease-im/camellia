package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * 任务合并框架
 * 适用场景：
 * 有相同请求参数的查询请求，并发或者高tps查询，对于数据一致性要求不是那么高
 * 此时为了避免每次请求都落到底层（DB或者复杂的cache计算），CamelliaMergeTask会控制相同查询请求的并发，穿透过去一个请求，并把结果分发给其他等待队列中的其他请求
 * 此外，还可以对结果进行短暂的缓存，从而提高请求merge的效果
 *
 * 确认任务唯一性的是：tag + key的组合键
 * Created by caojiajun on 2022/11/4
 */
public interface CamelliaMergeTask<K extends CamelliaMergeTaskKey, V> {

    /**
     * 任务合并类型
     * 支持单机模式和集群模式
     * 单机模式：只对单机的请求进行并发控制和合并
     * 集群模式：对集群内的所有请求进行并发控制和合并
     */
    CamelliaMergeTaskType getType();

    /**
     * 任务合并后，任务结果缓存时长，单位ms
     *
     * 1）如果大于0，则除了等待队列中的请求会使用刚刚获取的结果之后，缓存有效期内新进来的请求也会直接返回缓存结果
     *
     * 2）如果小于等于0，则不进行结果的缓存
     * ---单机合并：只对等待队列中的请求进行结果分发，分发完成之后就丢弃任务，如果有新请求过来，则之后的第一个请求会穿透
     * ---集群合并：对集群内的所有请求进行并发控制，只穿透一个，并且把结果分发给单机内的其他请求，每个节点都会穿透一次请求（并且会排序）
     */
    long resultCacheMillis();

    /**
     * 相同key的任务，堆积达到多少会直接执行，而不是等待第一个请求的执行结果缓存
     */
    default int taskQueueSizeThreshold() {
        return 1000;
    }

    /**
     * 整个线程池的任务队列（不同key的任务一起计算），堆积达到多少会直接执行，而不是等待第一个请求的执行结果缓存
     */
    default int executorQueueSizeThreshold() {
        return 10000;
    }

    /**
     * 任务合并时的并发控制锁的超时时间，仅集群合并时有效
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
     * @param key CamelliaMergeTaskExecutor会通过getKey方法获取输入，随后调用本方法
     * @return 任务执行结果
     */
    V execute(K key) throws Exception;

}
