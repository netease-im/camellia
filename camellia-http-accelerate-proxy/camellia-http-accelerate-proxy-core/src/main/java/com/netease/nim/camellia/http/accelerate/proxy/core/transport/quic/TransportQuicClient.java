package com.netease.nim.camellia.http.accelerate.proxy.core.transport.quic;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportClient;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/7/6
 */
public class TransportQuicClient implements ITransportClient {

    @Override
    public CompletableFuture<ProxyResponse> send(ProxyRequest proxyRequest) {
        //TODO
        return null;
    }

    @Override
    public void start() {
        //TODO
    }

    @Override
    public void stop() {
        //TODO
    }
}
