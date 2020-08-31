package com.netease.nim.camellia.redis.proxy.conf;

/**
 *
 * Created by caojiajun on 2020/8/28
 */
public enum QueueType {
    Disruptor,//use disruptor to transpond
    LinkedBlockingQueue,//use LinkedBlockingQueue to transpond
    None,//do not use queue，direct transpond
    ;
}
