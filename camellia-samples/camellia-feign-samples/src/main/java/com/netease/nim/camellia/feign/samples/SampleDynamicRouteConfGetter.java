package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.feign.client.DynamicRouteConfGetter;

/**
 * Created by caojiajun on 2022/4/6
 */
public class SampleDynamicRouteConfGetter implements DynamicRouteConfGetter {

    @Override
    public String bgroup(Object routeKey) {
        if (routeKey == null) return "default";
        if (String.valueOf(routeKey).equals("1")) {
            return "bgroup1";
        } else if (String.valueOf(routeKey).equals("2")) {
            return "bgroup2";
        }
        return "default";
    }
}
