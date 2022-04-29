package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es9plus/handleNotification")
public class HandleNotificationReq extends RequestMsgBody {
	private String pendingNotification;

	public String getPendingNotification() {
		return pendingNotification;
	}

	public void setPendingNotification(String pendingNotification) {
		this.pendingNotification = pendingNotification;
	}

}
