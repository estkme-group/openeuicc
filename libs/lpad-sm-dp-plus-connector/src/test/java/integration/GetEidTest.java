package integration;


import integration.utils.ReferenceData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.LocalProfileAssistant;
import com.truphone.lpa.impl.LocalProfileAssistantImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class GetEidTest {
    private LocalProfileAssistant localProfileAssistant;

    @Mock
    private ApduChannel mockApduChannel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        localProfileAssistant = new LocalProfileAssistantImpl(this.mockApduChannel);
    }

    @Test
    public void shouldReturnEid() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(ReferenceData.VALID_EID);

        assertEquals("89044050001000680000000000000170", this.localProfileAssistant.getEID());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenApduChannelRespondsWithNonHexadecimalValue() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("asdasd");


        assertEquals("89044050001000680000000000000170", this.localProfileAssistant.getEID());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenApduChannelRespondsWithAnInvalidEID() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("dfcd8f12a77c264a0ce4");

        assertEquals("89044050001000680000000000000170", this.localProfileAssistant.getEID());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenApduChannelRespondsWithNull() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn(null);


        assertEquals("89044050001000680000000000000170", this.localProfileAssistant.getEID());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionWhenApduChannelRespondsWithEmpty() {
        when(mockApduChannel.transmitAPDU(any(String.class)))
                .thenReturn("");


        assertEquals("89044050001000680000000000000170", this.localProfileAssistant.getEID());
    }
}
