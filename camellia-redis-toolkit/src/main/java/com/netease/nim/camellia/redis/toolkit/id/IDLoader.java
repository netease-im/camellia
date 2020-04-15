package com.netease.nim.camellia.redis.toolkit.id;

/**
 * 根据tag和步长返回一组id
 * Created by caojiajun on 2020/4/9.
 */
public interface IDLoader<T> {

    IDRange load(T tag, int step);
}
