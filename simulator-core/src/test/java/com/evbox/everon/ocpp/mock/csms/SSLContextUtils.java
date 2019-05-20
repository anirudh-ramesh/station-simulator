package com.evbox.everon.ocpp.mock.csms;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class SSLContextUtils {

    public static SSLContext createSSLContext(String keyStorePath, String keyStorePassword, String keyManagerPassword) {
        try {
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyManagerPassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, tmf.getTrustManagers(), null);

            return sslContext;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static KeyStore loadKeyStore(String keyStore, String keyStorePassword) throws Exception {

        try (InputStream is = SSLContextUtils.class.getClassLoader().getResourceAsStream(keyStore)) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, keyStorePassword.toCharArray());
            return loadedKeystore;
        }
    }

}
