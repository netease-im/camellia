package com.netease.nim.camellia.redis.proxy.tls;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.tls.frontend.OpenSslServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.OpenSslUpstreamTlsProvider;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.ReferenceCountedOpenSslEngine;
import io.netty.handler.ssl.SslHandler;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OpenSslTlsProviderTest extends AbstractTlsProviderTest {

    @Override
    protected void configureProviders() {
        // Fail rather than silently testing JSSE when the native library is missing.
        OpenSsl.ensureAvailability();
        conf.put("server.tls.provider.class.name", OpenSslServerTlsProvider.class.getName());
        conf.put("upstream.tls.provider.class.name", OpenSslUpstreamTlsProvider.class.getName());
    }

    @Override
    protected void verifyEngine(SslHandler handler) {
        assertTrue(handler.engine() instanceof ReferenceCountedOpenSslEngine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedSessionCreationFailsDuringInit() {
        conf.put("server.tls.enable.session.creation", "true");
        ProxyDynamicConf.reload();
        newServerProvider().init();
    }
}
