package com.netease.nim.camellia.core.client.env;

/**
 * Created by caojiajun on 2022/4/19
 */
public enum MultiWriteType {

    //单线程顺序执行
    SINGLE_THREAD,

    //多线程并发执行
    MULTI_THREAD_CONCURRENT,

    //异步多线程执行
    ASYNC_MULTI_THREAD,
    ;
}
