package com.truphone.lpad.worker;


import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.util.LogStub;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GetEidLpadWorkerTest {

    private GetEidLpadWorker getIdWorker;

    @Mock
    private Progress mockProgress;
    @Mock
    private ApduChannel mockApduChannel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        getIdWorker = new GetEidLpadWorker(mockProgress, mockApduChannel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenProgressIsNull() {
        new GetEidLpadWorker(null, mockApduChannel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenApduChannelIsNull() {
        new GetEidLpadWorker(mockProgress, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenTransmitAPDUReturnNull() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(null);

        getIdWorker.run(buildStringLpadWorkerExchange(ApduUtils.getEIDApdu()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenTransmitAPDUReturnEmpty() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("");

        getIdWorker.run(buildStringLpadWorkerExchange(ApduUtils.getEIDApdu()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenIdentifierDoesNotMatch() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("A0A40000027F");

        getIdWorker.run(buildStringLpadWorkerExchange(ApduUtils.getEIDApdu()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenDecodeFails() {
        LogStub.getInstance().setLogLevel(Level.FINEST);
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("asdasdasd");

        getIdWorker.run(buildStringLpadWorkerExchange(ApduUtils.getEIDApdu()));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenEidApduIsNull() {
        getIdWorker.run(buildStringLpadWorkerExchange(null));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenEidApduIsEmpty() {
        getIdWorker.run(buildStringLpadWorkerExchange(""));
    }

    @Test
    public void shouldGetEidData() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("bf3e125a10890440500010006800000000000001709000");

        String eidData = getIdWorker.run(buildStringLpadWorkerExchange(ApduUtils.getEIDApdu()));

        assertEquals("89044050001000680000000000000170", eidData);
        verify(mockProgress, times(1))
                .setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.GET_EID_RETRIEVING, "getEID retrieving...");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.GET_EID_CONVERTING, "getEID converting...");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.GET_EID_CONVERTED, "getEID converted...");

    }

    private LpadWorkerExchange<String> buildStringLpadWorkerExchange(final String eidApdu) {
        return new LpadWorkerExchange<>(eidApdu);
    }
}