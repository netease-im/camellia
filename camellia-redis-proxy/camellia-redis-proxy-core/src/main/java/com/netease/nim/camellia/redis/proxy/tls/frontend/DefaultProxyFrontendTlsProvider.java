package com.netease.nim.camellia.redis.proxy.tls.frontend;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.ssl.SSLContextUtil;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Created by caojiajun on 2023/8/9
 */
public class DefaultProxyFrontendTlsProvider implements ProxyFrontendTlsProvider {

    @Override
    public SSLContext createSSLContext() {
        String caFilePath;
        String caFile = ProxyDynamicConf.getString("proxy.frontend.tls.ca.file", null);
        if (caFile == null) {
            caFilePath = ProxyDynamicConf.getString("proxy.frontend.tls.ca.file.path", null);
        } else {
            caFilePath = SSLContextUtil.getFilePath(caFile);
        }
        if (caFilePath == null) {
            throw new IllegalArgumentException("caFilePath not found");
        }
        String crtFilePath;
        String crtFile = ProxyDynamicConf.getString("proxy.frontend.tls.crt.file", null);
        if (crtFile == null) {
            crtFilePath = ProxyDynamicConf.getString("proxy.frontend.tls.crt.file.path", null);
        } else {
            crtFilePath = SSLContextUtil.getFilePath(crtFile);
        }
        if (crtFilePath == null) {
            throw new IllegalArgumentException("crtFilePath not found");
        }
        String keyFilePath;
        String keyFile = ProxyDynamicConf.getString("proxy.frontend.tls.key.file", null);
        if (keyFile == null) {
            keyFilePath = ProxyDynamicConf.getString("proxy.frontend.tls.key.file.path", null);
        } else {
            keyFilePath = SSLContextUtil.getFilePath(keyFile);
        }
        if (keyFilePath == null) {
            throw new IllegalArgumentException("keyFilePath not found");
        }
        String password = ProxyDynamicConf.getString("proxy.frontend.tls.password", null);
        return SSLContextUtil.genSSLContext(caFilePath, crtFilePath, keyFilePath, password);
    }

    @Override
    public SslHandler createSslHandler(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setNeedClientAuth(ProxyDynamicConf.getBoolean("proxy.frontend.tls.need.client.auth", true));
        sslEngine.setUseClientMode(false);
        return new SslHandler(sslEngine);
    }
}
