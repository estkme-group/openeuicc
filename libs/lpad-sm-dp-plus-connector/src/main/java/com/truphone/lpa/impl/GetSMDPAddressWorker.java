/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.truphone.lpa.impl;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.rsp.dto.asn1.rspdefinitions.EuiccConfiguredAddressesResponse;
import com.truphone.util.LogStub;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author amilcar.pereira
 */
public class GetSMDPAddressWorker {

    private static final Logger LOG = Logger.getLogger(EnableProfileWorker.class.getName());
    private final ApduChannel apduChannel;
    private final Progress progress;

    public GetSMDPAddressWorker(ApduChannel apduChannel, Progress progress) {
        this.apduChannel = apduChannel;
        this.progress = progress;
    }
    
    public String run(){
        return transmitGetSMDPAddress();
    }

    private String transmitGetSMDPAddress() {

        String apdu = ApduUtils.getEuiccConfiguredAddressesApdu();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - getEuiccConfiguredAddressesApdu APDU: " + apdu);
        }

        String eResponse = apduChannel.transmitAPDU(apdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - getEuiccConfiguredAddressesApdu response: " + eResponse);
        }
      
        return eResponse;
    }
}
