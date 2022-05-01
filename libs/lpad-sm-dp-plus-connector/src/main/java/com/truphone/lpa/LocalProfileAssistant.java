package com.truphone.lpa;

import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpad.progress.Progress;

import java.util.List;
import java.util.Map;

public interface LocalProfileAssistant {

    String enableProfile(String iccid, Progress progress);

    String disableProfile(String iccid, Progress progress);

    String deleteProfile(String iccid, Progress progress);

    String getDefaultSMDP();

    String setDefaultSMDP(String smdpAddress, Progress progress);

    
    void downloadProfile(String matchingId, DownloadProgress progress) throws Exception;
    
    List<Map<String, String>> getProfiles();

    /**
     * Gets the EID from the eUICC
     * @return the EID from the eUICC
     */
    String getEID();

    /**
     * Allocates the Protected Profile Packages to specified EIDs based on given MCC
     * @param mcc Mobile country code
     * @return Activation Code Token
     */
    String allocateProfile(String mcc);

    void processPendingNotifications();

    boolean setNickname(String iccid, String nickname);
}