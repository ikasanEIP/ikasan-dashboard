package org.ikasan.job.orchestration.util;

public class AggregateContextInstanceStatus {
    private boolean heldJobs = false;
    private boolean skippedJobs = false;
    private boolean disabledJobs = false;

    public boolean isHeldJobs() {
        return heldJobs;
    }

    public void setHeldJobs() {
        this.heldJobs = true;
    }

    public boolean isSkippedJobs() {
        return skippedJobs;
    }

    public void setSkippedJobs() {
        this.skippedJobs = true;
    }

    public boolean isDisabledJobs() {
        return disabledJobs;
    }

    public void setDisabledJobs() {
        this.disabledJobs = true;
    }
}
