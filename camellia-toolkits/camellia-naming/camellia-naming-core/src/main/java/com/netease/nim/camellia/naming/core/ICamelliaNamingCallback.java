package com.netease.nim.camellia.naming.core;


import java.util.List;

/**
 * Created by caojiajun on 2025/11/18
 */
public interface ICamelliaNamingCallback {

    /**
     * add callback
     * @param list added list
     */
    void add(List<InstanceInfo> list);

    /**
     * remove callback
     * @param list removed list
     */
    void remove(List<InstanceInfo> list);

}
