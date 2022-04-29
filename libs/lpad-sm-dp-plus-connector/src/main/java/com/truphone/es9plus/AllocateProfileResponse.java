package com.truphone.es9plus;

public class AllocateProfileResponse {
    private String acFormat;
    private String smDpPlusAddress;
    private String acToken;
    private String smDpPlusOid;
    private String confirmationCodeRequiredFlag;

    public String getAcFormat() {

        return acFormat;
    }

    public void setAcFormat(String acFormat) {

        this.acFormat = acFormat;
    }

    public String getSmDpPlusAddress() {

        return smDpPlusAddress;
    }

    public void setSmDpPlusAddress(String smDpPlusAddress) {

        this.smDpPlusAddress = smDpPlusAddress;
    }

    public String getAcToken() {

        return acToken;
    }

    public void setAcToken(String acToken) {

        this.acToken = acToken;
    }

    public String getSmDpPlusOid() {

        return smDpPlusOid;
    }

    public void setSmDpPlusOid(String smDpPlusOid) {

        this.smDpPlusOid = smDpPlusOid;
    }

    public String getConfirmationCodeRequiredFlag() {

        return confirmationCodeRequiredFlag;
    }

    public void setConfirmationCodeRequiredFlag(String confirmationCodeRequiredFlag) {

        this.confirmationCodeRequiredFlag = confirmationCodeRequiredFlag;
    }
}
