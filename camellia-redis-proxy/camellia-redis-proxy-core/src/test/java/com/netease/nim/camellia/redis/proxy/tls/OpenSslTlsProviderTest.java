package com.netease.nim.camellia.redis.proxy.tls;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.tls.frontend.ServerTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.UpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.ReferenceCountedOpenSslEngine;
import io.netty.handler.ssl.SslHandler;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PKCS8Generator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OpenSslTlsProviderTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final Map<String, String> conf = new HashMap<>();
    private Path cert;
    private Path key;
    private Map<String, String> previousConf;
    private ProxyDynamicConfLoader previousLoader;

    @Before
    public void setUp() throws Exception {
        // A missing native library must fail this test, rather than silently testing JSSE.
        OpenSsl.ensureAvailability();
        previousConf = ProxyDynamicConf.getAll();
        previousLoader = ProxyDynamicConf.getConfigLoader();
        Security.addProvider(new BouncyCastleProvider());
        cert = folder.getRoot().toPath().resolve("cert.pem");
        key = folder.getRoot().toPath().resolve("key.pem");
        generateCertificate(cert, key);
        conf.put("server.tls.provider.class.name",
                "com.netease.nim.camellia.redis.proxy.tls.frontend.OpenSslServerTlsProvider");
        conf.put("upstream.tls.provider.class.name",
                "com.netease.nim.camellia.redis.proxy.tls.upstream.OpenSslUpstreamTlsProvider");
        for (String prefix : new String[]{"server", "upstream"}) {
            conf.put(prefix + ".tls.ca.cert.file.path", cert.toString());
            conf.put(prefix + ".tls.cert.file.path", cert.toString());
            conf.put(prefix + ".tls.key.file.path", key.toString());
            conf.put(prefix + ".tls.password", "test-password");
        }
        ProxyDynamicConf.init(null, new ProxyDynamicConfLoader() {
            public void init(Map<String, String> ignored) { }
            public Map<String, String> load() { return new HashMap<>(conf); }
        });
    }

    @After
    public void restoreConfiguration() {
        if (previousLoader != null) {
            ProxyDynamicConf.init(previousConf, previousLoader);
        }
    }

    @Test
    public void classpathCertificateConfigurationTakesPrecedence() throws Exception {
        Path classpath = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        String prefix = "tls-test-" + java.util.UUID.randomUUID();
        Path classpathCert = classpath.resolve(prefix + ".crt");
        Path classpathKey = classpath.resolve(prefix + ".key");
        try {
            Files.copy(cert, classpathCert);
            Files.copy(key, classpathKey);
            for (String side : new String[]{"server", "upstream"}) {
                conf.put(side + ".tls.ca.cert.file", classpathCert.getFileName().toString());
                conf.put(side + ".tls.cert.file", classpathCert.getFileName().toString());
                conf.put(side + ".tls.key.file", classpathKey.getFileName().toString());
                conf.put(side + ".tls.ca.cert.file.path", "missing-ca");
                conf.put(side + ".tls.cert.file.path", "missing-cert");
                conf.put(side + ".tls.key.file.path", "missing-key");
            }
            ProxyDynamicConf.reload();
            handshake(true);
        } finally {
            Files.deleteIfExists(classpathCert);
            Files.deleteIfExists(classpathKey);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedSessionCreationFailsDuringInit() {
        conf.put("server.tls.enable.session.creation", "true");
        ProxyDynamicConf.reload();
        newServerProvider().init();
    }

    @Test
    public void mutualTlsWithEncryptedPrivateKey() {
        handshake(true);
    }

    @Test
    public void oneWayTlsWithoutClientCertificate() {
        conf.put("server.tls.need.client.auth", "false");
        conf.put("server.tls.want.client.auth", "false");
        conf.remove("upstream.tls.cert.file.path");
        conf.remove("upstream.tls.key.file.path");
        ProxyDynamicConf.reload();
        handshake(true);
    }

    @Test
    public void mutualTlsRejectsMissingClientCertificate() {
        conf.remove("upstream.tls.cert.file.path");
        conf.remove("upstream.tls.key.file.path");
        ProxyDynamicConf.reload();
        handshake(false);
    }

    @Test
    public void resourceMappingOverridesDefaultTrustStore() throws Exception {
        Path otherCert = folder.getRoot().toPath().resolve("other.pem");
        generateCertificate(otherCert, folder.getRoot().toPath().resolve("other-key.pem"));
        // Default CA does not trust the server; the resource-specific CA does.
        conf.put("upstream.tls.ca.cert.file.path", otherCert.toString());
        JSONObject mapping = new JSONObject();
        mapping.put("resource", "rediss://@127.0.0.1:6379");
        mapping.put("ca.cert.file.path", cert.toString());
        mapping.put("cert.file.path", cert.toString());
        mapping.put("key.file.path", key.toString());
        mapping.put("password", "test-password");
        JSONArray mappings = new JSONArray();
        mappings.add(mapping);
        conf.put("upstream.tls.config", mappings.toJSONString());
        ProxyDynamicConf.reload();
        handshake(true);
    }

    @Test
    public void rejectsUntrustedServerCertificate() throws Exception {
        Path otherCert = folder.getRoot().toPath().resolve("other.pem");
        generateCertificate(otherCert, folder.getRoot().toPath().resolve("other-key.pem"));
        conf.put("upstream.tls.ca.cert.file.path", otherCert.toString());
        ProxyDynamicConf.reload();
        handshake(false);
    }

    @Test
    public void defaultProvidersStillUseJsse() {
        conf.remove("server.tls.provider.class.name");
        conf.remove("upstream.tls.provider.class.name");
        ProxyDynamicConf.reload();
        ServerTlsProvider server = newServerProvider();
        UpstreamTlsProvider client = newUpstreamProvider();
        assertTrue(server.init());
        assertTrue(client.init());
        SslHandler serverHandler = server.createSslHandler();
        SslHandler clientHandler = client.createSslHandler(new Resource("rediss://@127.0.0.1:6379"));
        assertFalse(serverHandler.engine() instanceof ReferenceCountedOpenSslEngine);
        assertFalse(clientHandler.engine() instanceof ReferenceCountedOpenSslEngine);
        exchange(serverHandler, clientHandler, true);
    }

    // Production's bean factory caches providers. Each test models a fresh process with new certificates.
    private ServerTlsProvider newServerProvider() {
        try {
            return ConfigInitUtil.initProxyFrontendTlsProvider().getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private UpstreamTlsProvider newUpstreamProvider() {
        try {
            return ConfigInitUtil.initProxyUpstreamTlsProvider().getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void handshake(boolean expectedSuccess) {
        ServerTlsProvider server = newServerProvider();
        UpstreamTlsProvider client = newUpstreamProvider();
        assertTrue(server.init());
        assertTrue(client.init());
        SslHandler serverHandler = server.createSslHandler();
        SslHandler clientHandler = client.createSslHandler(new Resource("rediss://@127.0.0.1:6379"));
        assertTrue(serverHandler.engine() instanceof ReferenceCountedOpenSslEngine);
        assertTrue(clientHandler.engine() instanceof ReferenceCountedOpenSslEngine);
        exchange(serverHandler, clientHandler, expectedSuccess);
    }

    private void exchange(SslHandler serverHandler, SslHandler clientHandler, boolean expectedSuccess) {
        EmbeddedChannel server = new EmbeddedChannel(serverHandler);
        EmbeddedChannel client = new EmbeddedChannel(clientHandler);
        try {
            try {
                for (int i = 0; i < 100; i++) {
                    transfer(client, server);
                    transfer(server, client);
                    if (serverHandler.handshakeFuture().isDone() && clientHandler.handshakeFuture().isDone()) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (expectedSuccess) {
                    throw e;
                }
            }
            if (expectedSuccess) {
                assertTrue(String.valueOf(serverHandler.handshakeFuture().cause()), serverHandler.handshakeFuture().isSuccess());
                assertTrue(String.valueOf(clientHandler.handshakeFuture().cause()), clientHandler.handshakeFuture().isSuccess());
                client.writeOutbound(Unpooled.copiedBuffer("PING", StandardCharsets.US_ASCII));
                transfer(client, server);
                ByteBuf received = server.readInbound();
                assertNotNull(received);
                try {
                    assertEquals("PING", received.toString(StandardCharsets.US_ASCII));
                } finally {
                    received.release();
                }
            } else {
                assertTrue((serverHandler.handshakeFuture().isDone() && !serverHandler.handshakeFuture().isSuccess())
                        || (clientHandler.handshakeFuture().isDone() && !clientHandler.handshakeFuture().isSuccess()));
            }
        } finally {
            try {
                client.finishAndReleaseAll();
            } finally {
                server.finishAndReleaseAll();
            }
        }
    }

    private void transfer(EmbeddedChannel from, EmbeddedChannel to) {
        Object message;
        while ((message = from.readOutbound()) != null) {
            to.writeInbound(message);
        }
        to.runPendingTasks();
    }

    private void generateCertificate(Path certFile, Path keyFile) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=localhost");
        long now = System.currentTimeMillis();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
                new Date(now - 60000), new Date(now + 3600000), name, pair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        try (Writer writer = Files.newBufferedWriter(certFile); JcaPEMWriter pem = new JcaPEMWriter(writer)) {
            pem.writeObject(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
        }
        try (Writer writer = Files.newBufferedWriter(keyFile); JcaPEMWriter pem = new JcaPEMWriter(writer)) {
            pem.writeObject(new JcaPKCS8Generator(pair.getPrivate(),
                    new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC).setProvider("BC")
                            .setPassword("test-password".toCharArray()).build()));
        }
    }
}
