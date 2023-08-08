package com.netease.nim.camellia.tools.ssl;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
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
        try (InputStream caInputStream = Files.newInputStream(Paths.get(caCrtFilePath));
             InputStream crtInputStream = Files.newInputStream(Paths.get(crtFilePath));
             InputStream keyInputStream = Files.newInputStream(Paths.get(keyFilePath))) {
            Security.addProvider(new BouncyCastleProvider());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //ca
            X509Certificate caCert = null;
            if (caInputStream.available() > 0) {
                caCert = (X509Certificate) cf.generateCertificate(caInputStream);
            }
            //crt
            X509Certificate cert = null;
            if (crtInputStream.available() > 0) {
                cert = (X509Certificate) cf.generateCertificate(crtInputStream);
            }
            //key
            KeyPair key;
            try (PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream))) {
                Object object = pemParser.readObject();
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(pass);
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                if (object instanceof PEMEncryptedKeyPair) {
                    key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
                } else {
                    key = converter.getKeyPair((PEMKeyPair) object);
                }
            }

            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", caCert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(caKs);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("certificate", cert);
            ks.setKeyEntry("private-key", key.getPrivate(), pass, new java.security.cert.Certificate[]{cert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pass);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getFilePath(String file) {
        URL resource = SSLContextUtil.class.getClassLoader().getResource(file);
        if (resource == null) {
            throw new IllegalArgumentException(file + " not found");
        }
        return resource.getPath();
    }
}
