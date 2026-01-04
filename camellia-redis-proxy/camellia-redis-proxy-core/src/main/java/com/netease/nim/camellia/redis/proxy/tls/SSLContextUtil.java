package com.netease.nim.camellia.redis.proxy.tls;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by caojiajun on 2023/8/8
 */
public class SSLContextUtil {

    public static SSLContext genSSLContext(final String caCrtFilePath, final String crtFilePath,
                                           final String keyFilePath, final String password) {
        char[] pass;
        if (password != null) {
            pass = password.toCharArray();
        } else {
            pass = new char[0];
        }
        try (InputStream caInputStream = caCrtFilePath == null ? null : Files.newInputStream(Paths.get(caCrtFilePath));
             InputStream crtInputStream = crtFilePath == null ? null : Files.newInputStream(Paths.get(crtFilePath));
             InputStream keyInputStream = keyFilePath == null ? null : Files.newInputStream(Paths.get(keyFilePath))) {
            Security.addProvider(new BouncyCastleProvider());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //ca
            X509Certificate caCert = null;
            if (caInputStream != null && caInputStream.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(caInputStream);
            }
            //crt
            X509Certificate cert = null;
            if (crtInputStream != null && crtInputStream.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(crtInputStream);
            }
            //key
            PrivateKey key = null;
            if (keyInputStream != null) {
                try (PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream))) {
                    Object object = pemParser.readObject();
                    PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(pass);
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                    if (object instanceof PEMEncryptedKeyPair) {
                        key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    } else if (object instanceof PrivateKeyInfo) {
                        key = converter.getPrivateKey((PrivateKeyInfo) object);
                    } else if (object instanceof PKCS8EncryptedPrivateKeyInfo privateKeyInfo) {
                        InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(pass);
                        key = converter.getPrivateKey(privateKeyInfo.decryptPrivateKeyInfo(provider));
                    } else {
                        key = converter.getKeyPair((PEMKeyPair) object).getPrivate();
                    }
                }
            }

            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            if (caCert != null) {
                caKs.setCertificateEntry("ca-certificate", caCert);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(caKs);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            if (cert != null && key != null) {
                ks.setCertificateEntry("certificate", cert);
                ks.setKeyEntry("private-key", key, pass, new java.security.cert.Certificate[]{cert});
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pass);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
