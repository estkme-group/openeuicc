/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.truphone.lpa.impl;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpad.progress.Progress;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;
import java.util.logging.Logger;

/**
 *
 * @author amilcar.pereira
 */
public class SetSMDPAddressWorker {

    private static final Logger LOG = Logger.getLogger(EnableProfileWorker.class.getName());
    private final ApduChannel apduChannel;
    private final Progress progress;
    private String dpAddrNew;

    public SetSMDPAddressWorker(ApduChannel apduChannel, Progress progress, String dpAddrNew) {
        this.apduChannel = apduChannel;
        this.progress = progress;
        this.dpAddrNew=dpAddrNew;
    }

    public String run() {
        String hexDPAddress = TextUtil.toHexString(dpAddrNew.getBytes());
        String apdu = ApduUtils.setDefaultDpAddressApdu(hexDPAddress);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - setEuiccConfiguredAddressesApdu APDU: " + apdu);
        }

        String eResponse = apduChannel.transmitAPDU(apdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - setEuiccConfiguredAddressesApdu response: " + eResponse);
        }

        return eResponse;
    }

  
}
