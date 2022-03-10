package com.netease.nim.camellia.feign.discovery;


import com.netease.nim.camellia.feign.resource.FeignResource;

public interface FeignResourcePool {

    FeignResource getResource(Object key);

    void onError(FeignResource feignResource);
}
