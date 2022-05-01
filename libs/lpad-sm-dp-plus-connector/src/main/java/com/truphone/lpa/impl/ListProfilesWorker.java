package com.truphone.lpa.impl;

import com.truphone.rsp.dto.asn1.rspdefinitions.ProfileInfo;
import com.truphone.rsp.dto.asn1.rspdefinitions.ProfileInfoListResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.util.LogStub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class ListProfilesWorker {
    private static final Logger LOG = Logger.getLogger(ListProfilesWorker.class.getName());

    private final ApduChannel apduChannel;

    ListProfilesWorker(ApduChannel apduChannel) {

        this.apduChannel = apduChannel;
    }

    List<Map<String, String>> run() {
        String profilesInfo = getProfileInfoListResponse();
        ProfileInfoListResponse profiles = new ProfileInfoListResponse();
        List<Map<String, String>> profileList = new ArrayList<>();

        try {
            decodeProfiles(profilesInfo, profiles);

            for (ProfileInfo info : profiles.getProfileInfoListOk().getProfileInfo()) {
                Map<String, String> profileMap = new HashMap<>();

                profileMap.put(ProfileKey.STATE.name(), LocalProfileAssistantImpl.DISABLED_STATE.equals(info.getProfileState().toString()) ? "Disabled" : "Enabled");
                profileMap.put(ProfileKey.ICCID.name(), info.getIccid().toString());
                profileMap.put(ProfileKey.NAME.name(), (info.getProfileName()!=null)?info.getProfileName().toString():"");
                profileMap.put(ProfileKey.NICKNAME.name(), (info.getProfileNickname()!=null)?info.getProfileNickname().toString():"");
                profileMap.put(ProfileKey.PROVIDER_NAME.name(), (info.getServiceProviderName()!=null)?info.getServiceProviderName().toString():"");
                profileMap.put(ProfileKey.ISDP_AID.name(), (info.getIsdpAid()!=null)?info.getIsdpAid().toString():"");
                profileMap.put(ProfileKey.PROFILE_CLASS.name(), (info.getProfileClass()!=null)?info.getProfileClass().toString():"");
                profileMap.put(ProfileKey.PROFILE_STATE.name(), info.getProfileState().toString());

                profileList.add(profileMap);
            }

            if (LogStub.getInstance().isDebugEnabled()) {
                LogStub.getInstance().logDebug (LOG, LogStub.getInstance().getTag() + " - getProfiles - returning: " + profileList.toString());
            }

            return profileList;

        } catch (DecoderException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " -  Unable to retrieve profiles. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve profiles");
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + ioe.getMessage(), ioe);

            throw new RuntimeException("Unable to retrieve profiles");
        }
    }

    private void decodeProfiles(String profilesInfo, ProfileInfoListResponse profiles) throws DecoderException, IOException {
        InputStream is = new ByteArrayInputStream(Hex.decodeHex(profilesInfo.toCharArray()));

        profiles.decode(is);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"Profile list object: " + profiles.toString());
        }
    }

    private String getProfileInfoListResponse() {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG, LogStub.getInstance().getTag() + " - Getting Profiles");
        }

        String apdu = ApduUtils.getProfilesInfoApdu(null);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug (LOG,"List profiles APDU: " + apdu);
        }

        return apduChannel.transmitAPDU(apdu);
    }
}
