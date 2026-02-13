package com.netease.nim.camellia.redis.proxy.tls.frontend;

import io.netty.handler.ssl.SslHandler;


/**
 * Created by caojiajun on 2023/8/9
 */
public interface ServerTlsProvider {

    boolean init();

    SslHandler createSslHandler();
}
