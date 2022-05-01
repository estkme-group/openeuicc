package com.truphone.lpa.impl;

import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.rsp.dto.asn1.rspdefinitions.SetNicknameResponse;
import com.truphone.util.LogStub;
import com.truphone.util.Util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetNicknameWorker {
    private static final Logger LOG = Logger.getLogger(ListProfilesWorker.class.getName());

    private final String iccid;
    private final String nickname;
    private final ApduChannel apduChannel;

    SetNicknameWorker(String iccid, String nickname, ApduChannel apduChannel) {
        this.apduChannel = apduChannel;
        this.iccid = iccid;
        this.nickname = nickname;
    }

    boolean run() {
        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Renaming profile: " + iccid);
        }

        String apdu = ApduUtils.setNicknameApdu(iccid, Util.byteArrayToHexString(nickname.getBytes(), ""));
        String eResponse = apduChannel.transmitAPDU(apdu);

        try {
            InputStream is = new ByteArrayInputStream(Hex.decodeHex(eResponse.toCharArray()));
            SetNicknameResponse response = new SetNicknameResponse();

            response.decode(is);

            if ("0".equals(response.getSetNicknameResult().toString())) {
                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Profile renamed: " + iccid);
                }
                return true;
            } else {
                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Profile not renamed: " + iccid);
                }
                return false;
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be renamed");

            throw new RuntimeException("Unable to rename profile: " + iccid + ", response: " + eResponse);
        } catch (DecoderException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - iccid: " + iccid + " profile failed to be renamed. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to rename profile: " + iccid + ", response: " + eResponse);
        }
    }
}
