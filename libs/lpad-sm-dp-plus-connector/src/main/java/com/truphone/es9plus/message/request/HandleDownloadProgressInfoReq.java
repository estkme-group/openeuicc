package com.truphone.es9plus.message.request;

import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

import java.util.Date;

@MsgType("/gsma/rsp2/es2plus/handleDownloadProgressInfo")
public class HandleDownloadProgressInfoReq extends RequestMsgBody {
	private String eid;
	private String iccid;
	private String profileType;
	private Date timestamp;
	private int notificationPointId;
	private String notificationPointStatus;
	private String resultData;

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getIccid() {
		return iccid;
	}

	public void setIccid(String iccid) {
		this.iccid = iccid;
	}

	public String getProfileType() {
		return profileType;
	}

	public void setProfileType(String profileType) {
		this.profileType = profileType;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public int getNotificationPointId() {
		return notificationPointId;
	}

	public void setNotificationPointId(int notificationPointId) {
		this.notificationPointId = notificationPointId;
	}

	public String getNotificationPointStatus() {
		return notificationPointStatus;
	}

	public void setNotificationPointStatus(String notificationPointStatus) {
		this.notificationPointStatus = notificationPointStatus;
	}

	public String getResultData() {
		return resultData;
	}

	public void setResultData(String resultData) {
		this.resultData = resultData;
	}

}
