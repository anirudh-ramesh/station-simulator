package com.evbox.everon.ocpp.simulator.websocket;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SecureOkHttpClient {

    public static OkHttpClient createSecureOkHttpClient(String keyStorePath, String keyStorePassword, String keyManagerPassword) {
        try {
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyManagerPassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, tmf.getTrustManagers(), null);

            return new OkHttpClient.Builder()
                    .addInterceptor(new LoggingInterceptor())
                    .addNetworkInterceptor(new LoggingInterceptor())
                    .pingInterval(10, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .hostnameVerifier((s, sslSession) -> true)
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    private static KeyStore loadKeyStore(String keyStorePath, String keyStorePassword) throws Exception {

        try (InputStream is = Files.newInputStream(Paths.get(keyStorePath))) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, keyStorePassword.toCharArray());
            return loadedKeystore;
        }
    }

}
