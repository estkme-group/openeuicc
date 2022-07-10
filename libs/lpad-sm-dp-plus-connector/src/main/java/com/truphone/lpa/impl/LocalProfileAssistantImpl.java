package com.truphone.lpa.impl;

import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.LocalProfileAssistant;
import com.truphone.lpa.LocalProfileInfo;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.apdu.NotificationType;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.worker.AllocateProfileWorker;
import com.truphone.lpad.worker.DeleteProfileWorker;
import com.truphone.lpad.worker.GetEidLpadWorker;
import com.truphone.lpad.worker.LpadWorkerExchange;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalProfileAssistantImpl implements LocalProfileAssistant {
    static final String PROFILE_RESULT_SUCESS = "0";
    static final String TRIGGER_PROFILE_REFRESH = "FF";

    private static final Logger LOG = Logger.getLogger(LocalProfileAssistantImpl.class.getName());

    private final ApduChannel apduChannel;
    private Es9PlusImpl es9Module;

    public LocalProfileAssistantImpl(final ApduChannel apduChannel){
        this.apduChannel = apduChannel;
        es9Module = new Es9PlusImpl();
        LOG.log(Level.INFO, LogStub.getInstance().getTag() + " - SM-DP connection initiated.");
    }

    private boolean checkRspServerURL(final String rspServerUrl) {

        return rspServerUrl.matches("^(https?:\\/\\/)?([\\da-z\\.-]+\\.[a-z\\.]{2,6}|[\\d\\.]+)([\\/:?=&#]{1}[\\da-z\\.-]+)*[\\/\\?]?$");
    }

    @Override
    public boolean enableProfile(final String iccid,
                                final Progress progress) {

        return PROFILE_RESULT_SUCESS.equals(
                new EnableProfileWorker(TextUtil.iccidLittleToBig(iccid), progress, apduChannel).run()
        );
    }

    @Override
    public boolean disableProfile(final String iccid,
                                 final Progress progress) {

        return PROFILE_RESULT_SUCESS.equals(
                new DisableProfileWorker(TextUtil.iccidLittleToBig(iccid), progress, apduChannel).run()
        );
    }

    @Override
    public boolean deleteProfile(final String iccid,
                                final Progress progress) {

        DeleteProfileWorker deleteProfileWorker = new DeleteProfileWorker(progress, apduChannel);

        LpadWorkerExchange<DeleteProfileWorker.DeleteProfileInputParams> exchange =
                new LpadWorkerExchange<>(deleteProfileWorker.new DeleteProfileInputParams(TextUtil.iccidLittleToBig(iccid)));

        return PROFILE_RESULT_SUCESS.equals(deleteProfileWorker.run(exchange));

    }

    @Override
    public void downloadProfile(final String matchingId, final String imei,
                                final DownloadProgress progress) throws Exception {

        new DownloadProfileWorker(matchingId, imei, progress, apduChannel, es9Module).run();
    }

    @Override
    public List<LocalProfileInfo> getProfiles() {
        return new ListProfilesWorker(apduChannel).run();
    }

    @Override
    public String getEID() {

        LpadWorkerExchange<String> exchange = new LpadWorkerExchange<>(ApduUtils.getEIDApdu());

        return new GetEidLpadWorker(new Progress(), apduChannel).run(exchange);
    }

    @Override
    public String allocateProfile(final String mcc) {
        Progress progress = new Progress();

        AllocateProfileWorker allocateProfileWorker = new AllocateProfileWorker(progress, es9Module);

        LpadWorkerExchange<AllocateProfileWorker.AllocateProfileInputParams> exchange
                = new LpadWorkerExchange<>(allocateProfileWorker.new AllocateProfileInputParams(mcc, getEID()));

        return allocateProfileWorker.run(exchange);
    }

    @Override
    public void processPendingNotifications() {

        new HandleNotificationsWorker(apduChannel, es9Module).run(NotificationType.ALL);
    }

    @Override
    public String getDefaultSMDP() {
        //LpadWorkerExchange<String> exchange = new LpadWorkerExchange<>(ApduUtils.setDefaultDpAddressApdu(smdpAddress));
        
        return new GetSMDPAddressWorker(apduChannel, new Progress()).run();
        
    }

    @Override
    public String setDefaultSMDP(String smdpAddress, Progress progress) {

        return new SetSMDPAddressWorker(apduChannel, progress, smdpAddress).run();

    }

    @Override
    public boolean setNickname(String iccid, String nickname) {
        return new SetNicknameWorker(TextUtil.iccidLittleToBig(iccid), nickname, apduChannel).run();
    }

    public void smdsRetrieveEvents(Progress progress) {
//        return new SmdsRetrieveEvents();
    }
}
