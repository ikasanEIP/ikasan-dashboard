package org.ikasan.scheduled.job.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class SolrJobLockHolderImpl implements JobLockHolder {
    private String lockName;
    private long lockCount = 1;
    private final List<SchedulerJob> schedulerJobs = new ArrayList<>();
    private final Set<String> lockHolders = new HashSet<>();

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public long getLockCount() {
        return lockCount;
    }

    public void setLockCount(long lockCount) {
        this.lockCount = lockCount;
    }

    public List<SchedulerJob> getSchedulerJobs() {
        return schedulerJobs;
    }

    public void addSchedulerJobs(List<SchedulerJob> jobs) {
        this.schedulerJobs.addAll(jobs);
    }

    public Set<String> getLockHolders() {
        return lockHolders;
    }

    public void addLockHolder(String jobIdentifier) {
        lockHolders.add(jobIdentifier);
    }

    public boolean removeLockHolder(String jobIdentifier) {
        return lockHolders.remove(jobIdentifier);
    }
}
