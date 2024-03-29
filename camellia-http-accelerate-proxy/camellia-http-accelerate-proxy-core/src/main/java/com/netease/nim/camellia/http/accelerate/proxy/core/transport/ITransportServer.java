package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;

/**
 * 接收来自其他sidecar-proxy的请求
 * Created by caojiajun on 2023/7/6
 */
public interface ITransportServer {

    void start();

    ServerStartupStatus getStatus();
}
