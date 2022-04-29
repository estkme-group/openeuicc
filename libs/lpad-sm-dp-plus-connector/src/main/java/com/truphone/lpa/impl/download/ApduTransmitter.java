package com.truphone.lpa.impl.download;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.ApduTransmittedListener;
import com.truphone.util.LogStub;

import java.util.List;
import java.util.logging.Logger;

public class ApduTransmitter {
    private static final Logger LOG = Logger.getLogger(ApduTransmitter.class.getName());

    private ApduChannel apduChannel;

    public ApduTransmitter(ApduChannel apduChannel) {

        this.apduChannel = apduChannel;
    }

    String transmitApdu(String apdu) {
        String apduResponse;

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - APDU to transmit: " + apdu);
        }

        apduResponse = apduChannel.transmitAPDU(apdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Transmit APDU response: " + apduResponse);
        }

        return apduResponse;
    }

    String transmitApdus(List<String> apdus) {
        String apduResponse;

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - APDUs to transmit: " + apdus);
        }

        apduResponse = apduChannel.transmitAPDUS(apdus);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Transmit APDUs response: " + apduResponse);
        }

        return apduResponse;
    }

    void addApduTransmittedListener(ApduTransmittedListener apduTransmittedListener) {

        apduChannel.setApduTransmittedListener(apduTransmittedListener);
    }

    void removeApduTransmittedListener(ApduTransmittedListener apduTransmittedListener) {

        apduChannel.removeApduTransmittedListener(apduTransmittedListener);
    }
}
