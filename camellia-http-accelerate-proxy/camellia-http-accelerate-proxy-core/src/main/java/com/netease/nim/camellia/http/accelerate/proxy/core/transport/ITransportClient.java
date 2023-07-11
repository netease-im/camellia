package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 把请求从本地sidecar-proxy转发给其他sidecar-proxy
 * Created by caojiajun on 2023/7/6
 */
public interface ITransportClient {


    /**
     * send the request to ISidCarProxyServer
     * @param proxyRequest request
     * @return response
     */
    CompletableFuture<ProxyResponse> send(ProxyRequest proxyRequest);

    void start();

    void stop();
}
