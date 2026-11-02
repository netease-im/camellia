package com.netease.nim.camellia.redis.proxy.tls.upstream;

import com.netease.nim.camellia.redis.proxy.tls.OpenSslContextUtil;
import io.netty.handler.ssl.OpenSsl;

import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

/**
 * BoringSSL via Netty tcnative, using the same certificate configuration as DefaultUpstreamTlsProvider.
 */
public class OpenSslUpstreamTlsProvider extends DefaultUpstreamTlsProvider {

    @Override
    public boolean init() {
        OpenSsl.ensureAvailability();
        return super.init();
    }

    @Override
    protected Supplier<SSLEngine> createSslEngineFactory(String caCertFilePath, String certFilePath,
                                                         String keyFilePath, String password) {
        return OpenSslContextUtil.createSslEngineFactory(false, caCertFilePath, certFilePath, keyFilePath, password);
    }
}
