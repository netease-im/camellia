package com.netease.nim.camellia.core.discovery;


import java.util.List;

/**
 * Created by caojiajun on 2022/3/1
 */
public interface CamelliaServerSelector<T> {

    T pick(List<T> list, Object key);
}
