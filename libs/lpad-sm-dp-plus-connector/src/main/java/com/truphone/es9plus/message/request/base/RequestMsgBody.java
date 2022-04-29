package com.truphone.es9plus.message.request.base;


import com.truphone.es9plus.message.MsgBody;

import java.security.InvalidParameterException;

public abstract class RequestMsgBody implements MsgBody {

    public void checkParameters() throws InvalidParameterException {
    }
}
