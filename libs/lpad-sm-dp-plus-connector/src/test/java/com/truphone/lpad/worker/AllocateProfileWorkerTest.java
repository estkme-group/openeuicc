package com.truphone.lpad.worker;


import com.truphone.es9plus.AllocateProfileResponse;
import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.util.LogStub;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class AllocateProfileWorkerTest {

//    private AllocateProfileWorker allocateProfileWorker;
//
//    @Mock
//    private Progress mockProgress;
//    @Mock
//    private Es9PlusImpl mockEs9PlusImpl;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        allocateProfileWorker = new AllocateProfileWorker(mockProgress, mockEs9PlusImpl);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionProgressIsNull() {
//        new AllocateProfileWorker(null, mockEs9PlusImpl);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenRsp29ModuleIsNull() {
//        new AllocateProfileWorker(mockProgress, null);
//    }
//
//    @Test
//    public void shouldAllocateAProfileForAValidMcc() {
//
////        String mcc = "351";
////        String eid = "2";
////        String acToken = "token";
////
////        AllocateProfileResponse mockAllocateProfileResponse = mock(AllocateProfileResponse.class);
////        when(mockAllocateProfileResponse.getAcToken())
////                .thenReturn(acToken);
////
////        when(mockEs9PlusImpl.allocateProfile(eid, mcc))
////                .thenReturn(mockAllocateProfileResponse);
////
////        String run = allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams(mcc, eid)));
////
////        assertEquals(acToken, run);
////        verify(mockProgress, times(1))
////                .setTotalSteps(2);
////        verify(mockProgress, times(1))
////                .stepExecuted(ProgressStep.ALLOCATE_PROFILE_ALLOCATING, "allocateProfile allocating...");
////        verify(mockProgress, times(1))
////                .stepExecuted(ProgressStep.ALLOCATE_PROFILE_ALLOCATED, "allocateProfile allocated!");
//    }
//
//    @Test(expected = RuntimeException.class)
//    public void shouldThrowRuntimeExceptionWhenRspEs29ModuleReturnsANullAllocateProfileResponse() {
//        LogStub.getInstance().setLogLevel(Level.FINEST);
//
//        String mcc = "351";
//        String eid = "2";
//
//        when(mockEs9PlusImpl.allocateProfile(eid, mcc))
//                .thenReturn(null);
//
//        allocateProfileWorker
//                = new AllocateProfileWorker(mockProgress, mockEs9PlusImpl);
//
//        allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams(mcc, eid)));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenAllocateProfileInputParamsIsNull() {
//        allocateProfileWorker.run(null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenMccIsNull() {
//
//        allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams(null, "eid")));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenMccIsEmpty() {
//
//        allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams("", "eid")));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenEidIsNull() {
//
//        allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams("mcc", null)));
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void shouldThrowIllegalArgumentExceptionWhenEidIsEmpty() {
//
//        allocateProfileWorker.run(buildLpadWorkerExchange(buildAllocateProfileInputParams("mcc", "")));
//    }
//
//    private AllocateProfileWorker.AllocateProfileInputParams buildAllocateProfileInputParams(final String mcc, final String eid) {
//        return allocateProfileWorker. new AllocateProfileInputParams(mcc, eid);
//    }
//
//    private LpadWorkerExchange<AllocateProfileWorker.AllocateProfileInputParams> buildLpadWorkerExchange(final AllocateProfileWorker.AllocateProfileInputParams allocateProfileInputParams) {
//        return new LpadWorkerExchange<>(allocateProfileInputParams);
//    }
}