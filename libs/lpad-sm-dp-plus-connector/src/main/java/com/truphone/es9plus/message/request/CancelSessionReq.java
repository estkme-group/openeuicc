package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es9plus/cancelSession")
public class CancelSessionReq extends RequestMsgBody {
	private String transactionId;
	private String euiccCancelSessionSigned;
	private String euiccCancelSessionSignature;

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getEuiccCancelSessionSigned() {
		return euiccCancelSessionSigned;
	}

	public void setEuiccCancelSessionSigned(String euiccCancelSessionSigned) {
		this.euiccCancelSessionSigned = euiccCancelSessionSigned;
	}

	public String getEuiccCancelSessionSignature() {
		return euiccCancelSessionSignature;
	}

	public void setEuiccCancelSessionSignature(String euiccCancelSessionSignature) {
		this.euiccCancelSessionSignature = euiccCancelSessionSignature;
	}

}
