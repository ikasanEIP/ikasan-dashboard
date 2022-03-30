package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.List;

public class JobLockBuilder {

    private String lockName;
    private List<SchedulerJob> schedulerJobs;
    private long lockCount = 1;

    public JobLockBuilder() {
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

    public JobLockBuilder withLockCount(Long lockCount) {
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
