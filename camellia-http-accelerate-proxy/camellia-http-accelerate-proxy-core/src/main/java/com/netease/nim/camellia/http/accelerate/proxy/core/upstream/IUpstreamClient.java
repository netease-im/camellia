package com.netease.nim.camellia.http.accelerate.proxy.core.upstream;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 请求发给后端
 * Created by caojiajun on 2023/7/6
 */
public interface IUpstreamClient {

    /**
     * send the request to nginx
     * @param request request
     * @return response
     */
    CompletableFuture<ProxyResponse> send(ProxyRequest request);

}
