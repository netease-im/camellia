package com.netease.nim.camellia.id.gen.common;


/**
 * 根据tag和步长返回一组id
 * Created by caojiajun on 2020/4/9.
 */
public interface IDLoader {

    IDRange load(String tag, int step);

    default boolean update(String tag, long id) {
        return false;
    }
}
