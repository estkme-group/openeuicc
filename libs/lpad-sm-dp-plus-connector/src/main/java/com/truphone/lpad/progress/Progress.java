package com.truphone.lpad.progress;

import com.truphone.util.LogStub;
import com.truphone.lpa.progress.ProgressPhase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Progress {
    private static final Logger LOG = Logger.getLogger(Progress.class.getName());

    private final Executor executor;

    private ProgressListener progressListener;
    private ProgressPhase progressPhase;
    private int totalSteps;
    private int currentStep;

    public Progress() {

        executor = Executors.newSingleThreadExecutor();
        progressPhase = ProgressPhase.RUNNING;
        totalSteps = 1;
        currentStep = 0;
    }

    public void stepExecuted(final ProgressStep step,
                             final String message) {

        if (progressListener != null) {
            ++currentStep;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    double percentage;

                    if (LogStub.getInstance().isDebugEnabled()) {
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - Progress - setAction - phase: " +
                                progressPhase + "; step: " + step + "; currentStep: " + currentStep +
                                "; message: " + message + "; totalSteps: " + totalSteps);
                    }

                    percentage = currentStep / totalSteps;

                    if (percentage > 1.0) {
                        percentage = 1.0;
                    }

                    progressListener.onAction(progressPhase.name(), step.name(), percentage, message);
                }
            });
        }
    }

    public void setProgressListener(ProgressListener progressListener) {

        this.progressListener = progressListener;
    }

    public void setTotalSteps(int totalSteps) {

        this.totalSteps = totalSteps;
    }
}
