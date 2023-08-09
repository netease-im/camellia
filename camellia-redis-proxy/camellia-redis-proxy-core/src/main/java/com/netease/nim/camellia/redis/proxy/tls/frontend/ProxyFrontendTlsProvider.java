package com.netease.nim.camellia.redis.proxy.tls.frontend;

import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;

/**
 * Created by caojiajun on 2023/8/9
 */
public interface ProxyFrontendTlsProvider {

    SSLContext createSSLContext();

    SslHandler createSslHandler(SSLContext sslContext);
}
