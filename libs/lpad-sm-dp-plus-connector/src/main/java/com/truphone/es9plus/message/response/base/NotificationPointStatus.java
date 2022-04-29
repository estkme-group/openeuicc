package com.truphone.es9plus.message.response.base;

public class NotificationPointStatus {

	private String status;

	private StatusCodeData statusCodeData;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public StatusCodeData getStatusCodeData() {
		return statusCodeData;
	}

	public void setStatusCodeData(StatusCodeData statusCodeData) {
		this.statusCodeData = statusCodeData;
	}
}
