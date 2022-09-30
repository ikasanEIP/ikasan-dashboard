package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLockBuilder {

    private String lockName;
    private Map<String, List<SchedulerJob>> schedulerJobs;
    private int lockCount = 1;

    public JobLockBuilder() {
    }

    public JobLockBuilder withLockName(String lockName) {
        this.lockName = lockName;
        return this;
    }

    public JobLockBuilder withJob(String contextName, SchedulerJob job) {
        if(this.schedulerJobs == null) {
            schedulerJobs = new HashMap<>();
        }

        if(!schedulerJobs.containsKey(contextName)) {
            schedulerJobs.put(contextName, new ArrayList<>());
        }

        schedulerJobs.get(contextName).add(job);
        return this;
    }

    public JobLockBuilder withLockCount(Integer lockCount) {
        this.lockCount = lockCount;
        return this;
    }

    public List<JobLock> build() {
        JobLock jobLock = new JobLockImpl();
        jobLock.setName(lockName);
        jobLock.setJobs(schedulerJobs);
        jobLock.setLockCount(lockCount);
        return List.of(jobLock);
    }
}
