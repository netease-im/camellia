package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.ssl.SSLContextUtil;

import javax.net.ssl.SSLContext;

/**
 * Created by caojiajun on 2023/8/8
 */
public class SSLUtils {

    public static SSLContext proxyFrontendSSLContext() {
        String caCrtFilePath;
        String caCrtFile = ProxyDynamicConf.getString("proxy.frontend.tls.ca.crt.file", null);
        if (caCrtFile == null) {
            caCrtFilePath = ProxyDynamicConf.getString("proxy.frontend.tls.ca.crt.file.path", null);
        } else {
            caCrtFilePath = SSLContextUtil.getFilePath(caCrtFile);
        }
        if (caCrtFilePath == null) {
            throw new IllegalArgumentException("caCrtFilePath not found");
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
        return SSLContextUtil.genSSLContext(caCrtFilePath, crtFilePath, keyFilePath, password);
    }
}
