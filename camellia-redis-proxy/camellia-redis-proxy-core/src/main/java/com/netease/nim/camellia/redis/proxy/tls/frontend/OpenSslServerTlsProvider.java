package com.netease.nim.camellia.redis.proxy.tls.frontend;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.tls.OpenSslContextUtil;
import io.netty.handler.ssl.OpenSsl;

import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

/**
 * BoringSSL via Netty tcnative, using the same certificate configuration as DefaultServerTlsProvider.
 */
public class OpenSslServerTlsProvider extends DefaultServerTlsProvider {

    @Override
    public boolean init() {
        OpenSsl.ensureAvailability();
        if (ProxyDynamicConf.getBoolean("server.tls.enable.session.creation", false)) {
            throw new IllegalArgumentException("OpenSSL does not support server.tls.enable.session.creation=true");
        }
        return super.init();
    }

    @Override
    protected Supplier<SSLEngine> createSslEngineFactory(String caCertFilePath, String certFilePath,
                                                         String keyFilePath, String password) {
        return OpenSslContextUtil.createSslEngineFactory(true, caCertFilePath, certFilePath, keyFilePath, password);
    }
}
