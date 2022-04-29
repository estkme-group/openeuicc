package com.truphone.es9plus.message.request;


import com.truphone.es9plus.message.MsgType;
import com.truphone.es9plus.message.request.base.RequestMsgBody;

@MsgType("/gsma/rsp2/es2plus/confirmOrder")
public class ConfirmOrderReq extends RequestMsgBody {
	private String iccid;              
	private String eid;                
	private String matchingId;       
	private String comfirmationCode;  
	private String smdsAddress;       
	private boolean releaseFlag;      
	public String getIccid() {
		return iccid;
	}
	public void setIccid(String iccid) {
		this.iccid = iccid;
	}
	public String getEid() {
		return eid;
	}
	public void setEid(String eid) {
		this.eid = eid;
	}
	public String getMatchingId() {
		return matchingId;
	}
	public void setMatchingId(String matchingId) {
		this.matchingId = matchingId;
	}
	public String getComfirmationCode() {
		return comfirmationCode;
	}
	public void setComfirmationCode(String comfirmationCode) {
		this.comfirmationCode = comfirmationCode;
	}
	public String getSmdsAddress() {
		return smdsAddress;
	}
	public void setSmdsAddress(String smdsAddress) {
		this.smdsAddress = smdsAddress;
	}
	public boolean getReleaseFlag() {
		return releaseFlag;
	}
	public void setReleaseFlag(boolean releaseFlag) {
		this.releaseFlag = releaseFlag;
	}
	

}
