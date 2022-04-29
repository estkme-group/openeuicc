package com.truphone.lpa;

import java.util.List;

public interface ApduChannel {
    String transmitAPDU(String apdu);

    String transmitAPDUS(List<String> apdus);

    void sendStatus();

    void setApduTransmittedListener(ApduTransmittedListener apduTransmittedListener);

    void removeApduTransmittedListener(ApduTransmittedListener apduTransmittedListener);
}
