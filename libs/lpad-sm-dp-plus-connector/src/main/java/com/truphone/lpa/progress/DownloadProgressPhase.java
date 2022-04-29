package com.truphone.lpa.progress;


import com.truphone.lpad.progress.ProgressStep;

import java.util.Arrays;
import java.util.List;

public enum DownloadProgressPhase {
    CONNECTING(0.05, ProgressStep.DOWNLOAD_PROFILE_RETRIEVING_EUICC_ADDRESS,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTING_EUICC_CONFIGURED_ADDRESS,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTED_EUICC_ADDRESS),
    AUTHENTICATING(0.3, ProgressStep.DOWNLOAD_PROFILE_GET_EUICC_INFO,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTING_EUICC_INFO,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTED_EUICC_INFO,
            ProgressStep.DOWNLOAD_PROFILE_GET_EUICC_CHALLENGE,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTING_EUICC_CHALLENGE,
            ProgressStep.DOWNLOAD_PROFILE_CONVERTED_EUICC_CHALLENGE,
            ProgressStep.DOWNLOAD_PROFILE_INITIATE_AUTHENTICATION,
            ProgressStep.DOWNLOAD_PROFILE_INITIATED_AUTHENTICATION,
            ProgressStep.DOWNLOAD_PROFILE_AUTHENTICATE_WITH_EUICC,
            ProgressStep.DOWNLOAD_PROFILE_AUTHENTICATED_WITH_EUICC,
            ProgressStep.DOWNLOAD_PROFILE_AUTHENTICATE_CLIENT,
            ProgressStep.DOWNLOAD_PROFILE_AUTHENTICATED_CLIENT),
    DOWNLOADING(0.1,
            ProgressStep.DOWNLOAD_PROFILE_PREPARE_DOWNLOAD,
            ProgressStep.DOWNLOAD_PROFILE_PREPARED_DOWNLOAD,
            ProgressStep.DOWNLOAD_PROFILE_GET_BOUND_PROFILE_PACKAGE,
            ProgressStep.DOWNLOAD_PROFILE_BOUND_PROFILE_PACKAGE_RETRIEVED),
    GENERATING(0.05,
            ProgressStep.DOWNLOAD_PROFILE_GENERATING_SBPP,
            ProgressStep.DOWNLOAD_PROFILE_GENERATED_SBPP,
            ProgressStep.DOWNLOAD_PROFILE_GENERATING_SBPP_APDUS,
            ProgressStep.DOWNLOAD_PROFILE_GENERATED_SBPP_APDUS),
    INSTALLING(0.5,
            ProgressStep.DOWNLOAD_PROFILE_LOADING_SBPP,
            ProgressStep.DOWNLOAD_PROFILE_INITIALIZE_SECURE_CHANNEL,
            ProgressStep.DOWNLOAD_PROFILE_CONFIGURE_ISDPA,
            ProgressStep.DOWNLOAD_PROFILE_STORE_METADATA,
            ProgressStep.DOWNLOAD_PROFILE_BOUND_PROFILE_PACKAGE,
            ProgressStep.DOWNLOAD_PROFILE_INSTALLED);

    private List<ProgressStep> progressSteps;
    private double phaseTotalPercentage;

    DownloadProgressPhase(double phaseTotalPercentage, ProgressStep... progressSteps) {

        this.progressSteps = Arrays.asList(progressSteps);
        this.phaseTotalPercentage = phaseTotalPercentage;
    }

    public List<ProgressStep> getProgressSteps() {

        return progressSteps;
    }

    public double getPhaseTotalPercentage() {

        return phaseTotalPercentage;
    }
}
