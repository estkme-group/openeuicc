package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es9plus/getBoundProfilePackage")
public class GetBoundProfilePackageReq extends RequestMsgBody {
	private String transactionId;
	private String prepareDownloadResponse;

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getPrepareDownloadResponse() {
		return prepareDownloadResponse;
	}

	public void setPrepareDownloadResponse(String prepareDownloadResponse) {
		this.prepareDownloadResponse = prepareDownloadResponse;
	}

}
