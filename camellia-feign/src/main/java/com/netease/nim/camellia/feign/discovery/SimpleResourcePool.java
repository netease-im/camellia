package com.netease.nim.camellia.feign.discovery;

import com.netease.nim.camellia.feign.resource.FeignResource;

/**
 * Created by caojiajun on 2022/3/1
 */
public class SimpleResourcePool implements FeignResourcePool {

    private final FeignResource feignResource;

    public SimpleResourcePool(FeignResource feignResource) {
        this.feignResource = feignResource;
    }

    @Override
    public FeignResource getResource(Object key) {
        return feignResource;
    }

    @Override
    public void onError(FeignResource feignResource) {
    }
}
