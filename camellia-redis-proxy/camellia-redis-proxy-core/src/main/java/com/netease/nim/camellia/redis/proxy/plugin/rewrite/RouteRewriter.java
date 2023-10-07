package com.netease.nim.camellia.redis.proxy.plugin.rewrite;

import com.netease.nim.camellia.redis.proxy.plugin.ProxyRequest;

/**
 * Created by caojiajun on 2023/10/7
 */
public interface RouteRewriter {

    RouteRewriteResult rewrite(ProxyRequest request);
}
