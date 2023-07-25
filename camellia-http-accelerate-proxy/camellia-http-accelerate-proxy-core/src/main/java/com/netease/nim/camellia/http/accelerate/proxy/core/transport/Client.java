package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config.TransportServerType;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.Status;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/7/25
 */
public interface Client {

    TransportServerType getType();

    void start();

    void stop();

    void send(ProxyRequest request, CompletableFuture<ProxyResponse> future);

    long getId();

    Status getStatus();

    ServerAddr getAddr();

    void setClosingStatus();
}
