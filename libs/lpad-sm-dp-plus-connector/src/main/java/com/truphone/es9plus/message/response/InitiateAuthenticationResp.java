package com.truphone.es9plus.message.response;


import com.truphone.es9plus.message.response.base.ResponseMsgBody;

public class InitiateAuthenticationResp extends ResponseMsgBody {
    private String transactionId;
    private String serverSigned1;
    private String serverSignature1;
    private String euiccCiPKIdToBeUsed;
    private String serverCertificate;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getServerSigned1() {
        return serverSigned1;
    }

    public void setServerSigned1(String serverSigned1) {
        this.serverSigned1 = serverSigned1;
    }

    public String getServerSignature1() {
        return serverSignature1;
    }

    public void setServerSignature1(String serverSignature1) {
        this.serverSignature1 = serverSignature1;
    }

    public String getEuiccCiPKIdToBeUsed() {
        return euiccCiPKIdToBeUsed;
    }

    public void setEuiccCiPKIdToBeUsed(String euiccCiPKIdToBeUsed) {
        this.euiccCiPKIdToBeUsed = euiccCiPKIdToBeUsed;
    }

    public String getServerCertificate() {
        return serverCertificate;
    }

    public void setServerCertificate(String serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    @Override
    public String toString() {

        return "InitiateAuthenticationResp{" +
                "transactionId='" + transactionId + '\'' +
                ", serverSigned1='" + serverSigned1 + '\'' +
                ", serverSignature1='" + serverSignature1 + '\'' +
                ", euiccCiPKIdToBeUsed='" + euiccCiPKIdToBeUsed + '\'' +
                ", serverCertificate='" + serverCertificate + '\'' +
                '}';
    }
}
