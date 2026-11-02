package com.netease.nim.camellia.redis.proxy.tls;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.tls.frontend.DefaultServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.DefaultUpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.UpstreamTlsProvider;
import io.netty.handler.ssl.SslHandler;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class DefaultTlsProviderTest extends AbstractTlsProviderTest {

    @Override
    protected void configureProviders() {
        // Exercise default selection without a provider override or native library requirement.
    }

    @Override
    protected void verifyEngine(SslHandler handler) {
        try {
            assertEquals(SSLContext.getDefault().createSSLEngine().getClass(), handler.engine().getClass());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void selectsDefaultProvidersWithoutConfiguration() {
        assertEquals(DefaultServerTlsProvider.class, newServerProvider().getClass());
        assertEquals(DefaultUpstreamTlsProvider.class, newUpstreamProvider().getClass());
        handshake(true);
    }

    @Test
    public void optionalClientAuthAllowsMissingClientCertificate() {
        conf.put("server.tls.need.client.auth", "false");
        conf.put("server.tls.want.client.auth", "true");
        conf.remove("upstream.tls.cert.file.path");
        conf.remove("upstream.tls.key.file.path");
        ProxyDynamicConf.reload();
        handshake(true);
    }

    @Test
    public void appliesProtocolCipherAndSessionCreationConfiguration() {
        conf.put("server.tls.enable.protocols", "TLSv1.2");
        conf.put("server.tls.enable.cipher.suites", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        conf.put("server.tls.enable.session.creation", "true");
        ProxyDynamicConf.reload();
        ServerTlsProvider server = newServerProvider();
        assertTrue(server.init());
        SslHandler handler = server.createSslHandler();
        verifyEngine(handler);
        assertArrayEquals(new String[]{"TLSv1.2"}, handler.engine().getEnabledProtocols());
        assertArrayEquals(new String[]{"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"}, handler.engine().getEnabledCipherSuites());
        assertTrue(handler.engine().getEnableSessionCreation());
        // Check negotiation and encrypted data transfer with these restrictions as well.
        handshake(true);
    }

    @Test
    public void newlyAddedUpstreamMappingWorksWithoutReinitializingProvider() throws Exception {
        Path otherCert = folder.getRoot().toPath().resolve("other.pem");
        generateCertificate(otherCert, folder.getRoot().toPath().resolve("other-key.pem"));
        conf.put("upstream.tls.ca.cert.file.path", otherCert.toString());
        ProxyDynamicConf.reload();
        ServerTlsProvider server = newServerProvider();
        UpstreamTlsProvider client = newUpstreamProvider();
        assertTrue(server.init());
        assertTrue(client.init());
        Resource resource = new Resource("rediss://@127.0.0.1:6379");
        exchange(server.createSslHandler(), client.createSslHandler(resource), false);

        JSONObject mapping = new JSONObject();
        mapping.put("resource", resource.getUrl());
        mapping.put("ca.cert.file.path", cert.toString());
        mapping.put("cert.file.path", cert.toString());
        mapping.put("key.file.path", key.toString());
        mapping.put("password", "test-password");
        JSONArray mappings = new JSONArray();
        mappings.add(mapping);
        conf.put("upstream.tls.config", mappings.toJSONString());
        ProxyDynamicConf.reload();
        exchange(server.createSslHandler(), client.createSslHandler(resource), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingServerCertificateFailsDuringInit() {
        conf.remove("server.tls.cert.file.path");
        ProxyDynamicConf.reload();
        newServerProvider().init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingServerPrivateKeyFailsDuringInit() {
        conf.remove("server.tls.key.file.path");
        ProxyDynamicConf.reload();
        newServerProvider().init();
    }

    @Test(expected = IllegalArgumentException.class)
    public void incorrectPrivateKeyPasswordFailsDuringInit() {
        conf.put("server.tls.password", "wrong-password");
        ProxyDynamicConf.reload();
        newServerProvider().init();
    }
}
