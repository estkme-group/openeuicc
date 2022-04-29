package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es9plus/authenticateClient")
public class AuthenticateClientReq extends RequestMsgBody {
	private String transactionId;
	private String authenticateServerResponse;

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getAuthenticateServerResponse() {
		return authenticateServerResponse;
	}

	public void setAuthenticateServerResponse(String authenticateServerResponse) {
		this.authenticateServerResponse = authenticateServerResponse;
	}

}
