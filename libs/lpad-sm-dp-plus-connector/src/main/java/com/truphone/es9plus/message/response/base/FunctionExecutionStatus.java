package com.truphone.es9plus.message.response.base;

public class FunctionExecutionStatus {
	private String status;
	private StatusCodeData statusCodeData;

	public FunctionExecutionStatus(String status) {
		super();
		this.status = status;
	}

	public FunctionExecutionStatus(String status, StatusCodeData statusCodeData) {
		super();
		this.status = status;
		this.statusCodeData = statusCodeData;
	}

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
