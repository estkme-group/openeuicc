package com.truphone.lpa.impl;

public class AuthenticateClientSmDp {
    private String smdpSigned2;
    private String smdpSignature2;
    private String smdpCertificate;

    public AuthenticateClientSmDp() {

        super();
    }

    public String getSmdpSigned2() {

        return smdpSigned2;
    }

    public String getSmdpSignature2() {

        return smdpSignature2;
    }

    public String getSmdpCertificate() {

        return smdpCertificate;
    }

    public void setSmdpSigned2(String smdpSigned2) {

        this.smdpSigned2 = smdpSigned2;
    }

    public void setSmdpSignature2(String smdpSignature2) {

        this.smdpSignature2 = smdpSignature2;
    }

    public void setSmdpCertificate(String smdpCertificate) {

        this.smdpCertificate = smdpCertificate;
    }
}