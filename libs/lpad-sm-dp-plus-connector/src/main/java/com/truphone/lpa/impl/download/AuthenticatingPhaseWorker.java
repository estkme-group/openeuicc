package com.truphone.lpa.impl.download;


import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.es9plus.LpaUtils;
import com.truphone.es9plus.message.response.AuthenticateClientResp;
import com.truphone.es9plus.message.response.InitiateAuthenticationResp;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.impl.AuthenticateClientSmDp;
import com.truphone.lpa.impl.InitialAuthenticationKeys;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpa.progress.DownloadProgressPhase;
import com.truphone.rsp.dto.asn1.rspdefinitions.GetEuiccChallengeResponse;
import com.truphone.util.LogStub;
import com.truphone.util.Util;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.truphone.lpad.progress.ProgressStep.*;

public class AuthenticatingPhaseWorker {
    private static final Logger LOG = Logger.getLogger(AuthenticatingPhaseWorker.class.getName());

    private final DownloadProgress progress;
    private final ApduTransmitter apduTransmitter;
    private final Es9PlusImpl es9Module;

    public AuthenticatingPhaseWorker(DownloadProgress progress, ApduTransmitter apduTransmitter, Es9PlusImpl es9Module) {

        this.progress = progress;
        this.apduTransmitter = apduTransmitter;
        this.es9Module = es9Module;
    }

    public String getEuiccInfo() {

        progress.setCurrentPhase(DownloadProgressPhase.AUTHENTICATING);
        progress.stepExecuted(DOWNLOAD_PROFILE_GET_EUICC_INFO, "getEuiccInfo retrieving...");

        return convertEuiccInfo1(apduTransmitter.transmitApdu(ApduUtils.getEuiccInfo1Apdu()));
    }

    private String convertEuiccInfo1(String euicInfo1APDUResponse) {
        String euiccInfo1;

        progress.stepExecuted(DOWNLOAD_PROFILE_CONVERTING_EUICC_INFO, "getEUICCInfo1 Success!");

        euiccInfo1 = euicInfo1APDUResponse.toUpperCase();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - EUICC Info Object: " + euiccInfo1);
        }

        euiccInfo1 = Base64.encodeBase64String(Util.hexStringToByteArray(euiccInfo1));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - EUICC Info Object (Base 64): " + euiccInfo1);
        }

        progress.stepExecuted(DOWNLOAD_PROFILE_CONVERTED_EUICC_INFO, "getEUICCInfo1 Success!");

        return euiccInfo1;
    }

    public String getEuiccChallenge(String matchingId) {

        progress.stepExecuted(DOWNLOAD_PROFILE_GET_EUICC_CHALLENGE, "getEuiccChallenge retrieving...");

        return convertEuiccChallenge(apduTransmitter.transmitApdu(ApduUtils.getEUICCChallengeApdu()), matchingId);
    }

    private String convertEuiccChallenge(String euiccChallengeApduResponse, String matchingId) {
        String euiccChallenge;

        progress.stepExecuted(DOWNLOAD_PROFILE_CONVERTING_EUICC_CHALLENGE, "convertEuiccChallenge converting...");

        try {
            euiccChallenge = Base64.encodeBase64String(Util.hexStringToByteArray(decodeGetEuiccChallengeResponse(euiccChallengeApduResponse)));

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - eUICCChallenge is " + euiccChallenge);
            }
        } catch (DecoderException e) {
            LOG.log(Level.SEVERE, "KOL.007" + e.getMessage(), e);
            LOG.severe(LogStub.getInstance().getTag() + " - matchingId: " + matchingId +
                    " Unable to retrieve eUICC challenge. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve eUICC challenge: " + matchingId);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "KOL.007" + ioe.getMessage(), ioe);
            LOG.severe(LogStub.getInstance().getTag() + " - matchingId: " + matchingId +
                    " Unable to retrieve eUICC challenge. IOException:" + ioe.getMessage());

            throw new RuntimeException("Unable to retrieve eUICC challenge");
        }

        progress.stepExecuted(DOWNLOAD_PROFILE_CONVERTED_EUICC_CHALLENGE, "convertEuiccChallenge converted...");

        return euiccChallenge;
    }

    private String decodeGetEuiccChallengeResponse(String euiccChallengeApduResponse) throws DecoderException, IOException {
        InputStream is = null;

        try {
            GetEuiccChallengeResponse euiccChallengeResponse = new GetEuiccChallengeResponse();

            is = new ByteArrayInputStream(Hex.decodeHex(euiccChallengeApduResponse.toCharArray()));

            euiccChallengeResponse.decode(is);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Decoded euiccChallengeResponse: " + euiccChallengeResponse.toString());
            }

            return euiccChallengeResponse.getEuiccChallenge().toString();
        } finally {
            CloseResources.closeResources(is);
        }
    }

    public void initiateAuthentication(InitialAuthenticationKeys initialAuthenticationKeys) {

        progress.stepExecuted(DOWNLOAD_PROFILE_INITIATE_AUTHENTICATION, "initiateAuthentication retrieving...");

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Initiating Auth with SM-DP+");
        }

        InitiateAuthenticationResp initiateAuthenticationResp = getInitiateAuthenticationResp(initialAuthenticationKeys);

        setServerSigned1(initialAuthenticationKeys, initiateAuthenticationResp);
        setServerSignature1(initialAuthenticationKeys, initiateAuthenticationResp);
        setEuiccCiPKIdToveUsed(initialAuthenticationKeys, initiateAuthenticationResp);
        setServerCertificate(initialAuthenticationKeys, initiateAuthenticationResp);
        setTransactionId(initialAuthenticationKeys, initiateAuthenticationResp);
        setMatchingId(initialAuthenticationKeys);
        setCtxParams1(initialAuthenticationKeys);

        progress.stepExecuted(DOWNLOAD_PROFILE_INITIATED_AUTHENTICATION, "initiateAuthentication initiated...");
    }

    private void setCtxParams1(InitialAuthenticationKeys initialAuthenticationKeys) {

        initialAuthenticationKeys.setCtxParams1(LpaUtils.generateCtxParams1());

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - ctxParams1: " + initialAuthenticationKeys.getCtxParams1());
        }
    }

    private void setMatchingId(InitialAuthenticationKeys initialAuthenticationKeys) {

        initialAuthenticationKeys.setMatchingId(Util.ASCIIToHex(initialAuthenticationKeys.getMatchingId()));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - matchingId: " + initialAuthenticationKeys.getMatchingId());
        }
    }

    private void setTransactionId(InitialAuthenticationKeys initialAuthenticationKeys, InitiateAuthenticationResp initiateAuthenticationResp) {

        initialAuthenticationKeys.setTransactionId(initiateAuthenticationResp.getTransactionId());

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - transactionId: " + initialAuthenticationKeys.getTransactionId());
        }
    }

    private void setServerCertificate(InitialAuthenticationKeys initialAuthenticationKeys, InitiateAuthenticationResp initiateAuthenticationResp) {

        initialAuthenticationKeys.setServerCertificate(initiateAuthenticationResp.getServerCertificate());
        initialAuthenticationKeys.setServerCertificate(Util.byteArrayToHexString(Base64.decodeBase64(initialAuthenticationKeys.getServerCertificate()), ""));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - serverCertificate: " + initialAuthenticationKeys.getServerCertificate());
        }
    }

    private void setEuiccCiPKIdToveUsed(InitialAuthenticationKeys initialAuthenticationKeys, InitiateAuthenticationResp initiateAuthenticationResp) {

        initialAuthenticationKeys.setEuiccCiPKIdTobeUsed(initiateAuthenticationResp.getEuiccCiPKIdToBeUsed());
        initialAuthenticationKeys.setEuiccCiPKIdTobeUsed(Util.byteArrayToHexString(Base64.decodeBase64(initialAuthenticationKeys.getEuiccCiPKIdTobeUsed()), ""));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - euiccCiPKIdTobeUsed: " + initialAuthenticationKeys.getEuiccCiPKIdTobeUsed());
        }
    }

    private void setServerSignature1(InitialAuthenticationKeys initialAuthenticationKeys, InitiateAuthenticationResp initiateAuthenticationResp) {

        initialAuthenticationKeys.setServerSignature1(initiateAuthenticationResp.getServerSignature1());
        initialAuthenticationKeys.setServerSignature1(Util.byteArrayToHexString(Base64.decodeBase64(initialAuthenticationKeys.getServerSignature1()), ""));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - serverSignature1: " + initialAuthenticationKeys.getServerSignature1());
        }
    }

    private void setServerSigned1(InitialAuthenticationKeys initialAuthenticationKeys, InitiateAuthenticationResp initiateAuthenticationResp) {

        initialAuthenticationKeys.setServerSigned1(initiateAuthenticationResp.getServerSigned1());
        initialAuthenticationKeys.setServerSigned1(Util.byteArrayToHexString(Base64.decodeBase64(initialAuthenticationKeys.getServerSigned1()), ""));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - serverSigned1: " + initialAuthenticationKeys.getServerSigned1());
        }
    }

    private InitiateAuthenticationResp getInitiateAuthenticationResp(InitialAuthenticationKeys initialAuthenticationKeys) {
        InitiateAuthenticationResp initiateAuthenticationResp = es9Module.initiateAuthentication(initialAuthenticationKeys.getEuiccChallenge(),
                initialAuthenticationKeys.getEuiccInfo1(), initialAuthenticationKeys.getEuiccConfiguredAddress());

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Initiating Auth with SM-DP+ - Response: " + initiateAuthenticationResp);
        }

        return initiateAuthenticationResp;
    }

    public AuthenticateClientSmDp authenticateClient(InitialAuthenticationKeys initialAuthenticationKeys, String encodedAuthenticateServerResponse) {

        progress.stepExecuted(DOWNLOAD_PROFILE_AUTHENTICATE_CLIENT, "authenticateClient retrieving...");

        AuthenticateClientResp authenticateClientResp = getAuthenticateClientResponse(initialAuthenticationKeys, encodedAuthenticateServerResponse);
        AuthenticateClientSmDp authenticateClientSmDp = convertAuthenticateClientResp(authenticateClientResp);

        progress.stepExecuted(DOWNLOAD_PROFILE_AUTHENTICATED_CLIENT, "authenticateClient authenticated...");

        return authenticateClientSmDp;
    }

    private AuthenticateClientSmDp convertAuthenticateClientResp(AuthenticateClientResp authenticateClientResp) {
        AuthenticateClientSmDp authenticateClientSmDp = new AuthenticateClientSmDp();

        authenticateClientSmDp.setSmdpSigned2(Util.byteArrayToHexString(Base64.decodeBase64(authenticateClientResp.getSmdpSigned2()), ""));
        authenticateClientSmDp.setSmdpSignature2(Util.byteArrayToHexString(Base64.decodeBase64(authenticateClientResp.getSmdpSignature2()), ""));
        authenticateClientSmDp.setSmdpCertificate(Util.byteArrayToHexString(Base64.decodeBase64(authenticateClientResp.getSmdpCertificate()), ""));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - authenticateClient returning: " + authenticateClientSmDp);
        }

        return authenticateClientSmDp;
    }

    private AuthenticateClientResp getAuthenticateClientResponse(InitialAuthenticationKeys initialAuthenticationKeys, String encodedAuthenticateServerResponse) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Authenticate client with SM-DP+ with initialAuthenticationKeys: " +
                    initialAuthenticationKeys +
                    " encodedAuthenticateServerResponse: " +
                    encodedAuthenticateServerResponse);
        }

        AuthenticateClientResp authenticateClientResp = es9Module.authenticateClient(initialAuthenticationKeys.getTransactionId(), encodedAuthenticateServerResponse, initialAuthenticationKeys.getEuiccConfiguredAddress());

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Authenticate client with SM-DP+ - Response : " + authenticateClientResp);
        }

        return authenticateClientResp;
    }

    public String authenticateWithEuicc(InitialAuthenticationKeys initialAuthenticationKeys) {

        progress.stepExecuted(DOWNLOAD_PROFILE_AUTHENTICATE_WITH_EUICC, "authenticateWithEuicc retrieving...");

        String authenticateServerResponse = apduTransmitter.transmitApdus(ApduUtils.authenticateServerApdu(initialAuthenticationKeys.getServerSigned1(),
                initialAuthenticationKeys.getServerSignature1(),
                initialAuthenticationKeys.getEuiccCiPKIdTobeUsed(), initialAuthenticationKeys.getServerCertificate(),
                initialAuthenticationKeys.getMatchingId()));
        String encodedAuthenticateServerResponse = Base64.encodeBase64String(Util.hexStringToByteArray(authenticateServerResponse));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Authenticate server response (base64): " + encodedAuthenticateServerResponse);
        }

        progress.stepExecuted(DOWNLOAD_PROFILE_AUTHENTICATED_WITH_EUICC, "authenticateWithEuicc authenticated...");

        return encodedAuthenticateServerResponse;
    }
}
