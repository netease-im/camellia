package com.netease.nim.camellia.redis.util;

import redis.clients.jedis.BitPosParams;

/**
 * Created by caojiajun on 2021/7/28
 */
public class CamelliaBitPosParams extends BitPosParams {

    public CamelliaBitPosParams() {
        super();
    }

    public CamelliaBitPosParams(long start) {
        super(start);
    }

    public CamelliaBitPosParams(long start, long end) {
        super(start, end);
    }

}
