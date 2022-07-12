package com.truphone.lpad.worker;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.LpadWorker;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.DeleteProfileResponse;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteProfileWorker implements LpadWorker<LpadWorkerExchange<DeleteProfileWorker.DeleteProfileInputParams>, String> {
    private static final Logger LOG = Logger.getLogger(DeleteProfileWorker.class.getName());

    private final Progress progress;
    private final ApduChannel apduChannel;

    static final String PROFILE_RESULT_SUCCESS = "0";

    /**
     * @param progress    Progress Bar
     * @param apduChannel aPDU Channel
     */
    public DeleteProfileWorker(Progress progress, ApduChannel apduChannel) {
        inputValidation(progress == null, "Progress must not be null");
        inputValidation(apduChannel == null, "ApduChannel must not be null");

        this.progress = progress;
        this.apduChannel = apduChannel;
    }

    /**
     * Deletes the Profile from the ICC and converts the response to be readable
     *
     * @param lpadWorkerExchange Exchange that must contain in its body the {@link DeleteProfileInputParams}
     * @return Delete response Code
     */
    public String run(final LpadWorkerExchange<DeleteProfileInputParams> lpadWorkerExchange) {

        inputValidation(lpadWorkerExchange == null, "Input params to invoke Delete Profile must not be null");
        inputValidation(lpadWorkerExchange.getBody() == null, "Input params must have body defined");
        inputValidation(TextUtil.isBlank(lpadWorkerExchange.getBody().getIccid()), "ICCID must not be null/empty");

        String iccid = lpadWorkerExchange.getBody().getIccid(); // Get ICCID From Body
        String eResponse = transmitDeleteProfile(iccid, progress);

        return convertDeleteProfile(iccid, progress, eResponse);
    }

    /**
     * Converts the response from the transmission, checks if there are errors in the transmission
     *
     * @param iccid     Integrated Circuit Card ID
     * @param progress  Progress Bar
     * @param eResponse Response from transmitDeleteProfile
     * @return Delete response Code
     */
    private String convertDeleteProfile(String iccid, Progress progress, String eResponse) {

        progress.stepExecuted(ProgressStep.DELETE_PROFILE_CONVERTING_RESPONSE, "Converting response");

        DeleteProfileResponse deleteProfileResponse = new DeleteProfileResponse();

        try {
            InputStream is = new ByteArrayInputStream(TextUtil.decodeHex(eResponse));
            deleteProfileResponse.decode(is);

            logDebug(" -  Delete response: " + deleteProfileResponse);
            if (PROFILE_RESULT_SUCCESS.equals(deleteProfileResponse.getDeleteResult().toString())) {
                logDebug(" - iccid: " + iccid + " profile deleted");
                logDebug(" - iccid: " + iccid + " Refreshing SIM card on Delete.");
                apduChannel.sendStatus();

                progress.stepExecuted(ProgressStep.DELETE_PROFILE_DELETED, iccid + " deleted successfully");
            } else {
                progress.stepExecuted(ProgressStep.DELETE_PROFILE_NOT_DELETED, iccid + " profile not deleted");

                LOG.info(LogStub.getInstance().getTag() + " - iccid:" + iccid + " profile not deleted");
            }

            return deleteProfileResponse.getDeleteResult().toString();

        } catch (IOException ioe) {
            LOG.severe(LogStub.getInstance().getTag() + " - iccid:" + iccid + " profile failed to be deleted");

            throw new RuntimeException("Unable to delete profile: " + iccid + ", response: " + eResponse);
        } catch (NumberFormatException e) {
            LOG.severe(LogStub.getInstance().getTag() + " - " + e.getMessage());
            LOG.severe(LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be deleted. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to delete profile: " + iccid + ", response: " + eResponse);
        }

    }

    /**
     * Transmits the action to deletes the profile for the specific ICC
     *
     * @param iccid    Integrated Circuit Card ID
     * @param progress Progress Bar
     * @return
     */
    private String transmitDeleteProfile(String iccid, Progress progress) {

        progress.setTotalSteps(3);
        progress.stepExecuted(ProgressStep.DELETE_PROFILE_DELETING_PROFILE, iccid + " delete profile");
        logDebug(" - Deleting profile: " + iccid);

        String apdu = ApduUtils.deleteProfileApdu(iccid);
        logDebug(" - Delete profile apdu: " + apdu);

        String eResponse = apduChannel.transmitAPDU(apdu);
        logDebug(" - Delete Response: " + eResponse);

        return eResponse;
    }

    private void logDebug(final String errorMessage) {
        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + errorMessage);
        }
    }

    private void inputValidation(final boolean invalidCondition, final String errorMessage) {
        if (invalidCondition) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * This class is responsible to gather the mandatory params to delete a profile, which are:
     * <ul>
     * <li>iccid: Integrated Circuit Card ID</li>
     * </ul>
     */
    public class DeleteProfileInputParams {
        private final String iccid;

        /**
         * Input params to allocate the Protected Profile Packages
         *
         * @param iccid Integrated Circuit Card ID
         */
        public DeleteProfileInputParams(final String iccid) {
            this.iccid = iccid;
        }

        String getIccid() {
            return iccid;
        }

    }
}