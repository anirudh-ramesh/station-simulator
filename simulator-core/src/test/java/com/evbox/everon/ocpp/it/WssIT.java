package com.evbox.everon.ocpp.it;

import com.evbox.everon.ocpp.mock.constants.StationConstants;
import com.evbox.everon.ocpp.mock.csms.OcppMockServer;
import com.evbox.everon.ocpp.mock.csms.OcppServerClient;
import com.evbox.everon.ocpp.mock.csms.SSLContextUtils;
import com.evbox.everon.ocpp.mock.csms.exchange.BootNotification;
import com.evbox.everon.ocpp.mock.factory.SimulatorConfigCreator;
import com.evbox.everon.ocpp.simulator.configuration.SimulatorConfiguration.StationConfiguration;
import com.evbox.everon.ocpp.simulator.station.Station;
import com.evbox.everon.ocpp.simulator.websocket.LoggingInterceptor;
import com.evbox.everon.ocpp.simulator.websocket.SecureOkHttpClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;

import static com.evbox.everon.ocpp.mock.constants.StationConstants.*;
import static com.evbox.everon.ocpp.mock.constants.StationConstants.DEFAULT_EVSE_CONNECTORS;
import static com.evbox.everon.ocpp.v20.message.station.BootNotificationRequest.Reason.POWER_UP;

public class WssIT {

    OcppServerClient ocppServerClient = new OcppServerClient();

    OcppMockServer ocppMockServer = OcppMockServer.builder()
            .hostname(StationConstants.HOST)
            .port(StationConstants.PORT)
            .securePort(SECURE_PORT)
            .path(StationConstants.PATH)
            .ocppServerClient(ocppServerClient)
            .username(STATION_ID)
            .password(BASIC_AUTH_PASSWORD)
            .build();

    StationConfiguration stationConfiguration = SimulatorConfigCreator.createStationConfiguration(STATION_ID, DEFAULT_EVSE_COUNT, DEFAULT_EVSE_CONNECTORS);

    @Test
    void connect() {
        ocppMockServer.start();

        ocppMockServer
                .when(BootNotification.request(POWER_UP))
                .thenReturn(BootNotification.response());

        String keyStore = "keystore.jks";
        String keyStorePassword = "password";
        SSLContext sslContext = SSLContextUtils.createSSLContext(keyStore, keyStorePassword, keyStorePassword);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new LoggingInterceptor())
                .addNetworkInterceptor(new LoggingInterceptor())
                .pingInterval(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.getSocketFactory())
                .hostnameVerifier((s, sslSession) -> true)
                .build();

        Station station = new Station(stationConfiguration, client);

        station.connectToServer(SECURE_OCPP_SERVER_URL);

        station.run();

        ocppMockServer.waitUntilConnected();

    }
}
