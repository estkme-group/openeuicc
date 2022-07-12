package com.truphone.lpad.worker;


import com.truphone.lpa.ApduChannel;
import com.truphone.lpad.LpadWorker;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.GetEuiccDataResponse;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetEidLpadWorker implements LpadWorker<LpadWorkerExchange<String>, String> {
    private static final Logger LOG = Logger.getLogger(GetEidLpadWorker.class.getName());

    private final Progress progress;
    private final ApduChannel apduChannel;

    public GetEidLpadWorker(final Progress progress,
                            final ApduChannel apduChannel) {

        inputValidation(progress == null, "received an invalid progress: " + progress);
        inputValidation(apduChannel == null, "received an invalid apduChannel: " + apduChannel);

        this.progress = progress;
        this.apduChannel = apduChannel;
    }

    /**
     * Gets the EID from the eUICC, using its apdu
     *
     * @param lpadWorkerExchange Exchange that must contain in its body the APDU related with the EID which we want to obtain
     * @return The EID from the eUICC, using its APDU
     */
    public String run(final LpadWorkerExchange<String> lpadWorkerExchange) {

        progress.setTotalSteps(3);
        progress.stepExecuted(ProgressStep.GET_EID_RETRIEVING, "getEID retrieving...");

        inputValidation(lpadWorkerExchange == null, "Lpa dWorker Exchange must be provided");
        inputValidation(TextUtil.isBlank(lpadWorkerExchange.getBody()), "EID APDU must be provided");

        logDebug("EID APDU: " + lpadWorkerExchange);


        String eidapduResponseStr = apduChannel.transmitAPDU(lpadWorkerExchange.getBody());

        logDebug("Response: " + eidapduResponseStr);

        return convertGetEuiccData(eidapduResponseStr, progress);
    }

    private String convertGetEuiccData(final String eidapduResponseStr,
                                       final Progress progress) {

        progress.stepExecuted(ProgressStep.GET_EID_CONVERTING, "getEID converting...");

        inputValidation(TextUtil.isBlank(eidapduResponseStr), "received an invalid eidapduResponseStr: " + eidapduResponseStr);

        GetEuiccDataResponse eidResponse = new GetEuiccDataResponse();

        try {

            logDebug("Decoding response: " + eidapduResponseStr);

            InputStream is = new ByteArrayInputStream(TextUtil.decodeHex(eidapduResponseStr));

            logDebug("Decoding with GetEuiccDataResponse");

            eidResponse.decode(is, true);

            logDebug("EID is: " + eidResponse.getEidValue().toString());

            progress.stepExecuted(ProgressStep.GET_EID_CONVERTED, "getEID converted...");

            return eidResponse.getEidValue().toString();
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " -  Unable to retrieve EID. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve EID");
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + ioe.getMessage(), ioe);

            throw new RuntimeException("Invalid EID response, unable to retrieve EID");
        }
    }

    private void logDebug(final String errorMessage) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - " + errorMessage);
        }
    }

    private void inputValidation(final boolean invalidCondition, final String errorMessage) {
        if (invalidCondition) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
