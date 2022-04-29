package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es9plus/initiateAuthentication")
public class InitiateAuthenticationReq extends RequestMsgBody {
	private String euiccChallenge;
	private String euiccInfo1;
	private String smdpAddress;

	public String getEuiccChallenge() {
		return euiccChallenge;
	}

	public void setEuiccChallenge(String euiccChallenge) {
		this.euiccChallenge = euiccChallenge;
	}

	public String getEuiccInfo1() {
		return euiccInfo1;
	}

	public void setEuiccInfo1(String euiccInfo1) {
		this.euiccInfo1 = euiccInfo1;
	}

	public String getSmdpAddress() {
		return smdpAddress;
	}

	public void setSmdpAddress(String smdpAddress) {
		this.smdpAddress = smdpAddress;
	}

}
