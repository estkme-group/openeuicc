package com.truphone.es9plus.message.response;

import com.truphone.es9plus.message.response.base.ResponseMsgBody;

public class DownloadOrderResp extends ResponseMsgBody {
	private String iccid;                 

	public String getIccid() {
		return iccid;
	}

	public void setIccid(String iccid) {
		this.iccid = iccid;
	}
	

}
