package com.truphone.es9plus.message.response.base;

import com.truphone.es9plus.message.MsgBody;

public abstract class ResponseMsgBody implements MsgBody {
	private HeaderResp header;

	public HeaderResp getHeader() {
		return header;
	}

	public void setHeader(HeaderResp header) {
		this.header = header;
	}

}
