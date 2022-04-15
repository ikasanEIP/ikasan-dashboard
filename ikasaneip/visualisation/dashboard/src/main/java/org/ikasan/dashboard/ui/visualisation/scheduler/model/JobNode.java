package org.ikasan.dashboard.ui.visualisation.scheduler.model;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.stream.IntStream;

public class JobNode {
    private SchedulerJob schedulerJob;
    private int spacing = 20;

    public JobNode(SchedulerJob schedulerJob) {
        this.schedulerJob = schedulerJob;
    }

    public JobNode(SchedulerJob schedulerJob, int spacing) {
        this.schedulerJob = schedulerJob;
        this.spacing = spacing;
    }

    public SchedulerJob getSchedulerJob() {
        return schedulerJob;
    }

    @Override
    public String toString() {
        StringBuffer spacingString = new StringBuffer();
        IntStream.range(0, spacing).forEach(i -> spacingString.append("*"));

        return spacingString.toString();
    }
}
