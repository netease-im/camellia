package com.netease.nim.camellia.id.gen.snowflake;

/**
 * Created by caojiajun on 2021/9/18
 */
public interface WorkerIdGen {

    /**
     * 生成一个workerId
     * @param maxWorkerId 最大的maxWorkerId，范围=[0,maxWorkerId]
     * @return workerId
     */
    long genWorkerId(long maxWorkerId);
}
