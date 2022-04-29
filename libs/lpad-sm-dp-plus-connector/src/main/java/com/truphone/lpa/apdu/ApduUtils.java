package com.truphone.lpa.apdu;

import org.apache.commons.lang3.StringUtils;
import com.truphone.util.ToTLV;
import com.truphone.util.Tools;

import java.util.ArrayList;
import java.util.List;

public class ApduUtils {

    private static final String CLA = "81";
    private static final String INSTRUCTION = "E2";
    private static final String P1_11 = "11";
    private static final String P1_91 = "91";
    private static final String P2 = "00";
    private static final int len = 120;

    public static String getEuiccInfo1Apdu() {
        String data = ToTLV.toTLV("BF20", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getEuiccInfo2Apdu() {
        String data = ToTLV.toTLV("BF22", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getEUICCChallengeApdu() {
        String data = ToTLV.toTLV("BF2E", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static List<String> authenticateServerApdu(String smdpSigned1, String smdpSignature1, String euiccCiPKIdToBeUsed,
            String cert, String matchingId) {
        String sctxParams1 = ToTLV.toTLV("A0", ToTLV.toTLV("80", matchingId) + ToTLV.toTLV("A1", ToTLV.toTLV("80", "35550607") + ToTLV.toTLV("A1", "")));
        String data = ToTLV.toTLV("BF38", smdpSigned1 + smdpSignature1 + euiccCiPKIdToBeUsed + cert + sctxParams1);

        return subCommandData(data, len, false);
    }

    public static List<String> prepareDownloadApdu(String smdpSigned2, String smdpSignature2, String cert, String hashCc) {
        StringBuilder data = new StringBuilder().append(smdpSigned2).append(smdpSignature2);
        if (hashCc != null) {
            data.append(ToTLV.toTLV("04", hashCc));
        }
        data.append(cert);
        return subCommandData(ToTLV.toTLV("BF21", data.toString()), len, false);
    }

    public static List<String> loadInitialiseSecureChannelApdu(List<String[]> data) {
        if (data.size() != 4 && data.size() != 5) {
            throw new RuntimeException("SBPP Error");
        }
        return subCommandData(data.get(0)[0], len, true);
    }

    public static List<String> loadConfigureISDPApdu(List<String[]> data) {
        if (data.size() != 4 && data.size() != 5) {
            throw new RuntimeException("SBPP Error");
        }
        List<String> SBPPList = new ArrayList<>();
        String configureISDP = data.get(1)[0];
        String configureISDPLength = Tools.toHex(String.valueOf(configureISDP.length() / 2));
        StringBuilder configureISDPApdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2)
                .append(configureISDPLength).append(configureISDP);
        SBPPList.add(configureISDPApdu.toString());
        return SBPPList;
    }

    public static List<String> loadStoreMetadataApdu(List<String[]> data) {
        if (data.size() != 4 && data.size() != 5) {
            throw new RuntimeException("SBPP Error");
        }
        List<String> SBPPList = new ArrayList<>();

        String[] storeMetadata = data.get(2);
        for (int i = 0; i < storeMetadata.length; i++) {
            List<String> storeMetadataList = subCommandData(storeMetadata[i], len, true);
            SBPPList.addAll(storeMetadataList);
        }
        return SBPPList;
    }

    public static List<String> loadProfileProtectionKeys(List<String[]> data) {
        if (data.size() != 5) {
            throw new RuntimeException("SBPP Error");
        }
        
        List<String> SBPPList = new ArrayList<>();
        String[] profileProtectionKeys = data.get(3);
        
        for (int i = 0; i < profileProtectionKeys.length; i++) {
            List<String> loadProfileElementsList = subCommandData(profileProtectionKeys[i], len, true);
            SBPPList.addAll(loadProfileElementsList);
        }

        return SBPPList;
        
    }

    public static List<String> loadBoundProfilePackageApdu(List<String[]> data) {

        if (data.size() != 4 && data.size() != 5) {
            throw new RuntimeException("SBPP Error");
        }

        List<String> SBPPList = new ArrayList<>();
        String[] loadProfileElements = (data.size() == 4) ? data.get(3) : data.get(4);

        for (int i = 0; i < loadProfileElements.length; i++) {
            List<String> loadProfileElementsList = subCommandData(loadProfileElements[i], len, true);
            SBPPList.addAll(loadProfileElementsList);
        }

        return SBPPList;
    }

    public static String removeNotificationFromListApdu(int notifycounter) {
        String data = ToTLV.toTLV("BF30", ToTLV.integerToTLV("80", notifycounter));
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String listNotificationApdu(String notificationType) {
        String data;

        if (StringUtils.isNotBlank(notificationType)) {
            data = ToTLV.toTLV("BF28", ToTLV.toTLV("81", "04" + notificationType));
        } else {
            data = ToTLV.toTLV("BF28", "");
        }

        return new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data)).toString();
    }

    public static String retrievePendingNotificationsListApdu(int notifyCounter) {
        
            String data = ToTLV.toTLV("BF2B", ToTLV.toTLV("A0", ToTLV.integerToTLV("80", notifyCounter)));
       
        
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));

        return apdu.toString();
    }

    public static String getProfilesInfoApdu(String isdp1) {
        String searchCriteria = "";
        if (!StringUtils.isEmpty(isdp1)) {
            searchCriteria = ToTLV.toTLV("A0", ToTLV.toTLV("4F", isdp1));
        }
        String data = ToTLV.toTLV("BF2D", searchCriteria);// + tagList);
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getEIDApdu() {
        String data = ToTLV.toTLV("BF3E", ToTLV.toTLV("5C", "5A"));
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String setNicknameApdu(String iccid, String profileNickname) {
        String data = ToTLV.toTLV("BF29", ToTLV.toTLV("5A", iccid) + ToTLV.toTLV("90", profileNickname));
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String enableProfileApdu(String iccidOrISDPaid, String refreshflag) {
        String data;
        if (iccidOrISDPaid.length() / 2 == 10) {
            data = ToTLV.toTLV("BF31", ToTLV.toTLV("A0", ToTLV.toTLV("5A", iccidOrISDPaid)) + ToTLV.toTLV("81", refreshflag));
        } else if (iccidOrISDPaid.length() / 2 == 16) {
            data = ToTLV.toTLV("BF31", ToTLV.toTLV("A0", ToTLV.toTLV("4F", iccidOrISDPaid)) + ToTLV.toTLV("81", refreshflag));
        } else {
            throw new RuntimeException("No iccid Or ISDPaid supplied");
        }
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String disableProfileApdu(String iccidOrISDPaid, String refreshflag) {
        String data;
        if (iccidOrISDPaid.length() / 2 == 10) {
            data = ToTLV.toTLV("BF32", ToTLV.toTLV("A0", ToTLV.toTLV("5A", iccidOrISDPaid)) + ToTLV.toTLV("81", refreshflag));
        } else if (iccidOrISDPaid.length() / 2 == 16) {
            data = ToTLV.toTLV("BF32", ToTLV.toTLV("A0", ToTLV.toTLV("4F", iccidOrISDPaid)) + ToTLV.toTLV("81", refreshflag));
        } else {
            throw new RuntimeException("No iccid Or ISDPaid supplied");
        }
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String deleteProfileApdu(String iccidOrISDPaid) {
        String data;
        if (iccidOrISDPaid.length() / 2 == 10) {
            data = ToTLV.toTLV("BF33", ToTLV.toTLV("5A", iccidOrISDPaid));
        } else if (iccidOrISDPaid.length() / 2 == 16) {
            data = ToTLV.toTLV("BF33", ToTLV.toTLV("4F", iccidOrISDPaid));
        } else {
            throw new RuntimeException("No iccid Or ISDPaid supplied");
        }

        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getEuiccConfiguredAddressesApdu() {
        String data = "BF3C00";
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));

        return apdu.toString();
    }

    public static String setDefaultDpAddressApdu(String dpAddrNew) {
        String data = ToTLV.toTLV("BF3F", ToTLV.toTLV("80", dpAddrNew));
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getProfilesInfo_profileStateApdu(String iccidOrISDPaid) {
        String taglist = ToTLV.toTLV("5C", "9F70");
        String searchCriteria = null;
        if (iccidOrISDPaid.length() / 2 == 10) {
            searchCriteria = ToTLV.toTLV("A0", ToTLV.toTLV("5A", iccidOrISDPaid));
        } else if (iccidOrISDPaid.length() / 2 == 16) {
            searchCriteria = ToTLV.toTLV("A0", ToTLV.toTLV("4F", iccidOrISDPaid));
        }
        String data = ToTLV.toTLV("BF2D", searchCriteria + taglist);
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getEUICCInfo_spaceApdu() {
        String data = ToTLV.toTLV("BF22", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getProfilesInfo_ISDPaidApdu(String iccid) {
        String taglist = ToTLV.toTLV("5C", "4F");
        String searchCriteria = ToTLV.toTLV("A0", ToTLV.toTLV("5A", iccid));
        String data = ToTLV.toTLV("BF2D", searchCriteria + taglist);
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String checkIfAnyEnabledProfileApdu() {
        String taglist = ToTLV.toTLV("5C", "9F70");
        String searchCriteria = "";
        String data = ToTLV.toTLV("BF2D", searchCriteria + taglist);
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getNotifyCounterApdu() {
        String data = ToTLV.toTLV("BF28", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String getNotifyTypeApdu() {
        String data = ToTLV.toTLV("BF28", "");
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String removeNotificationApdu(String notifyCounter) {
        String data = ToTLV.toTLV("BF30", ToTLV.toTLV("80", notifyCounter));
        StringBuilder apdu = new StringBuilder().append(CLA).append(INSTRUCTION).append(P1_91).append(P2).append(ToTLV.toTLV(data));
        return apdu.toString();
    }

    public static String nextNotifyCounterApdu(String iccGp_notifycounter) {
        String nextNotifyCounter = "";
        int inotifycounter = Integer.valueOf(iccGp_notifycounter, 16);
        String inotifycounterHex = Tools.toHex(String.valueOf(++inotifycounter));
        inotifycounterHex = "000000" + inotifycounterHex;
        if (inotifycounter < 127) {
            nextNotifyCounter = inotifycounterHex.substring(inotifycounterHex.length() - 2);
        } else if (inotifycounter < 32767) {
            nextNotifyCounter = inotifycounterHex.substring(inotifycounterHex.length() - 4);
        } else if (inotifycounter < 8388607) {
            nextNotifyCounter = inotifycounterHex.substring(inotifycounterHex.length() - 6);
        } else {
            nextNotifyCounter = inotifycounterHex.substring(inotifycounterHex.length() - 8);
        }
        return nextNotifyCounter;
    }

    private static List<String> subCommandData(String data, int len, boolean isLenSub) {
        List<String> commandDataList = new ArrayList<>();
        int dataLen = data.length() / 2;
        int cP2 = 0;
        while (dataLen != 0) {
            if (dataLen > len) {
                String subData = data.substring(0, 2 * len);
                StringBuilder apdu = new StringBuilder();
                if (isLenSub) {
                    apdu.append(CLA).append(INSTRUCTION).append(P1_11).append(Tools.toHex(String.valueOf(cP2))).append(Tools.itoa(len, 1)).append(subData);
                } else {
                    apdu.append(CLA).append(INSTRUCTION).append(P1_11).append(Tools.toHex(String.valueOf(cP2))).append(Tools.toHex(String.valueOf(len))).append(subData);
                }
                commandDataList.add(apdu.toString());
                data = data.substring(2 * len);
                dataLen = dataLen - len;
                cP2++;
            } else {
                StringBuilder apdu = new StringBuilder();
                if (isLenSub) {
                    apdu.append(CLA).append(INSTRUCTION).append(P1_91).append(Tools.toHex(String.valueOf(cP2))).append(Tools.itoa(dataLen, 1)).append(data);
                } else {
                    apdu.append(CLA).append(INSTRUCTION).append(P1_91).append(Tools.toHex(String.valueOf(cP2))).append(Tools.toHex(String.valueOf(dataLen))).append(data);
                }
                dataLen = 0;
                commandDataList.add(apdu.toString());
            }
        }
        return commandDataList;
    }

    public static String getResponse() {
        StringBuilder apdu = new StringBuilder();
        apdu.append(CLA).append("C0").append("00").append("00").append("00");
        return apdu.toString();
    }

    /**
     * * Proprietary Commands *
     */
    public static String getLPAeDownloadProfileApdu() {
        StringBuilder apdu = new StringBuilder();
        apdu.append(CLA).append("B0").append("00").append("00").append("01").append("02");
        return apdu.toString();
    }

    public static String getLPAeGetProgressInfoApdu() {
        StringBuilder apdu = new StringBuilder();
        apdu.append(CLA).append("B0").append("00").append("00").append("01").append("08");
        return apdu.toString();
    }

    public static String getLPAeMemoryResetApdu() {
        StringBuilder apdu = new StringBuilder();
        apdu.append(CLA).append("B0").append("00").append("00").append("01").append("06");
        return apdu.toString();
    }

    public static String getLPAeSetLPAModeApdu(String mode) {
        StringBuilder apdu = new StringBuilder();
        apdu.append(CLA).append("B0").append("00").append("00").append("02").append("09").append(mode);
        return apdu.toString();
    }

    public static String getSendStatusAPDU() {
        return "80F20100";
    }
}
