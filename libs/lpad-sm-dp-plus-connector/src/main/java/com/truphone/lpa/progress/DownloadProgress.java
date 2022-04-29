package com.truphone.lpa.progress;

import com.truphone.lpad.progress.ProgressListener;
import com.truphone.lpad.progress.ProgressStep;
import com.truphone.util.LogStub;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class DownloadProgress {
    private static final Logger LOG = Logger.getLogger(DownloadProgress.class.getName());

    private final Executor executor;

    private ProgressListener progressListener;
    private DownloadProgressPhase downloadProgressPhase;
    private int totalPhaseSteps;
    private int currentPhaseStepSum;

    public DownloadProgress() {

        executor = Executors.newSingleThreadExecutor();
        downloadProgressPhase = DownloadProgressPhase.AUTHENTICATING;
        totalPhaseSteps = DownloadProgressPhase.AUTHENTICATING.getProgressSteps().size();
        currentPhaseStepSum = 0;
    }

    public void stepExecuted(final ProgressStep step,
                             final String message) {

        if (progressListener != null) {
            ++currentPhaseStepSum;

            final DownloadProgressPhase downloadProgressPhaseAux = downloadProgressPhase;
            final int totalPhaseStepsAux = totalPhaseSteps;
            final int currentPhaseStepSumAux = currentPhaseStepSum;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    double percentage;

                    if (LogStub.getInstance().isDebugEnabled()) {
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - stepExecuted - step: " + step + "; currentPhaseStepSum: " + currentPhaseStepSumAux +
                                "; message: " + message + "; currentProgressPhase: " + downloadProgressPhaseAux +
                                "; totalPhaseSteps: " + totalPhaseStepsAux);
                    }

                    percentage = calculatePercentage(downloadProgressPhaseAux, currentPhaseStepSumAux, totalPhaseStepsAux);

                    if (LogStub.getInstance().isDebugEnabled()) {
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - stepExecuted - onAction - step: " + step.name() +
                                "; message: " + message + "; percentage: " + percentage +
                                "; downloadProgressPhase: " + downloadProgressPhaseAux.name());
                    }

                    progressListener.onAction(downloadProgressPhaseAux.name(), step.name(), percentage, message);
                }

                private double calculatePercentage(final DownloadProgressPhase downloadProgressPhaseAux,
                                                   final int currentPhaseStepSumAux,
                                                   final int totalPhaseStepsAux) {
                    double percentage;

                    if (currentPhaseStepSumAux == totalPhaseStepsAux) {
                        percentage = 1.0;
                    } else {
                        percentage = (double) currentPhaseStepSumAux / totalPhaseStepsAux;

                        if (percentage > 1.0) {
                            percentage = 1.0;
                        }
                    }

                    if (LogStub.getInstance().isDebugEnabled()) {
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - calculatePercentage - percentage_1: " + percentage);
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - calculatePercentage - percentage_f: " +
                                (percentage * downloadProgressPhaseAux.getPhaseTotalPercentage() / 1.0 + getTotalPreviousPhases(downloadProgressPhaseAux)));
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - calculatePercentage - previous: " + getTotalPreviousPhases(downloadProgressPhaseAux));
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - calculatePercentage - current: " + (percentage * downloadProgressPhaseAux.getPhaseTotalPercentage()));
                    }

                    percentage = percentage * downloadProgressPhaseAux.getPhaseTotalPercentage() / 1.0 + getTotalPreviousPhases(downloadProgressPhaseAux);

                    if (LogStub.getInstance().isDebugEnabled()) {
                        LogStub.getInstance().logDebug(LOG, LogStub.getInstance().getTag() +
                                " - DownloadProgress - calculatePercentage - final: " + percentage);
                    }

                    return percentage > 1.0 ? 1.0 : percentage;
                }

                private double getTotalPreviousPhases(DownloadProgressPhase downloadProgressPhase) {
                    int currentPhaseNumber = downloadProgressPhase.ordinal();
                    double total = 0.0;

                    for (int i = 0; i < currentPhaseNumber; i++) {
                        total += DownloadProgressPhase.values()[i].getPhaseTotalPercentage();
                    }

                    return total;
                }
            });
        }
    }

    public void setProgressListener(ProgressListener progressListener) {

        this.progressListener = progressListener;
    }

    public void setCurrentPhase(DownloadProgressPhase downloadProgressPhase,
                                int totalPhaseSteps) {

        this.downloadProgressPhase = downloadProgressPhase;
        this.totalPhaseSteps = totalPhaseSteps;
        currentPhaseStepSum = 0;

    }

    public void setCurrentPhase(DownloadProgressPhase downloadProgressPhase) {

        setCurrentPhase(downloadProgressPhase, downloadProgressPhase.getProgressSteps().size());
    }
}
