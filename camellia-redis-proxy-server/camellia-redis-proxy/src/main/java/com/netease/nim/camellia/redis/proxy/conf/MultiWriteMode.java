package com.netease.nim.camellia.redis.proxy.conf;

/**
 * policy of multi-write
 * Created by caojiajun on 2020/9/27
 */
public enum MultiWriteMode {

    /**
     * first redis reply, then reply to client
     */
    FIRST_RESOURCE_ONLY,

    /**
     * all multi-write redis reply, then reply first redis's reply
     */
    ALL_RESOURCES_NO_CHECK,

    /**
     * all multi-write redis reply, and none of replies is error, then reply first redis's reply, else reply the first error reply
     */
    ALL_RESOURCES_CHECK_ERROR,
    ;
}
