package com.netease.nim.camellia.redis.proxy.tls.upstream;

import com.netease.nim.camellia.core.model.Resource;
import io.netty.handler.ssl.SslHandler;


/**
 * Created by caojiajun on 2023/8/9
 */
public interface ProxyUpstreamTlsProvider {

    boolean init();

    SslHandler createSslHandler(Resource resource);
}
