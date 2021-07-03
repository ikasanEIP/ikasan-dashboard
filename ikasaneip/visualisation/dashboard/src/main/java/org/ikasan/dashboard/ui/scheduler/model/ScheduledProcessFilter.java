package org.ikasan.dashboard.ui.scheduler.model;

public class ScheduledProcessFilter {

    private long startTime;

    private long endTime;

    private String filter;

    private boolean errorsOnly = false;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isErrorsOnly() {
        return errorsOnly;
    }

    public void setErrorsOnly(boolean errorsOnly) {
        this.errorsOnly = errorsOnly;
    }
}
