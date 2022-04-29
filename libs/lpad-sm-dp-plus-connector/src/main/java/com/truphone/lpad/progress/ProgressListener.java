package com.truphone.lpad.progress;

public interface ProgressListener {

    void onAction(String phase,
                  String step,
                  Double percentage,
                  String message);
}
