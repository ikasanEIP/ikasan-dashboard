package org.ikasan.scheduled.joblock.model;

import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;

import java.util.concurrent.ConcurrentHashMap;

public class SolrJobLockCacheDataImpl implements JobLockCacheData {
    private ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> jobLocksByIdentifier = new ConcurrentHashMap<>();

    @Override
    public ConcurrentHashMap<String, String> getJobLocksByIdentifier() {
        return jobLocksByIdentifier;
    }

    @Override
    public void setJobLocksByIdentifier(ConcurrentHashMap<String, String> jobLocksByIdentifier) {
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
