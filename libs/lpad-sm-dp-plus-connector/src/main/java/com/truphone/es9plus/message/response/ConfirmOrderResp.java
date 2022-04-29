package com.truphone.es9plus.message.response;

import com.truphone.es9plus.message.response.base.ResponseMsgBody;

public class ConfirmOrderResp extends ResponseMsgBody {
	private String eid;
	private String matchingId;
	private String smdpAddress;

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getMatchingId() {
		return matchingId;
	}

	public void setMatchingId(String matchingId) {
		this.matchingId = matchingId;
	}

	public String getSmdpAddress() {
		return smdpAddress;
	}

	public void setSmdpAddress(String smdpAddress) {
		this.smdpAddress = smdpAddress;
	}

}
