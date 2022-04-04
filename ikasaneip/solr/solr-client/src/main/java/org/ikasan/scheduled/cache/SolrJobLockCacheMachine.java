package org.ikasan.scheduled.cache;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.scheduled.job.model.SolrJobLockHolderImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SolrJobLockCacheMachine implements JobLockCache {

    @JsonProperty
    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName;
    @JsonProperty
    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByIdentifier;

    private SolrJobLockCacheMachine() {
        jobLocksByLockName = new ConcurrentHashMap<>();
        jobLocksByIdentifier = new ConcurrentHashMap<>();
    }

    private static final class JobLockMachineHolder {
        public final static SolrJobLockCacheMachine INSTANCE = new SolrJobLockCacheMachine();
    }

    public static SolrJobLockCacheMachine instance() {
        return JobLockMachineHolder.INSTANCE;
    }

    public synchronized void addLock(JobLock jobLock) {
        if (jobLock != null) {
            // we need jobLocksByLockName to create the global lock holder added later in jobLocksByIdentifier
            JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new SolrJobLockHolderImpl();
                jobLockHolder.setLockName(jobLock.getName());
                jobLockHolder.setLockCount(jobLock.getLockCount());
                jobLockHolder.addSchedulerJobs(jobLock.getJobs());
            } else {
                jobLockHolder.addSchedulerJobs(jobLock.getJobs());
            }
            jobLocksByLockName.put(jobLock.getName(), jobLockHolder);

            List<SchedulerJob> jobs = jobLocksByLockName.get(jobLock.getName()).getSchedulerJobs();
            for (SchedulerJob schedulerJob : jobs) {
                jobLocksByIdentifier.put(schedulerJob.getIdentifier(), jobLockHolder);
            }
        }
    }

    public synchronized void addLocks(List<JobLock> jobLocks) {
        if (jobLocks != null) {
            jobLocks.forEach(this::addLock);
        }
    }

    public synchronized boolean lock(String jobIdentifier) {
        if (jobIdentifier != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null && !locked(jobIdentifier)) {
                jobLockHolder.addLockHolder(jobIdentifier);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean release(String jobIdentifier) {
        if (jobIdentifier != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null) {
                return jobLockHolder.removeLockHolder(jobIdentifier);
            }
        }
        return false;
    }

    public synchronized boolean locked(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && workingCountIsGreaterThanOrEqualToLockCount(jlh);
    }

    public synchronized boolean hasLock(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && jlh.getLockHolders().contains(jobIdentifier);
    }

    public synchronized boolean existsByIdentifier(String jobIdentifier) {
        return jobIdentifier != null && jobLocksByIdentifier.get(jobIdentifier) != null;
    }

    public synchronized boolean existsByJobLockName(String jobLockName) {
        return jobLockName != null && jobLocksByLockName.get(jobLockName) != null;
    }

    public synchronized List<SchedulerJob> getJobsForIdentifier(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh == null ? Collections.emptyList() : jlh.getSchedulerJobs();
    }

    public synchronized void reset() {
        jobLocksByLockName.clear();
        jobLocksByIdentifier.clear();
    }

    public synchronized boolean resetLock(String lockName) {
        if (existsByJobLockName(lockName)) {
            jobLocksByLockName.get(lockName).getLockHolders().clear();
            return true;
        }
        return false;
    }

    @Override
    public void setJobLockCacheService(JobLockCacheService jobLockCacheService) {
        // do nothing in solr implementation
    }

    public ConcurrentHashMap<String, JobLockHolder> getJobLocksByLockName() {
        return jobLocksByLockName;
    }

    public ConcurrentHashMap<String, JobLockHolder> getJobLocksByIdentifier() {
        return jobLocksByIdentifier;
    }

    private JobLockHolder getJobLockHolderForJobIdentifier(String jobIdentifier) {
        JobLockHolder jlh = null;
        if (jobIdentifier != null) {
            jlh = jobLocksByIdentifier.get(jobIdentifier);
        }
        return jlh;
    }

    private boolean workingCountIsGreaterThanOrEqualToLockCount(JobLockHolder jlh) {
        return jlh.getLockHolders().size() >= jlh.getLockCount();
    }
}
