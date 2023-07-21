package com.netease.nim.camellia.http.accelerate.proxy.core.proxy;

import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;

/**
 * 接收来自nginx的请求
 * Created by caojiajun on 2023/7/6
 */
public interface IHttpAccelerateProxy {

    void start();

    ServerStartupStatus getStatus();
}
