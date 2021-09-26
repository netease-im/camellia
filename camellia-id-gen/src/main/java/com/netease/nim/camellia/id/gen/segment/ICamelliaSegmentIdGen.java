package com.netease.nim.camellia.id.gen.segment;


import java.util.List;

/**
 * 基于数据库的id生成器，趋势递增，支持设置单元
 * Created by caojiajun on 2021/9/24
 */
public interface ICamelliaSegmentIdGen {

    /**
     * 获取一批id
     * @param count 数量
     * @return 一批id
     */
    List<Long> genIds(String tag, int count);

    /**
     * 获取一个id
     * @return id
     */
    long genId(String tag);

}
