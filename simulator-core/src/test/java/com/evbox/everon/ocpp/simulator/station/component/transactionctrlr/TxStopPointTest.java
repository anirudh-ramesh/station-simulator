package com.evbox.everon.ocpp.simulator.station.component.transactionctrlr;

import com.evbox.everon.ocpp.common.OptionList;
import com.evbox.everon.ocpp.simulator.station.StationMessageSender;
import com.evbox.everon.ocpp.simulator.station.StationStore;
import com.evbox.everon.ocpp.simulator.station.evse.StateManager;
import com.evbox.everon.ocpp.simulator.station.evse.ChargingStopReason;
import com.evbox.everon.ocpp.simulator.station.evse.Connector;
import com.evbox.everon.ocpp.simulator.station.evse.Evse;
import com.evbox.everon.ocpp.simulator.station.evse.states.*;
import com.evbox.everon.ocpp.simulator.station.subscription.Subscriber;
import com.evbox.everon.ocpp.v20.message.station.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static com.evbox.everon.ocpp.mock.constants.StationConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TxStopPointTest {

    @Mock
    Connector connectorMock;

    @Mock
    Evse evseMock;

    @Mock
    StationStore stationStoreMock;

    @Mock
    StationMessageSender stationMessageSenderMock;

    @Mock
    StateManager stateManagerMock;

    @Captor
    ArgumentCaptor<Subscriber<StatusNotificationRequest, StatusNotificationResponse>> statusNotificationCaptor;

    @Captor
    ArgumentCaptor<Subscriber<AuthorizeRequest, AuthorizeResponse>> authorizeCaptor;

    @Captor
    ArgumentCaptor<Subscriber<TransactionEventRequest, TransactionEventResponse>> transactionEventCaptor;


    @BeforeEach
    void setUp() {
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);
        when(evseMock.getEvseState()).thenReturn(new ChargingState());
        this.stateManagerMock = new StateManager(null, stationStoreMock, stationMessageSenderMock);
    }

    @Test
    void verifyStopOnlyOnAuthorizedAuthAction() {
        when(evseMock.getId()).thenReturn(DEFAULT_EVSE_ID);
        when(evseMock.unlockConnector()).thenReturn(DEFAULT_CONNECTOR_ID);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.AUTHORIZED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.DE_AUTHORIZED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnAuthorizedUnplugAction() {
        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.AUTHORIZED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnEVConnectedUnplugAction() {
        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.EV_CONNECTED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnEVConnectedAuthAction() {
        when(evseMock.getId()).thenReturn(DEFAULT_EVSE_ID);
        when(evseMock.unlockConnector()).thenReturn(DEFAULT_CONNECTOR_ID);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.EV_CONNECTED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnPowerPathClosedUnplugAction() {
        when(evseMock.getEvseState()).thenReturn(new StoppedState());


        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.POWER_PATH_CLOSED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnPowerPathClosedAuthAction() {
        when(evseMock.getId()).thenReturn(DEFAULT_EVSE_ID);
        when(evseMock.unlockConnector()).thenReturn(DEFAULT_CONNECTOR_ID);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.POWER_PATH_CLOSED )));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
    }

    @Test
    void verifyStopOnlyOnPowerPathClosedAuthUnplugAction() {
        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.POWER_PATH_CLOSED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
        verify(evseMock).setEvseState(argThat(s -> s.getStateName().equals(StoppedState.NAME)));

        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnAuthorizedAndEVConnectedAuthAction() {
        when(evseMock.getId()).thenReturn(DEFAULT_EVSE_ID);
        when(evseMock.unlockConnector()).thenReturn(DEFAULT_CONNECTOR_ID);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.AUTHORIZED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.DE_AUTHORIZED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnAuthorizedAndEVConnectedUnplugAction() {
        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Collections.singletonList(TxStartStopPointVariableValues.AUTHORIZED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
    }

    @Test
    void verifyStartOnAuthorizedAndPowerPathClosedAuthUnplugAction() {
        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Arrays.asList(TxStartStopPointVariableValues.AUTHORIZED, TxStartStopPointVariableValues.POWER_PATH_CLOSED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).createTransaction(anyString());
        verify(evseMock).setEvseState(argThat( s -> s.getStateName().equals(StoppedState.NAME)));

        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnAuthorizedAndEVConnectedAndPowerPathClosedUnplugAction() {
        when(evseMock.getEvseState()).thenReturn(new StoppedState());

        when(evseMock.findConnector(anyInt())).thenReturn(connectorMock);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Arrays.asList(TxStartStopPointVariableValues.AUTHORIZED, TxStartStopPointVariableValues.EV_CONNECTED, TxStartStopPointVariableValues.POWER_PATH_CLOSED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.cableUnplugged(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID);
        verify(stationMessageSenderMock).sendStatusNotificationAndSubscribe(any(), any(), statusNotificationCaptor.capture());
        statusNotificationCaptor.getValue().onResponse(new StatusNotificationRequest(), new StatusNotificationResponse());

        verify(stationMessageSenderMock, times(1)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.EV_DEPARTED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(1)).stopTransaction();
    }

    @Test
    void verifyStopOnAuthorizedAndEVConnectedAndPowerPathClosedAuthAction() {
        when(evseMock.getId()).thenReturn(DEFAULT_EVSE_ID);
        when(evseMock.unlockConnector()).thenReturn(DEFAULT_CONNECTOR_ID);
        when(stationStoreMock.getTxStopPointValues()).thenReturn(new OptionList<>(Arrays.asList(TxStartStopPointVariableValues.AUTHORIZED, TxStartStopPointVariableValues.EV_CONNECTED, TxStartStopPointVariableValues.POWER_PATH_CLOSED)));
        when(stationStoreMock.findEvse(anyInt())).thenReturn(evseMock);

        stateManagerMock.authorized(DEFAULT_EVSE_ID, DEFAULT_TOKEN_ID);
        verify(stationMessageSenderMock).sendAuthorizeAndSubscribe(anyString(), anyList(), authorizeCaptor.capture());
        authorizeCaptor.getValue().onResponse(new AuthorizeRequest(), new AuthorizeResponse().withIdTokenInfo(new IdTokenInfo().withStatus(IdTokenInfo.Status.ACCEPTED)).withEvseId(Collections.singletonList(DEFAULT_EVSE_ID)));

        verify(stationMessageSenderMock, times(0)).sendTransactionEventEnded(DEFAULT_EVSE_ID, DEFAULT_CONNECTOR_ID, TransactionEventRequest.TriggerReason.STOP_AUTHORIZED, TransactionData.StoppedReason.EV_DISCONNECTED);
        verify(evseMock, times(0)).stopTransaction();
    }

}