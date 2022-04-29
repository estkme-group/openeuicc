package com.truphone.lpa.impl;

public class InitialAuthenticationKeys {
    private String matchingId;
    private String euiccConfiguredAddress;
    private String euiccInfo1;
    private String euiccChallenge;
    private String serverSigned1;
    private String serverSignature1;
    private String euiccCiPKIdTobeUsed;
    private String serverCertificate;
    private String transactionId;
    private String ctxParams1;

    InitialAuthenticationKeys(String matchingId, String euiccConfiguredAddress, String euiccInfo1, String euiccChallenge) {

        this.matchingId = matchingId;
        this.euiccConfiguredAddress = euiccConfiguredAddress;
        this.euiccInfo1 = euiccInfo1;
        this.euiccChallenge = euiccChallenge;
    }

    public String getMatchingId() {

        return matchingId;
    }

    public String getServerSigned1() {

        return serverSigned1;
    }

    public String getServerSignature1() {

        return serverSignature1;
    }

    public String getEuiccCiPKIdTobeUsed() {

        return euiccCiPKIdTobeUsed;
    }

    public String getServerCertificate() {

        return serverCertificate;
    }

    public String getTransactionId() {

        return transactionId;
    }

    public String getCtxParams1() {

        return ctxParams1;
    }

    public String getEuiccConfiguredAddress() {

        return euiccConfiguredAddress;
    }

    public String getEuiccInfo1() {

        return euiccInfo1;
    }

    public String getEuiccChallenge() {

        return euiccChallenge;
    }

    public void setMatchingId(String matchingId) {

        this.matchingId = matchingId;
    }

    public void setServerSigned1(String serverSigned1) {

        this.serverSigned1 = serverSigned1;
    }

    public void setServerSignature1(String serverSignature1) {

        this.serverSignature1 = serverSignature1;
    }

    public void setEuiccCiPKIdTobeUsed(String euiccCiPKIdTobeUsed) {

        this.euiccCiPKIdTobeUsed = euiccCiPKIdTobeUsed;
    }

    public void setServerCertificate(String serverCertificate) {

        this.serverCertificate = serverCertificate;
    }

    public void setTransactionId(String transactionId) {

        this.transactionId = transactionId;
    }

    public void setCtxParams1(String ctxParams1) {

        this.ctxParams1 = ctxParams1;
    }

    @Override
    public String toString() {
        return "InitialAuthenticationKeys{" +
                "matchingId='" + matchingId + '\'' +
                ", euiccConfiguredAddress='" + euiccConfiguredAddress + '\'' +
                ", euiccInfo1='" + euiccInfo1 + '\'' +
                ", euiccChallenge='" + euiccChallenge + '\'' +
                ", serverSigned1='" + serverSigned1 + '\'' +
                ", serverSignature1='" + serverSignature1 + '\'' +
                ", euiccCiPKIdTobeUsed='" + euiccCiPKIdTobeUsed + '\'' +
                ", serverCertificate='" + serverCertificate + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", ctxParams1='" + ctxParams1 + '\'' +
                '}';
    }
}