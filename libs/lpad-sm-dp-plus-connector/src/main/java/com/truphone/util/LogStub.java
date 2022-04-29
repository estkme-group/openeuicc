package com.truphone.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogStub {
    private static LogStub instance;

    private Level logLevel;
    private String tag;
    private boolean androidLog;

    private LogStub() {

        logLevel = Level.ALL;
        tag = "";
        androidLog = false;
    }

    public static LogStub getInstance() {

        if (instance == null) {
            instance = new LogStub();
        }

        return instance;
    }

    public void setLogLevel(Level logLevel) {

        this.logLevel = logLevel;
    }

    public boolean isDebugEnabled() {

        return logLevel.intValue() <= Level.FINE.intValue();
    }

    private boolean isTraceEnabled() {

        return logLevel.intValue() <= Level.FINEST.intValue();
    }

    public String getTag() {

        return tag;
    }

    public void setTag(String tag) {

        this.tag = tag;
    }

    public void setAndroidLog(boolean androidLog) {

        this.androidLog = androidLog;
    }

    private boolean isAndroidLog() {

        return androidLog;
    }

    public void logDebug(Logger logger, String message) {
        logger.info(message);
//        if (isAndroidLog()) {
//            logger.info(message);
//        } else {
//            //logger.fine(message);
//            System.out.println(message);
//        }
    }
}
