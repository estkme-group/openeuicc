package com.truphone.lpa.impl;


import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.EnableProfileResponse;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

class EnableProfileWorker {
    private static final Logger LOG = Logger.getLogger(EnableProfileWorker.class.getName());

    private final String iccid;
    private final Progress progress;
    private final ApduChannel apduChannel;

    EnableProfileWorker(String iccid, Progress progress, ApduChannel apduChannel) {

        this.iccid = iccid;
        this.progress = progress;
        this.apduChannel = apduChannel;
    }

    String run() {
        String eResponse = transmitEnableProfile();

        return convertEnableProfileResponse(eResponse);
    }

    private String convertEnableProfileResponse(String eResponse) {

        progress.stepExecuted(ProgressStep.ENABLE_PROFILE_CONVERTING_RESPONSE, "Enable profile APDU");

        try {
            EnableProfileResponse enableProfileResponse = new EnableProfileResponse();
            InputStream is = new ByteArrayInputStream(TextUtil.decodeHex(eResponse));

            enableProfileResponse.decode(is);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Enable response: " + enableProfileResponse.toString());
            }

            if (LocalProfileAssistantImpl.PROFILE_RESULT_SUCESS.equals(enableProfileResponse.getEnableResult().toString())) {
                progress.stepExecuted(ProgressStep.ENABLE_PROFILE_PROFILE_ENABLED, iccid + " profile enabled successfully");

                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - iccid:" + iccid + " profile enabled successfully");
                }

                apduChannel.sendStatus();

                progress.stepExecuted(ProgressStep.ENABLE_PROFILE_TRIGGERED_PROFILE_SWITCH, iccid + " triggered profile switch");
            } else {
                progress.stepExecuted(ProgressStep.ENABLE_PROFILE_PROFILE_NOT_ENABLED, iccid + " profile not enabled");

                LOG.info(LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile not enabled");
            }

            return enableProfileResponse.getEnableResult().toString();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.severe(LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be enabled. message: " + e.getMessage());

            throw new RuntimeException("Unable to enable profile: " + iccid + ", response: " + eResponse);
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.severe(LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be enabled. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to enable profile: " + iccid + ", response: " + eResponse);
        }
    }

    private String transmitEnableProfile() {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Enabling profile: " + iccid);
        }

        progress.setTotalSteps(4);
        progress.stepExecuted(ProgressStep.ENABLE_PROFILE_ENABLING_PROFILE, "Enabling profile");

        String apdu = ApduUtils.enableProfileApdu(iccid, LocalProfileAssistantImpl.TRIGGER_PROFILE_REFRESH);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Enable profile APDU: " + apdu);
        }

        String eResponse = apduChannel.transmitAPDU(apdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Enable profile response: " + eResponse);
        }

        return eResponse;
    }
}
