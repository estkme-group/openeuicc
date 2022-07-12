package com.truphone.lpa.impl;


import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.DisableProfileResponse;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

class DisableProfileWorker {
    private static final Logger LOG = Logger.getLogger(DisableProfileWorker.class.getName());

    private final String iccid;
    private final Progress progress;
    private final ApduChannel apduChannel;

    DisableProfileWorker(String iccid, Progress progress, ApduChannel apduChannel) {

        this.iccid = iccid;
        this.progress = progress;
        this.apduChannel = apduChannel;
    }

    String run() {
        String eResponse = transmitDisableProfile(iccid, progress);

        return convertDisableProfileResponse(iccid, progress, eResponse);
    }

    private String convertDisableProfileResponse(String iccid, Progress progress, String eResponse) {

        progress.stepExecuted(ProgressStep.DISABLE_PROFILE_CONVERTING_RESPONSE, "Converting response");

        try {
            InputStream is = new ByteArrayInputStream(TextUtil.decodeHex(eResponse));
            DisableProfileResponse disableProfileResponse = new DisableProfileResponse();

            disableProfileResponse.decode(is);

            if (LocalProfileAssistantImpl.PROFILE_RESULT_SUCESS.equals(disableProfileResponse.getDisableResult().toString())) {
                progress.stepExecuted(ProgressStep.DISABLE_PROFILE_DISABLED, iccid + " disabled");

                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - SEND Status to APDU Channel");
                }

                apduChannel.sendStatus();

                progress.stepExecuted(ProgressStep.DISABLE_PROFILE_TRIGGERED_PROFILE_SWITCH, iccid + " triggered profile switch");
            } else {
                progress.stepExecuted(ProgressStep.DISABLE_PROFILE_NOT_DISABLED, iccid + " profile not disabled");

                LOG.info(LogStub.getInstance().getTag() + " - iccid:" + iccid + " profile not disabled");
            }

            return disableProfileResponse.getDisableResult().toString();
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be disabled");

            throw new RuntimeException("Unable to disable profile: " + iccid + ", response: " + eResponse);
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be disabled. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to disable profile: " + iccid + ", response: " + eResponse);
        }
    }

    private String transmitDisableProfile(String iccid, Progress progress) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Disabling profile: " + iccid);
        }

        progress.setTotalSteps(4);
        progress.stepExecuted(ProgressStep.DISABLE_PROFILE_DISABLING_PROFILE, iccid + " disabling profile");

        String apdu = ApduUtils.disableProfileApdu(iccid, LocalProfileAssistantImpl.TRIGGER_PROFILE_REFRESH);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Disable profile apdu: " + apdu);
        }

        String eResponse = apduChannel.transmitAPDU(apdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Disable response: " + eResponse);
        }

        return eResponse;
    }
}
