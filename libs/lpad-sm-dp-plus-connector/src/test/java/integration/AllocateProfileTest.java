package integration;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import integration.utils.ReferenceData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.LocalProfileAssistant;
import com.truphone.lpa.impl.LocalProfileAssistantImpl;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class AllocateProfileTest {
    private LocalProfileAssistant localProfileAssistant;

//    @Mock
//    private ApduChannel mockApduChannel;
//
//    @ClassRule
//    public static WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().port(8090).httpsPort(8443).notifier(new ConsoleNotifier(true)));
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//
//        localProfileAssistant = new LocalProfileAssistantImpl(this.mockApduChannel);
//    }
//
//    @Test
//    public void shouldReturnAcToken() {
//        wireMockRule.stubFor(post(urlMatching("/custom/profile/"))
//                .withHeader("Content-type", equalTo("application/x-www-form-urlencoded"))
//                .withHeader("User-Agent", equalTo("gsma-rsp-com.truphone.lpad"))
//                .withHeader("X-Admin-Protocol", equalTo("gsma/rsp/v2.2.0"))
//                .withRequestBody(containing("eid=89044050001000680000000000000170&mcc=351"))
//                .willReturn(aResponse().withStatus(200).withBody("$1$rsp.truphone.com$2")));
//
//        when(mockApduChannel.transmitAPDU(anyString()))
//                .thenReturn(ReferenceData.VALID_EID);
//
//        Assert.assertEquals("2", localProfileAssistant.allocateProfile("351"));
//    }
//
//    @Test(expected = RuntimeException.class)
//    public void shouldThrowRuntimeExceptionWhenRspServerRespondesWithEmpty() {
//        wireMockRule.stubFor(post(urlMatching("/custom/profile/"))
//                .withHeader("Content-type", equalTo("application/x-www-form-urlencoded"))
//                .withHeader("User-Agent", equalTo("gsma-rsp-com.truphone.lpad"))
//                .withHeader("X-Admin-Protocol", equalTo("gsma/rsp/v2.2.0"))
//                .withRequestBody(containing("eid=89044050001000680000000000000170&mcc=351"))
//                .willReturn(aResponse().withStatus(200).withBody("x")));
//
//        when(mockApduChannel.transmitAPDU(anyString()))
//                .thenReturn(ReferenceData.VALID_EID);
//
//        localProfileAssistant.allocateProfile("351");
//    }
//
//
//    @Test(expected = RuntimeException.class)
//    public void shouldThrowRuntimeExceptionWhenRspServerRespondesWithStatusDifferentOf2xx() {
//        wireMockRule.stubFor(post(urlMatching("/custom/profile/"))
//                .withHeader("Content-type", equalTo("application/x-www-form-urlencoded"))
//                .withHeader("User-Agent", equalTo("gsma-rsp-com.truphone.lpad"))
//                .withHeader("X-Admin-Protocol", equalTo("gsma/rsp/v2.2.0"))
//                .withRequestBody(containing("eid=89044050001000680000000000000170&mcc=351"))
//                .willReturn(aResponse().withStatus(400).withBody("x")));
//
//        when(mockApduChannel.transmitAPDU(anyString()))
//                .thenReturn(ReferenceData.VALID_EID);
//
//        localProfileAssistant.allocateProfile("351");
//    }
}