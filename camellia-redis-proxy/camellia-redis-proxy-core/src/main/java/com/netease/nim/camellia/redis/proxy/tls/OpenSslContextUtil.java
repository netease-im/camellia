package com.netease.nim.camellia.redis.proxy.tls;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

/**
 * Creates native TLS engines from the same key/trust managers used by JSSE.
 */
public class OpenSslContextUtil {

    private OpenSslContextUtil() {
    }

    public static Supplier<SSLEngine> createSslEngineFactory(boolean server, String caCertFilePath,
                                                            String certFilePath, String keyFilePath, String password) {
        OpenSsl.ensureAvailability();
        SSLContextUtil.TlsManagers managers = SSLContextUtil.loadTlsManagers(
                caCertFilePath, certFilePath, keyFilePath, password);
        try {
            SslContextBuilder builder = server ? SslContextBuilder.forServer(managers.keyManagerFactory())
                    : SslContextBuilder.forClient();
            // Match existing JSSE behavior: validate the CA chain without endpoint identification.
            // Netty 4.2 enables endpoint identification by default for client contexts.
            if (!server) {
                builder.endpointIdentificationAlgorithm(null);
            }
            if (!server && certFilePath != null && keyFilePath != null) {
                builder.keyManager(managers.keyManagerFactory());
            }
            // OPENSSL manages context lifetime, matching the providers' existing context cache lifecycle.
            SslContext context = builder.sslProvider(SslProvider.OPENSSL)
                    .trustManager(managers.trustManagerFactory()).build();
            return () -> context.newEngine(ByteBufAllocator.DEFAULT);
        } catch (Exception e) {
            throw new IllegalArgumentException("create OpenSSL context error", e);
        }
    }
}
