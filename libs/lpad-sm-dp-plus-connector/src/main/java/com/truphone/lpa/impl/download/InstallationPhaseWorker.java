package com.truphone.lpa.impl.download;

import com.truphone.lpa.ApduTransmittedListener;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpa.progress.DownloadProgressPhase;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.ProfileInstallationResult;
import com.truphone.rsp.dto.asn1.rspdefinitions.ProfileInstallationResultData;
import com.truphone.util.LogStub;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstallationPhaseWorker {

    private static final Logger LOG = Logger.getLogger(InstallationPhaseWorker.class
            .getName());

    private final DownloadProgress progress;
    private final ApduTransmitter apduTransmitter;

    public InstallationPhaseWorker(DownloadProgress progress, ApduTransmitter apduTransmitter) {

        this.progress = progress;
        this.apduTransmitter = apduTransmitter;
    }

    public void loadingSbppApdu(Map<SbppApdu, List<String>> sbpp) {
        ApduTransmittedListener apduTransmittedListener;

        progress.setCurrentPhase(DownloadProgressPhase.INSTALLING, getTotalApdus(sbpp)
                + DownloadProgressPhase.INSTALLING.getProgressSteps().size());
        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_LOADING_SBPP,
                "generateSbpp generating...");

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Sending SBPP to eUICC");
        }

        apduTransmittedListener = addApduTransmittedListener();

        loadInitialiseSecureChannel(sbpp.get(SbppApdu.INITIALIZE_SECURE_CHANNEL));
        loadConfigureIsdpa(sbpp.get(SbppApdu.CONFIGURE_ISDPA));
        loadStoreMetadata(sbpp.get(SbppApdu.STORE_METADATA));
        
        if (sbpp.size() == 5) {
            loadReplaceSessionKeys(sbpp.get(SbppApdu.REPLACE_SESSIONS_KEYS));
        }
        
        loadBoundProfilePackage(sbpp.get(SbppApdu.BOUND_PROFILE_PACKAGE));
        removeApduTransmittedListener(apduTransmittedListener);

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_INSTALLED,
                "INSTALLED!");
    }

    private int getTotalApdus(Map<SbppApdu, List<String>> sbpp) {
        int totalApdus = 0;

        for (List<String> apdus : sbpp.values()) {
            totalApdus += apdus.size();
        }

        return totalApdus;
    }

    private ApduTransmittedListener addApduTransmittedListener() {
        ApduTransmittedListener apduTransmittedListener = new ApduTransmittedListener() {
            @Override
            public void onApduTransmitted() {

                progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_APDU_TRANSMITTED, "Apdu transmitted");
            }
        };

        apduTransmitter.addApduTransmittedListener(apduTransmittedListener);

        return apduTransmittedListener;
    }

    private void removeApduTransmittedListener(ApduTransmittedListener apduTransmittedListener) {

        apduTransmitter.removeApduTransmittedListener(apduTransmittedListener);
    }

    private void loadBoundProfilePackage(List<String> sbpp) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_BOUND_PROFILE_PACKAGE,
                "loadBoundProfilePackage...");

        String profileInstallationResult = apduTransmitter.transmitApdus(sbpp);

        if (StringUtils.isNotBlank(profileInstallationResult) && profileInstallationResult.length() > 4) {
            checkProfileInstallationResult(profileInstallationResult);
        } else {
            throw new RuntimeException("Unexpected response on loadBoundProfilePackage");
        }
    }

    private void loadStoreMetadata(List<String> sbpp) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_STORE_METADATA,
                "loadStoreMetadata...");

        String profileInstallationResult = apduTransmitter.transmitApdus(sbpp);

        if (profileInstallationResult.compareTo("9000") != 0) {
            if (StringUtils.isNotBlank(profileInstallationResult) && profileInstallationResult.length() > 4) {
                checkProfileInstallationResult(profileInstallationResult);
            } else {
                throw new RuntimeException("Unexpected response on loadStoreMetadata");
            }
        }
    }

    private void loadConfigureIsdpa(List<String> sbpp) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_CONFIGURE_ISDPA,
                "loadConfigureIsdpa...");

        String profileInstallationResult = apduTransmitter.transmitApdus(sbpp);

        if (profileInstallationResult.compareTo("9000") != 0) {
            if (StringUtils.isNotBlank(profileInstallationResult) && profileInstallationResult.length() > 4) {
                checkProfileInstallationResult(profileInstallationResult);
            } else {
                throw new RuntimeException("Unexpected response on loadConfigureIsdpa");
            }
        }
    }

    private void loadInitialiseSecureChannel(List<String> sbpp) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_INITIALIZE_SECURE_CHANNEL,
                "loadInitialiseSecureChannel...");

        String profileInstallationResult = apduTransmitter.transmitApdus(sbpp);

        if (profileInstallationResult.compareTo("9000") != 0) {
            if (StringUtils.isNotBlank(profileInstallationResult) && profileInstallationResult.length() > 4) {
                checkProfileInstallationResult(profileInstallationResult);
            } else {
                throw new RuntimeException("Unexpected response on loadInitialiseSecureChannel");
            }
        }
    }

    private void checkProfileInstallationResult(String profileInstallationResultRaw) {
        boolean success = false;
        String errorMessage = "";

        try {
            ProfileInstallationResult profileInstallationResult = getProfileInstallationResult(profileInstallationResultRaw);

            if (profileInstallationResult.getProfileInstallationResultData() != null
                    && profileInstallationResult.getProfileInstallationResultData().getFinalResult() != null) {

                ProfileInstallationResultData.FinalResult finalResult = profileInstallationResult.getProfileInstallationResultData().getFinalResult();

                if (finalResult.getSuccessResult() != null) {
                    LOG.info(LogStub.getInstance().getTag() + " - Decoded Success Result: " + finalResult.getSuccessResult().toString());

                    success = true;

                } else {
                    errorMessage = invalidFinalResult(finalResult);
                }
            } else {
                errorMessage = invalidProfileInstallationData();
            }

            if (!success) {
                throw new RuntimeException(errorMessage);
            }

        } catch (DecoderException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.severe(LogStub.getInstance().getTag() + " -  Unable to retrieve Profile Installation Result. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve Profile Installation Result.");
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + ioe.getMessage(), ioe);
            LOG.severe(LogStub.getInstance().getTag() + " -  Unable to retrieve Profile Installation Result. IOException:" + ioe.getMessage());

            throw new RuntimeException("Unable to retrieve Profile Installation Result.");
        }
    }

    private String invalidFinalResult(ProfileInstallationResultData.FinalResult finalResult) {
        String errorMessage = finalResult.getErrorResult().getErrorReason().toString() + ":"
                + finalResult.getErrorResult().getBppCommandId().toString() + ":"
                + finalResult.getErrorResult().getSimaResponse().toString();

        LOG.info(LogStub.getInstance().getTag() + " - Decoded ERROR Result: " + finalResult.getErrorResult().toString());

        return errorMessage;
    }

    private String invalidProfileInstallationData() {
        String errorMessage = "Could not parse Profile Installation Result";

        LOG.info(LogStub.getInstance().getTag() + " - Profile Installation Result Data or Final result is null.");

        return errorMessage;
    }

    private ProfileInstallationResult getProfileInstallationResult(String profileInstallationResultRaw) throws DecoderException, IOException {
        InputStream is = null;

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Check Profile Installation Result input: " + profileInstallationResultRaw);
        }

        try {
            ProfileInstallationResult profileInstallationResult = new ProfileInstallationResult();

            is = new ByteArrayInputStream(Hex.decodeHex(profileInstallationResultRaw.toCharArray()));

            profileInstallationResult.decode(is, true);

            return profileInstallationResult;
        } finally {
            CloseResources.closeResources(is);
        }
    }

    private void loadReplaceSessionKeys(List<String> sbpp) {
        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_REPLACE_SESSIONS_KEYS,
                "loadReplaceSessionsKeys...");

        String profileInstallationResult = apduTransmitter.transmitApdus(sbpp);

        if (profileInstallationResult.compareTo("9000") != 0) {
            if (StringUtils.isNotBlank(profileInstallationResult) && profileInstallationResult.length() > 4) {
                checkProfileInstallationResult(profileInstallationResult);
            } else {
                throw new RuntimeException("Unexpected response on loadReplaceSessionsKeys");
            }
        }
    }
}
