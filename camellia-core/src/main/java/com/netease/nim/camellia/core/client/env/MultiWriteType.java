package com.netease.nim.camellia.core.client.env;

/**
 * Created by caojiajun on 2022/4/19
 */
public enum MultiWriteType {

    //单线程顺序执行
    SINGLE_THREAD,

    //多线程并发执行
    MULTI_THREAD_CONCURRENT,

    //异步多线程执行，所有写地址都异步执行
    ASYNC_MULTI_THREAD,

    //混合异步多线程执行，第一个写地址同步执行，如果成功则异步执行其余地址
    MISC_ASYNC_MULTI_THREAD,
    ;
}
