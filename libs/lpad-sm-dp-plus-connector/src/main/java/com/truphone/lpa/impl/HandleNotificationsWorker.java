package com.truphone.lpa.impl;

import com.truphone.rsp.dto.asn1.rspdefinitions.*;
import com.beanit.asn1bean.ber.ReverseByteArrayOutputStream;
import com.truphone.es9plus.Es9PlusImpl;
import com.truphone.lpa.ApduChannel;
import com.truphone.lpa.apdu.ApduUtils;
import com.truphone.lpa.apdu.NotificationType;
import com.truphone.util.LogStub;
import com.truphone.util.TextUtil;
import com.truphone.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HandleNotificationsWorker {
    private static final Logger LOG = Logger.getLogger(HandleNotificationsWorker.class.getName());

    private final ApduChannel apduChannel;
    private final Es9PlusImpl es9Module;

    HandleNotificationsWorker(ApduChannel apduChannel, Es9PlusImpl es9Module) {

        this.apduChannel = apduChannel;
        this.es9Module = es9Module;
    }

    void run(NotificationType notificationType) {
        String notificationList = getNotificationsList(notificationType);
        ListNotificationResponse list = new ListNotificationResponse();

        try {
            decodeNotificationList(notificationList, list);

            for (NotificationMetadata notification : list.getNotificationMetadataList().getNotificationMetadata()) {
                if (LogStub.getInstance().isDebugEnabled()) {
                    LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Notification: " + notification.toString());
                }

                int seqNo = notification.getSeqNumber().intValue();
                RetrieveNotificationsListResponse notificationListResponse = getRetrieveNotificationsListResponse(seqNo);

                handlePendingNotification(seqNo, notificationListResponse);
            }

        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + e.getMessage(), e);
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " -  Unable to retrieve profiles. Exception in Decoder:" + e.getMessage());

            throw new RuntimeException("Unable to retrieve profiles");
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, LogStub.getInstance().getTag() + " - " + ioe.getMessage(), ioe);

            throw new RuntimeException("Unable to retrieve profiles");
        }
    }

    private void decodeNotificationList(String notificationList, ListNotificationResponse list) throws NumberFormatException, IOException {
        InputStream is = new ByteArrayInputStream(TextUtil.decodeHex(notificationList));

        list.decode(is, true);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - List of notifications: " + list.toString());
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Number of notifications: " + list.getNotificationMetadataList().getNotificationMetadata().size());
        }
    }

    private String getNotificationsList(NotificationType notificationType) {

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Listing notification for type: " + notificationType.toString() +
                    "[" + notificationType.name() + "]");
        }

        String listNotificationsApdu = ApduUtils.listNotificationApdu(notificationType.toString());

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Listing notifications apdu: " + listNotificationsApdu);
        }

        String notificationList = apduChannel.transmitAPDU(listNotificationsApdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Notification list response: " + notificationList);
        }

        return notificationList;
    }

    private void handlePendingNotification(int seqNo, RetrieveNotificationsListResponse notificationListResponse) throws IOException, NumberFormatException {

        if (notificationListResponse != null && notificationListResponse.getNotificationList() != null &&
                notificationListResponse.getNotificationList().getPendingNotification() != null)
            for (PendingNotification pendingNotification : notificationListResponse.getNotificationList().getPendingNotification()) {
                String encodedPendingNotification = getEncodedPendingNotification(pendingNotification);
                
                String serverAddress="";
                if(pendingNotification.getProfileInstallationResult()!=null){
                    //It's a PIR
                    serverAddress = pendingNotification.getProfileInstallationResult().getProfileInstallationResultData().getNotificationMetadata().getNotificationAddress().toString();
                }else{
                   serverAddress = pendingNotification.getOtherSignedNotification().getTbsOtherNotification().getNotificationAddress().toString();
                }
                
                es9Module.handleNotification(encodedPendingNotification, serverAddress);

                removeNotification(seqNo);
            }
    }

    private RetrieveNotificationsListResponse getRetrieveNotificationsListResponse(int seqNo) throws NumberFormatException, IOException {
        String retrieveNotificationFromListApdu = ApduUtils.retrievePendingNotificationsListApdu(seqNo);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Retrieving notification: " + retrieveNotificationFromListApdu);
        }

        String notificationResponse = apduChannel.transmitAPDU(retrieveNotificationFromListApdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Notification response: " + notificationResponse);
        }

        return decodeNotificationResponse(notificationResponse);
    }

    private RetrieveNotificationsListResponse decodeNotificationResponse(String notificationResponse) throws NumberFormatException, IOException {
        InputStream is3 = new ByteArrayInputStream(TextUtil.decodeHex(notificationResponse));
        RetrieveNotificationsListResponse notificationListResponse = new RetrieveNotificationsListResponse();

        notificationListResponse.decode(is3, true);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Notification: " + notificationListResponse.toString());
        }

        return notificationListResponse;
    }

    private String getEncodedPendingNotification(PendingNotification pendingNotification) throws IOException {
        ReverseByteArrayOutputStream berByteArrayOutputStream = new ReverseByteArrayOutputStream(4000, true);

        pendingNotification.encode(berByteArrayOutputStream);

        String pendingNotificationStr = Util.byteArrayToHexString(berByteArrayOutputStream.getArray(), "");

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Pending notification: " + pendingNotificationStr);
        }

        String encodedPendingNotification = Base64.getEncoder().encodeToString(Util.hexStringToByteArray(pendingNotificationStr));

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Encoded pending notification: " + encodedPendingNotification);
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Sending notification to SM-DP+");
        }

        return encodedPendingNotification;
    }

    private void removeNotification(int seqNo) throws NumberFormatException, IOException {
        String removeNotificationApdu = ApduUtils.removeNotificationFromListApdu(seqNo);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Remove notification apdu: " + removeNotificationApdu);
        }

        String removeNotificationResponse = apduChannel.transmitAPDU(removeNotificationApdu);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Remove notification response: " + removeNotificationResponse);
        }

        decodeRemoveNotification(removeNotificationResponse);
    }

    private void decodeRemoveNotification(String removeNotificationResponse) throws NumberFormatException, IOException {
        InputStream is2 = new ByteArrayInputStream(TextUtil.decodeHex(removeNotificationResponse));
        NotificationSentResponse notificationSentResponse = new NotificationSentResponse();

        notificationSentResponse.decode(is2, true);

        if (LogStub.getInstance().isDebugEnabled()) {
            LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() + " - Notification removal response: " + notificationSentResponse.toString());
        }
    }
}
