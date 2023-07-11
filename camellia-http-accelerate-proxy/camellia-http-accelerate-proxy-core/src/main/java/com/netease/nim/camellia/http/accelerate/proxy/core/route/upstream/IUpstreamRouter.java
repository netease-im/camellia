package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.upstream.IUpstreamClient;

/**
 * Created by caojiajun on 2023/7/7
 */
public interface IUpstreamRouter {

    void start();

    void reload();

    IUpstreamClient select(ProxyRequest request);
}
