package com.truphone.lpa.impl.download;


import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpa.progress.DownloadProgressPhase;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.EuiccConfiguredAddressesResponse;
import com.truphone.util.LogStub;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectingPhaseWorker {
    private static final Logger LOG = Logger.getLogger(ConnectingPhaseWorker.class.getName());

    private DownloadProgress progress;
    private ApduTransmitter apduTransmitter;

    public ConnectingPhaseWorker(DownloadProgress progress, ApduTransmitter apduTransmitter) {

        this.progress = progress;
        this.apduTransmitter = apduTransmitter;
    }

    public String getEuiccConfiguredAddress(String matchingId) {

        progress.setCurrentPhase(DownloadProgressPhase.CONNECTING);
        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_RETRIEVING_EUICC_ADDRESS, "getEuiccConfiguredAddress - retrieving...");

        return convertEuiccConfiguredAddress(apduTransmitter.transmitApdu(ApduUtils.getEuiccConfiguredAddressesApdu()), matchingId);
    }

    private String convertEuiccConfiguredAddress(String euiCCConfiguredAddressAPDUResponse, String matchingId) {

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_CONVERTING_EUICC_CONFIGURED_ADDRESS, "getEuiccConfiguredAddresses Success!");

        String euiccConfiguredAddresses = getEuiccConfiguredAddress(euiCCConfiguredAddressAPDUResponse, matchingId);

        progress.stepExecuted(ProgressStep.DOWNLOAD_PROFILE_CONVERTED_EUICC_ADDRESS, "Converted EUICC Address Success!");

        return euiccConfiguredAddresses;
    }

    private String getEuiccConfiguredAddress(String euiCCConfiguredAddressAPDUResponse, String matchingId) {

        try {
            EuiccConfiguredAddressesResponse euiccConfiguredAddressesResponse = decodeEuiccConfiguredAddressesResponse(euiCCConfiguredAddressAPDUResponse);
            
            String euiccConfiguredAddress = euiccConfiguredAddressesResponse.getDefaultDpAddress().toString();
            
            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - SM-DP+ configured address: " + euiccConfiguredAddress);
            }

            return euiccConfiguredAddress;
        } catch (DecoderException e) {
            LOG.log(Level.SEVERE, "KOL.007" + e.getMessage(), e);
            LOG.severe(LogStub.getInstance().getTag() + " - matchingId: " + matchingId +
                    " Unable to retrieve eUICC configured address. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve eUICC configured address: " + matchingId);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "KOL.007" + ioe.getMessage(), ioe);
            LOG.severe(LogStub.getInstance().getTag() + " - matchingId: " + matchingId +
                    " Unable to retrieve eUICC configured address. IOException:" + ioe.getMessage());

            throw new RuntimeException("Unable to retrieve eUICC configured address");
        }
    }

    private EuiccConfiguredAddressesResponse decodeEuiccConfiguredAddressesResponse(String euiCCConfiguredAddressAPDUResponse) throws DecoderException, IOException {
        InputStream is = null;

        try {
            EuiccConfiguredAddressesResponse euiccConfiguredAddressesResponse = new EuiccConfiguredAddressesResponse();
            is = new ByteArrayInputStream(Hex.decodeHex(euiCCConfiguredAddressAPDUResponse.toCharArray()));

            euiccConfiguredAddressesResponse.decode(is);

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - decoded euiccConfiguredAddressesResponse: " + euiccConfiguredAddressesResponse.toString());
            }
            return euiccConfiguredAddressesResponse;
        } finally {
            CloseResources.closeResources(is);
        }
    }
}
