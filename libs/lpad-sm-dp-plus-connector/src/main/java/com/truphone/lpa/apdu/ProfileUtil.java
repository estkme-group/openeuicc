package com.truphone.lpa.apdu;

import com.truphone.util.LogStub;
import com.truphone.util.TLVBean;

import java.util.ArrayList;
import java.util.logging.Logger;

public class ProfileUtil {
    private static final Logger LOG = Logger.getLogger(ProfileUtil.class
            .getName());

    public ArrayList<String[]> generateSBPP(String BPP) {
        if (!BPP.substring(0, 4).equals("BF36")) {
            throw new RuntimeException("Incorrect format in BPP");
        }
        ArrayList<String[]> array = new ArrayList<String[]>();
        int endOfSeg = 0;
        String[] initChannel = new String[1];
        initChannel[0] = headAndInitChannel(BPP);
        array.add(initChannel);
        endOfSeg += initChannel[0].length();
        String[] isdp = new String[1];
        isdp[0] = configIsdp(BPP.substring(endOfSeg));
        array.add(isdp);
        endOfSeg += isdp[0].length();
        String[] metaData = metaData(BPP.substring(endOfSeg));
        array.add(metaData);
        for (int i = 0; i < metaData.length; i++) {
            endOfSeg += metaData[i].length();
        }
        int beginOfSeg = BPP.substring(endOfSeg).indexOf("A2");

        if (beginOfSeg == 0) {
            String[] ppk = new String[1];
            ppk[0] = ppk(BPP.substring(endOfSeg));
            array.add(ppk);
            endOfSeg += ppk[0].length();
        }
        String[] ppp = ppp(BPP.substring(endOfSeg));
        array.add(ppp);
        return array;
    }

    private String headAndInitChannel(String data) {
        int beginOfSeg = data.indexOf("BF36");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in  BPP");
        }
        data += "FFFF";// endTag
        TLVBean tlv = null;
        tlv = selectTlv(data, "BF36", "FFFF");
        if (tlv == null) {
            throw new RuntimeException("Incorrect format in  BPP");
        }
        String head = tlv.getTaglen();
        data = tlv.getValue();
        // InitChannel
        beginOfSeg = data.indexOf("BF23");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in InitialiseSecureChannel of BPP");
        }
        tlv = null;

        tlv = selectTlv(data, "BF23", "A0");
        if (tlv == null) {
            throw new RuntimeException("Incorrect format in InitialiseSecureChannel of BPP");
        }
        String initChannel = tlv.getTaglen() + tlv.getValue();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"headAndInitChannel: " + head + initChannel);
        }

        return head + initChannel;
    }


    private String configIsdp(String data) {
        int beginOfSeg = data.indexOf("A0");

        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in configIsdp of BPP");
        }
        String str = null;
        TLVBean tlv = null;

        tlv = selectTlv(data, "A0", "A1");
        if (tlv == null) {
            throw new RuntimeException("Incorrect format in configIsdp of BPP");
        }
        str = tlv.getTaglen() + tlv.getValue();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"configIsdp: " + str);
        }

        return str;
    }


    private String[] metaData(String data) {
        int beginOfSeg = data.indexOf("A1");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in head of metaData");
        }
        TLVBean tlv = null;

        tlv = selectTlv(data, "A1", "A2");
        if (tlv == null) {
            tlv = selectTlv(data, "A1", "A3");
            if (tlv == null) {
                throw new RuntimeException("Incorrect format in head of metaData");
            }
        }
        String head = tlv.getTaglen();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"headOfMetadata: " + head);
        }

        data = tlv.getValue();

        ArrayList<String> array = new ArrayList<String>();

        beginOfSeg = data.indexOf("88");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in body of metaData");
        }
        tlv = null;
        data += "FFFF";
        TLVBean tlvBean = null;
        while (tlvBean == null) {
            tlv = selectTlv(data, "88", "FFFF");
            tlvBean = tlv;
            if (tlvBean == null) {
                tlv = selectTlv(data, "88", "88");
                if (tlv == null) {
                    throw new RuntimeException("Incorrect format in body of metaData");
                }
                String segBody = tlv.getTaglen() + tlv.getValue();
                array.add(segBody);
                data = data.substring(segBody.length());
            } else {
                array.add(tlv.getTaglen() + tlv.getValue());
            }

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug (LOG,"bodyOfMetadata: " + tlv.getTaglen() + tlv.getValue());
            }
        }
        String[] body = (String[]) array.toArray(new String[0]);
        ArrayList<String> metaData = new ArrayList<String>();
        metaData.add(head);
        for (int i = 0, n = body.length; i < n; i++) {
            metaData.add(body[i]);
        }
        return metaData.toArray(new String[0]);
    }

    private String ppk(String data) {
        int beginOfSeg = data.indexOf("A2");

        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in ppk of BPP");
        }
        String ppk = null;
        TLVBean tlvBean = null;

        tlvBean = selectTlv(data, "A2", "A3");
        if (tlvBean == null) {
            throw new RuntimeException("Incorrect format in ppk of BPP");
        }
        ppk = tlvBean.getTaglen() + tlvBean.getValue();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"ppk: " + ppk);
        }

        return ppk;
    }

    private String[] ppp(String data) {
        int beginOfSeg = data.indexOf("A3");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in head of ppp");
        }
        TLVBean tlvBean = null;

        data += "FFFF";
        tlvBean = selectTlv(data, "A3", "FFFF");
        if (tlvBean == null) {
            throw new RuntimeException("Incorrect format in head of ppp");
        }
        String head = tlvBean.getTaglen();

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"headOfPPP: " + head);
        }

        data = tlvBean.getValue();

        ArrayList<String> array = new ArrayList<String>();

        beginOfSeg = data.indexOf("86");
        if (!(beginOfSeg == 0)) {
            throw new RuntimeException("Incorrect format in body of ppp");
        }
        tlvBean = null;
        data += "FFFF";
        TLVBean tlvBeanAux = null;
        while (tlvBeanAux == null) {
            tlvBean = selectTlv(data, "86", "FFFF");
            tlvBeanAux = tlvBean;
            if (tlvBeanAux == null) {
                tlvBean = selectTlv(data, "86", "86");
                if (tlvBean == null) {
                    throw new RuntimeException("Incorrect format in body of ppp");
                }
                String segBody = tlvBean.getTaglen() + tlvBean.getValue();
                array.add(segBody);
                data = data.substring(segBody.length());
            } else {
                array.add(tlvBean.getTaglen() + tlvBean.getValue());
            }

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug (LOG,"bodyOfPPP: " + tlvBean.getTaglen() + tlvBean.getValue());
            }
        }
        String[] body = (String[]) array.toArray(new String[0]);
        ArrayList<String> ppp = new ArrayList<String>();
        ppp.add(head);
        for (int i = 0, n = body.length; i < n; i++) {
            ppp.add(body[i]);
        }
        return ppp.toArray(new String[0]);
    }

    public TLVBean selectTlv(String inputData, String beginTag, String endTag) {
        int beginOfSeg = inputData.indexOf(beginTag);
        String s = inputData.substring(beginOfSeg + beginTag.length());
        if (s.substring(0, 2).equals("83")) {
            int num83 = Integer.parseInt("83", 16) * 2 + 2;
            int num = Integer.parseInt(s.substring(2, 8), 16) * 2 + 8;
            if ((num <= s.length()) && (s.substring(num).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + s.substring(0, 8), s.substring(8, num));
            } else if ((num83 <= s.length()) && (s.substring(num83).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + "83", s.substring(2, num83));
            }
        } else if (s.substring(0, 2).equals("82")) {
            int num82 = Integer.parseInt("82", 16) * 2 + 2;
            int num = Integer.parseInt(s.substring(2, 6), 16) * 2 + 6;
            if ((num <= s.length()) && (s.substring(num).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + s.substring(0, 6), s.substring(6, num));
            } else if ((num82 <= s.length()) && (s.substring(num82).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + "82", s.substring(2, num82));
            }
        } else if (s.substring(0, 2).equals("81")) {
            int num81 = Integer.parseInt("81", 16) * 2 + 2;
            int num = Integer.parseInt(s.substring(2, 4), 16) * 2 + 4;
            if ((num <= s.length()) && (s.substring(num).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + s.substring(0, 4), s.substring(4, num));
            } else if ((num81 <= s.length()) && (s.substring(num81).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + "81", s.substring(2, num81));
            }
        } else {
            int num = Integer.parseInt(s.substring(0, 2), 16) * 2 + 2;
            if ((num <= s.length()) && (s.substring(num).indexOf(endTag) == 0)) {
                return new TLVBean(beginTag + s.substring(0, 2), s.substring(2, num));
            }
        }
        return null;
    }
}
