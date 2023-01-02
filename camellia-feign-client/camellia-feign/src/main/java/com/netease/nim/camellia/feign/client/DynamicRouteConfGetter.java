package com.netease.nim.camellia.feign.client;

/**
 * Created by caojiajun on 2022/3/28
 */
public interface DynamicRouteConfGetter {

    /**
     * 根据参数进行动态路由
     * @param routeKey 参数中的路由key
     * @return bgroup
     */
    String bgroup(Object routeKey);

}
