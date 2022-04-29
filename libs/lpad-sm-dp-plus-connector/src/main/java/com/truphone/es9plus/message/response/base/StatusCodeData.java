package com.truphone.es9plus.message.response.base;

public class StatusCodeData {

	private String subjectCode;

	private String reasonCode;

	private String message;

	private String subjectIdentifier;

	public StatusCodeData() {
		super();
	}

	public StatusCodeData(String subjectCode, String reasonCode, String message) {
		super();
		this.subjectCode = subjectCode;
		this.reasonCode = reasonCode;
		this.message = message;
	}

	public StatusCodeData(String subjectCode, String reasonCode, String message, String subjectIdentifier) {
		super();
		this.subjectCode = subjectCode;
		this.reasonCode = reasonCode;
		this.message = message;
		this.subjectIdentifier = subjectIdentifier;
	}

	public String getSubjectCode() {
		return subjectCode;
	}

	public void setSubjectCode(String subjectCode) {
		this.subjectCode = subjectCode;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSubjectIdentifier() {
		return subjectIdentifier;
	}

	public void setSubjectIdentifier(String subjectIdentifier) {
		this.subjectIdentifier = subjectIdentifier;
	}

}
