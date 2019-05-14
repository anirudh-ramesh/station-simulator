package com.evbox.everon.ocpp.simulator.websocket;

import com.evbox.everon.ocpp.simulator.configuration.SimulatorConfiguration;
import okhttp3.*;
import okio.ByteString;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Optional;

public class OkHttpWebSocketClient {

    private static final String COLON = ":";

    private final OkHttpClient client;
    private final SimulatorConfiguration.StationConfiguration stationConfiguration;
    private WebSocket webSocket;
    private ChannelListener listener;

    public OkHttpWebSocketClient(OkHttpClient client, SimulatorConfiguration.StationConfiguration stationConfiguration) {
        this.client = client;
        this.stationConfiguration = stationConfiguration;
    }

    public void setListener(ChannelListener listener) {
        this.listener = listener;
    }

    public void connect(String url) {

        String plainCredentials = stationConfiguration.getUsername() + COLON + stationConfiguration.getPassword();

        Request request = new Request.Builder().url(url)
                .addHeader("Sec-WebSocket-Protocol", "ocpp2.0")
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(plainCredentials.getBytes()))
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                listener.onOpen(response.toString());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                listener.onMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                listener.onMessage(new String(bytes.toByteArray()));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                listener.onClosing(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                listener.onClosing(code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                listener.onFailure(t, Optional.ofNullable(response).map(Response::toString).orElse(""));
            }
        });
    }

    public void disconnect() {
        webSocket.close(1000, "Simulator goes offline");
    }

    public boolean sendMessage(String message) {
        return webSocket.send(message);
    }
}
