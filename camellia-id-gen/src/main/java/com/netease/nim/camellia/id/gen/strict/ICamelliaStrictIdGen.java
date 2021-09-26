package com.netease.nim.camellia.id.gen.strict;

/**
 * 可以返回严格递增的消息id
 * Created by caojiajun on 2021/9/24
 */
public interface ICamelliaStrictIdGen {

    /**
     * 生成一个id
     * @param tag 业务tag
     * @return id
     */
    long genId(String tag);

    /**
     * 返回最新的id，但是不使用
     * @param tag 业务tag
     * @return id
     */
    long peekId(String tag);
}
