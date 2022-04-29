package com.truphone.es9plus;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.truphone.es9plus.message.request.AuthenticateClientReq;
import com.truphone.es9plus.message.request.GetBoundProfilePackageReq;
import com.truphone.es9plus.message.request.HandleNotificationReq;
import com.truphone.es9plus.message.request.InitiateAuthenticationReq;
import com.truphone.es9plus.message.response.AuthenticateClientResp;
import com.truphone.es9plus.message.response.GetBoundProfilePackageResp;
import com.truphone.es9plus.message.response.InitiateAuthenticationResp;
import com.truphone.util.LogStub;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Es9PlusImpl {
    private static final Gson GS = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOG = Logger.getLogger(Es9PlusImpl.class.getName());
    private static final String INITIATE_AUTHENTICATION_PATH = "/gsma/rsp2/es9plus/initiateAuthentication";
    private static final String AUTHENTICATE_CLIENT_PATH = "/gsma/rsp2/es9plus/authenticateClient";
    private static final String GET_BOUND_PROFILE_PACKAGE_PATH = "/gsma/rsp2/es9plus/getBoundProfilePackage";
    private static final String HANDLE_NOTIFICATION_PATH = "/gsma/rsp2/es9plus/handleNotification";
    //private static final String ALLOCATE_PROFILE_PATH = "/custom/profile/";

//    private String rspServerUrl;

//    public void configure(String rspServerUrl) {
//
//        this.rspServerUrl = rspServerUrl;
//    }

    public InitiateAuthenticationResp initiateAuthentication(final String euiccChallenge,
                                                             final String euiccInfo1,
                                                             final String smdpAddress) {
        try {
            InitiateAuthenticationReq initiateAuthenticationReq = new InitiateAuthenticationReq();
            initiateAuthenticationReq.setEuiccChallenge(euiccChallenge);
            initiateAuthenticationReq.setEuiccInfo1(euiccInfo1);
            initiateAuthenticationReq.setSmdpAddress(smdpAddress);
            String body = GS.toJson(initiateAuthenticationReq);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, "RSP Request: " + body);
            }

            HttpResponse result = new HttpRSPClient().clientRSPRequest(body, "https://"+smdpAddress, INITIATE_AUTHENTICATION_PATH);
            if (result != null && !"".equals(result.getContent())) {
                String response = toJsonString(result.getContent());
                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, "RSP Response: " + response);
                }
                return GS.fromJson(response, InitiateAuthenticationResp.class);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error contacting RSP Server", e);

            throw new RuntimeException("Unable to communicate with RSP Server");
        }
        return null;
    }

    public AuthenticateClientResp authenticateClient(final String transactionId,
                                                     final String authenticateServerResponse,
                                                     final String smdpAddress) {
        try {
            AuthenticateClientReq authenticateClientReq = new AuthenticateClientReq();
            authenticateClientReq.setTransactionId(transactionId);
            authenticateClientReq.setAuthenticateServerResponse(authenticateServerResponse);
            String body = GS.toJson(authenticateClientReq);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, "RSP Request: " + body);
            }

            HttpResponse result = new HttpRSPClient().clientRSPRequest(body, "https://" + smdpAddress, AUTHENTICATE_CLIENT_PATH);
            if (result != null && !"".equals(result.getContent())) {
                String response = toJsonString(result.getContent());

                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, "RSP Response: " + response);
                }

                return GS.fromJson(response, AuthenticateClientResp.class);
            } else {
                LOG.severe("Error contacting RSP Server");

                throw new RuntimeException("Unable to communicate with RSP Server");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error contacting RSP Server", e);

            throw new RuntimeException("Unable to communicate with RSP Server");
        }
    }

    public GetBoundProfilePackageResp getBoundProfilePackage(final String transactionId,
                                                             final String prepareDownloadResponse,
                                                             final String smdpAddress) {
        try {
            GetBoundProfilePackageReq getBoundProfilePackageReq = new GetBoundProfilePackageReq();
            getBoundProfilePackageReq.setTransactionId(transactionId);
            getBoundProfilePackageReq.setPrepareDownloadResponse(prepareDownloadResponse);
            String body = GS.toJson(getBoundProfilePackageReq);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, "RSP Request: " + body);
            }

            HttpResponse result = new HttpRSPClient().clientRSPRequest(body, "https://" + smdpAddress, GET_BOUND_PROFILE_PACKAGE_PATH);
            if (result != null && !"".equals(result.getContent())) {
                String response = toJsonString(result.getContent());

                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, "RSP Response: " + response);
                }

                return GS.fromJson(response, GetBoundProfilePackageResp.class);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error contacting RSP Server", e);

            throw new RuntimeException("Unable to communicate with RSP Server");
        }

        return null;
    }

    /**
     * ES9+.handleNotification
     */
    public void handleNotification(final String pendingNotification, String serverAddress) {
        try {
            HandleNotificationReq handleNotificationReq = new HandleNotificationReq();

            handleNotificationReq.setPendingNotification(pendingNotification);

            String body = GS.toJson(handleNotificationReq);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, "RSP Request: " + body);
            }
            
            
            HttpResponse result = new HttpRSPClient().clientRSPRequest(body, "https://"+serverAddress, HANDLE_NOTIFICATION_PATH);

            if (result != null && result.getStatusCode() == 204) {
                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, "RSP Response was 204 ");
                }

            } else {
                LOG.severe("Error contacting RSP Server or not 204: " + result);

                throw new RuntimeException("Unable to handle notification with RSP Server");
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error contacting RSP Server", e);

            throw new RuntimeException("Unable to handle notification with RSP Server");
        }
    }

//    public AllocateProfileResponse allocateProfile(final String eid,
//                                                   final String mcc) {
//
//        try {
//            String body = "eid=" + eid + "&mcc=" + mcc;
//
//            if (LogStub.getInstance().isDebugEnabled()) {
//                LogStub.getInstance().logDebug(LOG, "RSP Request: " + body);
//            }
//
//            HttpResponse result = new HttpRSPClient().clientSimpleRequest(body, rspServerUrl, ALLOCATE_PROFILE_PATH);
//
//            if (result != null && !"".equals(result.getContent())) {
//                if (LogStub.getInstance().isDebugEnabled()) {
//                    LogStub.getInstance().logDebug(LOG, "RSP Response: " + result);
//                }
//
//                return getAllocateProfileResponse(result.getContent());
//            } else {
//                throw new RuntimeException("No profile could be allocated");
//            }
//        } catch (Exception e) {
//            LOG.log(Level.SEVERE, e.getMessage(), e);
//
//            throw new RuntimeException("Unable to allocate profile with RSP Server");
//        }
//    }

    private AllocateProfileResponse getAllocateProfileResponse(final String content) {
        AllocateProfileResponse allocateProfileResponse = null;
        String fixedContent = content != null ? (content.startsWith("$") ? content.substring(1) : content) : "";
        String[] responseTokens = fixedContent.split("\\$");

        if (responseTokens.length > 1) {
            allocateProfileResponse = new AllocateProfileResponse();
            allocateProfileResponse.setAcFormat(responseTokens[0]);
            allocateProfileResponse.setSmDpPlusAddress(responseTokens[1]);
            allocateProfileResponse.setAcToken(responseTokens.length > 2 ? responseTokens[2] : "");
            allocateProfileResponse.setSmDpPlusOid(responseTokens.length > 3 ? responseTokens[3] : null);
            allocateProfileResponse.setConfirmationCodeRequiredFlag(responseTokens.length > 4 ? responseTokens[4] : null);
        }

        return allocateProfileResponse;
    }

    private String toJsonString(final String msg) {
        int index = msg.indexOf("{");

        return msg.substring(index);
    }
}