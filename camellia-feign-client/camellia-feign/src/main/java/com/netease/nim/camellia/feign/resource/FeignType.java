package com.netease.nim.camellia.feign.resource;

/**
 * Created by caojiajun on 2022/3/1
 */
public enum FeignType {

    //格式：feign#http://www.foo.com
    //格式：feign#https://www.foo.com
    Feign("feign#"),

    //格式：feign-discovery#http://serviceName
    //格式：feign-discovery#https://serviceName
    FeignDiscovery("feign-discovery#"),

    ;
    private final String prefix;

    FeignType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
