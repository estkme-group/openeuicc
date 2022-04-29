package integration;


import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.LocalProfileAssistant;
import com.truphone.lpa.impl.LocalProfileAssistantImpl;
import com.truphone.lpad.progress.Progress;
import com.truphone.util.ToTLV;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DeleteProfileTest {
    private LocalProfileAssistant localProfileAssistant;

    @Mock
    private ApduChannel mockApduChannel;

    @Mock
    private Progress mockProgress;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        localProfileAssistant = new LocalProfileAssistantImpl(this.mockApduChannel);
    }

    @Test
    public void shouldResponseOk() {
        String response = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "00"));
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(response);

        assertEquals("0", this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress));
    }

    @Test
    public void shouldFailedIccidOrAidNotFound() {
        String response = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "01"));
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(response);

        assertEquals("1", this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress));
    }

    @Test
    public void shouldFailedProfileNotInDisabledState() {
        String response = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "02"));
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(response);

        assertEquals("2", this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress));
    }

    @Test
    public void shouldFailedDisallowedByPolicy() {
        String response = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "03"));
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(response);

        assertEquals("3", this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress));
    }

    @Test
    public void shouldFailedUndefinedError() {
        String response = ToTLV.toTLV("BF33", ToTLV.toTLV("80", "7F"));
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(response);

        assertEquals("127", this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress));
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailedReturnStringWrong() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("randomstring");

        this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress);
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailedReturnEmptyString() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("");

        this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress);
    }


    @Test(expected = RuntimeException.class)
    public void shouldFailedReturnNullString() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(null);

        this.localProfileAssistant.deleteProfile("89445035401458888888", mockProgress);
    }

}
