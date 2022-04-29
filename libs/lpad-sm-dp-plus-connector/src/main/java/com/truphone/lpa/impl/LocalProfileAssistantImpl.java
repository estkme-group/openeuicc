package com.truphone.lpa.impl;

import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.LocalProfileAssistant;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.apdu.NotificationType;
import com.truphone.lpa.progress.DownloadProgress;
import com.truphone.lpad.progress.Progress;
import com.truphone.lpad.worker.AllocateProfileWorker;
import com.truphone.lpad.worker.DeleteProfileWorker;
import com.truphone.lpad.worker.GetEidLpadWorker;
import com.truphone.lpad.worker.LpadWorkerExchange;
import com.truphone.util.LogStub;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalProfileAssistantImpl implements LocalProfileAssistant {
    static final String PROFILE_RESULT_SUCESS = "0";
    static final String TRIGGER_PROFILE_REFRESH = "FF";
    static final String DISABLED_STATE = PROFILE_RESULT_SUCESS;

    private static final Logger LOG = Logger.getLogger(LocalProfileAssistantImpl.class.getName());

    private final ApduChannel apduChannel;
    private Es9PlusImpl es9Module;

//    public LocalProfileAssistantImpl(final ApduChannel apduChannel,
//                                     final String rspServerUrl) {
    public LocalProfileAssistantImpl(final ApduChannel apduChannel){
        this.apduChannel = apduChannel;
        es9Module = new Es9PlusImpl();

        //LOG.info(LogStub.getInstance().getTag() + " - Init SM-DP connection - " + rspServerUrl);

//        if (!StringUtils.isNotBlank(rspServerUrl) || !checkRspServerURL(rspServerUrl)) {
//            if (LogStub.getInstance().isDebugEnabled()) {
//                LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Fixing RSP Server URL to default - " + rspServerUrl);
//            }
//
//            throw new IllegalArgumentException("RSP Server URL is invalid: " + rspServerUrl);
//        }

//        es9Module.configure(rspServerUrl);
        //es9Module.configure();
        LOG.log(Level.INFO, LogStub.getInstance().getTag() + " - SM-DP connection initiated.");
    }

    private boolean checkRspServerURL(final String rspServerUrl) {

        return rspServerUrl.matches("^(https?:\\/\\/)?([\\da-z\\.-]+\\.[a-z\\.]{2,6}|[\\d\\.]+)([\\/:?=&#]{1}[\\da-z\\.-]+)*[\\/\\?]?$");
    }

    @Override
    public String enableProfile(final String iccid,
                                final Progress progress) {

        return new EnableProfileWorker(iccid, progress, apduChannel).run();
    }

    @Override
    public String disableProfile(final String iccid,
                                 final Progress progress) {

        return new DisableProfileWorker(iccid, progress, apduChannel).run();
    }

    @Override
    public String deleteProfile(final String iccid,
                                final Progress progress) {

        DeleteProfileWorker deleteProfileWorker = new DeleteProfileWorker(progress, apduChannel);

        LpadWorkerExchange<DeleteProfileWorker.DeleteProfileInputParams> exchange =
                new LpadWorkerExchange<>(deleteProfileWorker.new DeleteProfileInputParams(iccid));

        return deleteProfileWorker.run(exchange);

    }

    @Override
    public void downloadProfile(final String matchingId,
                                final DownloadProgress progress) throws Exception {

        new DownloadProfileWorker(matchingId, progress, apduChannel, es9Module).run();
    }

    @Override
    public List<Map<String, String>> getProfiles() {

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

    public void smdsRetrieveEvents(Progress progress) {
//        return new SmdsRetrieveEvents();
    }
}
