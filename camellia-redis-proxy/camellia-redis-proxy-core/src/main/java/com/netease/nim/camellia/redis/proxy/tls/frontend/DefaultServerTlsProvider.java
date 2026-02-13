package com.netease.nim.camellia.redis.proxy.tls.frontend;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.tls.SSLContextUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ImmediateExecutor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/8/9
 */
public class DefaultServerTlsProvider implements ServerTlsProvider {

    private SSLContext sslContext;
    private boolean startTls = false;
    private Executor executor = ImmediateExecutor.INSTANCE;

    @Override
    public boolean init() {
        createSSLContext();
        this.startTls = ProxyDynamicConf.getBoolean("server.tls.startTls.enable", false);
        int poolSize = ProxyDynamicConf.getInt("server.tls.executor.pool.size", 0);
        int queueSize = ProxyDynamicConf.getInt("server.tls.executor.queue.size", 10240);
        if (poolSize <= 0) {
            this.executor = ImmediateExecutor.INSTANCE;
        } else {
            this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueSize), new DefaultThreadFactory("server-tls-executor"),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return true;
    }

    @Override
    public SslHandler createSslHandler() {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        String needClientAuth = ProxyDynamicConf.getString("server.tls.need.client.auth", "true");
        if (needClientAuth != null) {
            if (needClientAuth.equalsIgnoreCase("true")) {
                sslEngine.setNeedClientAuth(true);
            } else if (needClientAuth.equalsIgnoreCase("false")) {
                sslEngine.setNeedClientAuth(false);
            }
        }
        String wantClientAuth = ProxyDynamicConf.getString("server.tls.want.client.auth", "");
        if (wantClientAuth != null) {
            if (wantClientAuth.equalsIgnoreCase("true")) {
                sslEngine.setWantClientAuth(true);
            } else if (wantClientAuth.equalsIgnoreCase("false")) {
                sslEngine.setWantClientAuth(false);
            }
        }
        String enableSessionCreation = ProxyDynamicConf.getString("server.tls.enable.session.creation", "");
        if (enableSessionCreation != null) {
            if (enableSessionCreation.equalsIgnoreCase("true")) {
                sslEngine.setEnableSessionCreation(true);
            } else if (enableSessionCreation.equalsIgnoreCase("false")) {
                sslEngine.setEnableSessionCreation(false);
            }
        }
        String enabledProtocols = ProxyDynamicConf.getString("server.tls.enable.protocols", "");
        if (enabledProtocols != null && !enabledProtocols.trim().isEmpty()) {
            String[] protocols = enabledProtocols.split(",");
            sslEngine.setEnabledProtocols(protocols);
        }
        String enabledCipherSuites = ProxyDynamicConf.getString("server.tls.enable.cipher.suites", "");
        if (enabledCipherSuites != null && !enabledCipherSuites.trim().isEmpty()) {
            String[] cipherSuites = enabledCipherSuites.split(",");
            sslEngine.setEnabledCipherSuites(cipherSuites);
        }
        sslEngine.setUseClientMode(false);
        return new SslHandler(sslEngine, startTls, executor);
    }

    private void createSSLContext() {
        String caCertFilePath;
        String caCertFile = ProxyDynamicConf.getString("server.tls.ca.cert.file", null);
        if (caCertFile == null) {
            caCertFilePath = ProxyDynamicConf.getString("server.tls.ca.cert.file.path", null);
        } else {
            caCertFilePath = FileUtils.getClasspathFilePath(caCertFile);
        }
        String crtFilePath;
        String crtFile = ProxyDynamicConf.getString("server.tls.cert.file", null);
        if (crtFile == null) {
            crtFilePath = ProxyDynamicConf.getString("server.tls.cert.file.path", null);
        } else {
            crtFilePath = FileUtils.getClasspathFilePath(crtFile);
        }
        if (crtFilePath == null) {
            throw new IllegalArgumentException("crtFilePath not found");
        }
        String keyFilePath;
        String keyFile = ProxyDynamicConf.getString("server.tls.key.file", null);
        if (keyFile == null) {
            keyFilePath = ProxyDynamicConf.getString("server.tls.key.file.path", null);
        } else {
            keyFilePath = FileUtils.getClasspathFilePath(keyFile);
        }
        if (keyFilePath == null) {
            throw new IllegalArgumentException("keyFilePath not found");
        }
        String password = ProxyDynamicConf.getString("server.tls.password", null);
        this.sslContext = SSLContextUtil.genSSLContext(caCertFilePath, crtFilePath, keyFilePath, password);
    }


}
