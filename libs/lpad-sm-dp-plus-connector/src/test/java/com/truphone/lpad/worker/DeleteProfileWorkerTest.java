package com.truphone.lpad.worker;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.util.ToTLV;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DeleteProfileWorkerTest {

    private DeleteProfileWorker deleteProfileWorker;

    @Mock
    private Progress mockProgress;
    @Mock
    private ApduChannel mockApduChannel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        deleteProfileWorker = new DeleteProfileWorker(mockProgress, mockApduChannel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionProgressIsNull() {
        new DeleteProfileWorker(null, mockApduChannel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenApduChannelIsNull() {
        new DeleteProfileWorker(mockProgress, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenDeleteProfileInputParamsIsNull() {
        deleteProfileWorker.run(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenDeleteProfileInputParamsBodyIsNull() {
        deleteProfileWorker.run(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenICCIDIsNull() {
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenICCIDIsEmpty() {
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams("")));
    }

    @Test(expected = RuntimeException.class)
    public void theTransmissionFailedResponseWrongContent(){
        String iccid = "89445035401458888888";
        String eResponse = "TEST";
        when(mockApduChannel.transmitAPDU(anyString())).thenReturn(eResponse);

        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test(expected = RuntimeException.class)
    public void theTransmissionFailedResponseEmpty(){
        String iccid = "89445035401458888888";
        String eResponse = "";

        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);

        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test
    public void theTransmissionResponseOk(){
        String iccid = "89445035401458888888";
        String eResponse = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "00"));

        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockApduChannel, times(1)).sendStatus();

        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test
    public void theTransmissionFailedIccidOrAidNotFound(){
        String iccid = "89445035401458888888";
        String eResponse = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "01"));

        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockApduChannel, times(0)).sendStatus();

        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test
    public void theTransmissionFailedProfileNotInDisabledState(){
        String iccid = "89445035401458888888";
        String eResponse = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "02"));

        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockApduChannel, times(0)).sendStatus();

        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test
    public void theTransmissionFailedDisallowedByPolicy(){
        String iccid = "89445035401458888888";
        String eResponse = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "03"));

        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockApduChannel, times(0)).sendStatus();

        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    @Test
    public void theTransmissionFailedUndefinedError(){
        String iccid = "89445035401458888888";
        String eResponse = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "7F")); //7F
        when(mockApduChannel.transmitAPDU(any(String.class))).thenReturn(eResponse);
        deleteProfileWorker.run(buildLpadWorkerExchange(buildDeleteProfileInputParams(iccid)));

        verify(mockProgress, times(1)).setTotalSteps(3);
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");
        verify(mockApduChannel, times(0)).sendStatus();

        verify(mockProgress, times(0))
                .stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
        verify(mockProgress, times(1))
                .stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");
    }

    private DeleteProfileWorker.DeleteProfileInputParams buildDeleteProfileInputParams(final String iccid) {
        return deleteProfileWorker. new DeleteProfileInputParams(iccid);
    }

    private LpadWorkerExchange<DeleteProfileWorker.DeleteProfileInputParams> buildLpadWorkerExchange(final DeleteProfileWorker.DeleteProfileInputParams deleteProfileInputParams) {
        return new LpadWorkerExchange<>(deleteProfileInputParams);
    }


}