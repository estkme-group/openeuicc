package com.truphone.lpa.impl.download;


import com.truphone.lpa.impl.InitialAuthenticationKeys;
import org.apache.commons.codec.binary.Base64;
import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.es9plus.message.response.GetBoundProfilePackageResp;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.impl.AuthenticateClientSmDp;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpa.progress.DownloadProgressPhase;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.util.LogStub;
import com.truphone.util.Util;

import java.util.logging.Logger;

public class DownloadPhaseWorker {
    private static final Logger LOG = Logger.getLogger(DownloadPhaseWorker.class.getName());

    private final DownloadProgress progress;
    private final ApduTransmitter apduTransmitter;
    private final Es9PlusImpl es9Module;

    public DownloadPhaseWorker(DownloadProgress progress, ApduTransmitter apduTransmitter, Es9PlusImpl es9Module) {

        this.progress = progress;
        this.apduTransmitter = apduTransmitter;
        this.es9Module = es9Module;
    }

    public String prepareDownload(AuthenticateClientSmDp authenticateClientSmDp) {

        progress.setCurrentPhase(DownloadProgressPhase.DOWNLOADING);
        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_PREPARE_DOWNLOAD, "prepareDownload retrieving...");

        String prepareDownloadResponse = apduTransmitter.transmitApdus(ApduUtils.prepareDownloadApdu(authenticateClientSmDp.getSmdpSigned2(),
                authenticateClientSmDp.getSmdpSignature2(), authenticateClientSmDp.getSmdpCertificate(),
                null));
        String encodedPrepareDownloadResponse = Base64.encodeBase64String(Util.hexStringToByteArray(prepareDownloadResponse));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Prepare download response (base64): " + encodedPrepareDownloadResponse);
        }

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_PREPARED_DOWNLOAD, "prepareDownload retrieved...");

        return encodedPrepareDownloadResponse;
    }

    public String getBoundProfilePackage(InitialAuthenticationKeys initialAuthenticationKeys, String encodedPrepareDownloadResponse) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_GET_BOUND_PROFILE_PACKAGE,
                "downloadAndInstallProfilePackage retrieving...");

        GetBoundProfilePackageResp getBoundProfilePackageResp = getGetBoundProfilePackageResp(initialAuthenticationKeys, encodedPrepareDownloadResponse, initialAuthenticationKeys.getEuiccConfiguredAddress());
        String bpp = Util.byteArrayToHexString(Base64.decodeBase64(getBoundProfilePackageResp.getBoundProfilePackage()), "");

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_BOUND_PROFILE_PACKAGE_RETRIEVED,
                "downloadAndInstallProfilePackage retrieved...");

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - getBoundProfilePackage - BPP is: " + bpp);
        }

        return bpp;
    }

    private GetBoundProfilePackageResp getGetBoundProfilePackageResp(InitialAuthenticationKeys initialAuthenticationKeys, String encodedPrepareDownloadResponse, String smdpAddress) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Getting bound profile package from SM-DP+");
        }

        GetBoundProfilePackageResp getBoundProfilePackageResp = es9Module.getBoundProfilePackage(initialAuthenticationKeys.getTransactionId(),
                encodedPrepareDownloadResponse,smdpAddress);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Getting bound profile package from SM-DP+ - Response - " +
                    encodedPrepareDownloadResponse);
        }

        return getBoundProfilePackageResp;
    }
}
