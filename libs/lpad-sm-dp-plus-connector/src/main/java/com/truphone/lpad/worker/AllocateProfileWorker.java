package com.truphone.lpad.worker;

import com.truphone.es9plus.AllocateProfileResponse;
import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.util.LogStub;
import com.truphone.lpad.LpadWorker;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker containing all logic to allocate a profile for a MCC with a specific EID
 */
public class AllocateProfileWorker implements LpadWorker<LpadWorkerExchange<AllocateProfileWorker.AllocateProfileInputParams>, String> {
    private static final Logger LOG = Logger.getLogger(AllocateProfileWorker.class.getName());

    private final Progress progress;
    private final Es9PlusImpl es9PlusImpl;

    /**
     * @param progress    Progress Bar
     * @param es9PlusImpl RSP29 Client
     */
    public AllocateProfileWorker(final Progress progress,
                                 final Es9PlusImpl es9PlusImpl) {

        inputValidation(progress == null, "Progress must not be null");
        inputValidation(es9PlusImpl == null, "Es9PlusImpl must not be null");

        this.progress = progress;
        this.es9PlusImpl = es9PlusImpl;
    }

    /**
     * Allocates the Protected Profile Packages to specified EIDs based on given MCC
     *
     * @param lpadWorkerExchange Exchange that must contain in its body the {@link AllocateProfileInputParams}
     * @return Activation Code Token
     */
    public String run(final LpadWorkerExchange<AllocateProfileInputParams> lpadWorkerExchange) {
//        inputValidation(lpadWorkerExchange == null, "Input params to invoke Allocate Profile must not be null");
//        inputValidation(lpadWorkerExchange.getBody() == null, "Input params must have body defined");
//        inputValidation(StringUtils.isBlank(lpadWorkerExchange.getBody().getEid()), "EID must not be null/empty");
//        inputValidation(StringUtils.isBlank(lpadWorkerExchange.getBody().getMcc()), "MCC must not be null/empty");
//
//        progress.setTotalSteps(2);
//        progress.stepExecuted(ProgressStep.ALLOCATE_PROFILE_ALLOCATING, "allocateProfile allocating...");
//
//        logDebug(" -  Allocating profile for MCC: " + lpadWorkerExchange.getBody().getMcc()
//                + " and EID: " + lpadWorkerExchange.getBody().getEid());
//
//        AllocateProfileResponse allocateProfileResponse
//                = es9PlusImpl.allocateProfile(lpadWorkerExchange.getBody().getEid(), lpadWorkerExchange.getBody().getMcc());
//
//        if (allocateProfileResponse != null) {
//            progress.stepExecuted(ProgressStep.ALLOCATE_PROFILE_ALLOCATED, "allocateProfile allocated!");
//
//            logDebug(" -  Allocate Profile Response: " + allocateProfileResponse);
//
//            return allocateProfileResponse.getAcToken();
//        } else {
//            logDebug(" -  No matching id returned from profile broker, please check profiles are available");
//
//            throw new RuntimeException("Unable to allocate profile");
//        }
            throw new IllegalArgumentException("Not implemented");
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
     * This class is responsible to gather the mandatory params to allocate a profile, which are:
     * <ul>
     * <li>mcc: Mobile country code</li>
     * <li>eid: eUICC-ID</li>
     * </ul>
     */
    public class AllocateProfileInputParams {
        private final String mcc;
        private final String eid;

        /**
         * Input params to allocate the Protected Profile Packages
         *
         * @param mcc Mobile country code
         * @param eid eUICC-ID
         */
        public AllocateProfileInputParams(final String mcc, final String eid) {
            this.mcc = mcc;
            this.eid = eid;
        }

        String getMcc() {
            return mcc;
        }

        String getEid() {
            return eid;
        }
    }
}


