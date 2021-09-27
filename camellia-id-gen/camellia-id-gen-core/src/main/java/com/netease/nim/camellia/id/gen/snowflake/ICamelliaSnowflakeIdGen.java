package com.netease.nim.camellia.id.gen.snowflake;

/**
 * 雪花算法
 * Created by caojiajun on 2021/9/24
 */
public interface ICamelliaSnowflakeIdGen {

    /**
     * 生成一个id
     * @return id
     */
    long genId();
}
