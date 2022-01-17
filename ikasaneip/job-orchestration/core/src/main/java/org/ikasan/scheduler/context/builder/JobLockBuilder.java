package org.ikasan.scheduler.context.builder;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLockBuilder {

    private String lockName;
    private List<SchedulerJob> schedulerJobs;

    protected JobLockBuilder() {
    }

    public JobLockBuilder withLockName(String lockName) {
        this.lockName = lockName;
        return this;
    }

    public JobLockBuilder withJob(SchedulerJob job) {
        if(this.schedulerJobs == null) {
            schedulerJobs = new ArrayList<>();
        }

        schedulerJobs.add(job);

        return this;
    }

    public Map<String, List<SchedulerJob>> build() {
        Map<String, List<SchedulerJob>> jobLocks =  new HashMap<>();
        jobLocks.put(this.lockName, this.schedulerJobs);

        return jobLocks;
    }
}
