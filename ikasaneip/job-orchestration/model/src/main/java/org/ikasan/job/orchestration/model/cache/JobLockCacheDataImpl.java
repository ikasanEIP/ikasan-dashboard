package org.ikasan.job.orchestration.model.cache;

import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;

import java.util.concurrent.ConcurrentHashMap;

public class JobLockCacheDataImpl implements JobLockCacheData {
    private ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, JobLockHolder> jobLocksByIdentifier = new ConcurrentHashMap<>();

    @Override
    public ConcurrentHashMap<String, JobLockHolder> getJobLocksByIdentifier() {
        return jobLocksByIdentifier;
    }

    @Override
    public void setJobLocksByIdentifier(ConcurrentHashMap<String, JobLockHolder> jobLocksByIdentifier) {
        this.jobLocksByIdentifier = jobLocksByIdentifier;
    }

    @Override
    public ConcurrentHashMap<String, JobLockHolder> getJobLocksByLockName() {
        return jobLocksByLockName;
    }

    @Override
    public void setJobLocksByLockName(ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName) {
        this.jobLocksByLockName = jobLocksByLockName;
    }
}
