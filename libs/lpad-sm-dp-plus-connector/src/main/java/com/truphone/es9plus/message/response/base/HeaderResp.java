package com.truphone.es9plus.message.response.base;

public class HeaderResp {
	private FunctionExecutionStatus functionExecutionStatus;

	public HeaderResp(FunctionExecutionStatus functionExecutionStatus) {
		super();
		this.functionExecutionStatus = functionExecutionStatus;
	}

	public FunctionExecutionStatus getFunctionExecutionStatus() {
		return functionExecutionStatus;
	}

	public void setFunctionExecutionStatus(FunctionExecutionStatus functionExecutionStatus) {
		this.functionExecutionStatus = functionExecutionStatus;
	}

}
