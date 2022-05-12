package org.ikasan.job.orchestration.context.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class JobLockCacheImpl implements JobLockCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheImpl.class);
    private static final String CONTEXT_ID = ":context-id:";

    @JsonProperty
    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName;
    @JsonProperty
    private final ConcurrentHashMap<String, JobLockHolder> jobLocksByIdentifier;
    @JsonIgnore
    private JobLockCacheService jobLockCacheService;
    @JsonIgnore
    private ConcurrentHashMap<String, List<SchedulerJobInitiationEvent>> queuedSchedulerJobInitiationEvents;

    private JobLockCacheImpl() {
        jobLocksByLockName = new ConcurrentHashMap<>();
        jobLocksByIdentifier = new ConcurrentHashMap<>();
        queuedSchedulerJobInitiationEvents = new ConcurrentHashMap<>();
    }

    private static final class JobLockMachineHolder {
        public final static JobLockCacheImpl INSTANCE = new JobLockCacheImpl();
    }

    public static JobLockCacheImpl instance() {
        return JobLockMachineHolder.INSTANCE;
    }

    @Override
    public synchronized void addLocks(List<JobLock> jobLocks) {
        if (jobLocks != null) {
            jobLocks.forEach(this::addLock);
        }
    }

    private synchronized void addLock(JobLock jobLock) {
        if (jobLock != null) {
            // we need jobLocksByLockName to create the global lock holder added later in jobLocksByIdentifier
            JobLockHolder jobLockHolder = jobLocksByLockName.get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new JobLockHolderImpl();
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

            saveJobLockCacheRecord();
            LOGGER.debug(String.format("Added job lock: %s", jobLock.getName()));
        }
    }

    @Override
    public synchronized boolean lock(String jobIdentifier, String contextId) {
        boolean locked = false;
        LOGGER.debug(String.format("Locking jobIdentifier: %s contextId: %s", jobIdentifier, contextId));
        if (jobIdentifier != null && contextId != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null && !locked(jobIdentifier)) {
                jobLockHolder.addLockHolder(jobIdentifier + CONTEXT_ID + contextId);
                saveJobLockCacheRecord();
                locked = true;
            }
        }
        String message = locked ? "Successfully locked " : "Failed to lock ";
        LOGGER.debug(String.format("%s jobIdentifier: %s contextId %s", message, jobIdentifier, contextId));
        return locked;
    }

    @Override
    public synchronized boolean release(String jobIdentifier, String contextId) {
        boolean removed = false;
        LOGGER.debug(String.format("Releasing lock for jobIdentifier: %s  contextId %s", jobIdentifier, contextId));
        if (jobIdentifier != null && contextId != null) {
            JobLockHolder jobLockHolder = jobLocksByIdentifier.get(jobIdentifier);
            if (jobLockHolder != null) {
                removed = jobLockHolder.removeLockHolder(jobIdentifier + CONTEXT_ID + contextId);
                if (removed) {
                    saveJobLockCacheRecord();
                }
            }
        }
        String message = removed ? "Successfully released " : "Failed to release ";
        LOGGER.debug(String.format("%s jobIdentifier: %s  contextId %s", message, jobIdentifier, contextId));
        return removed;
    }

    @Override
    public synchronized boolean locked(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && workingCountIsGreaterThanOrEqualToLockCount(jlh);
    }

    @Override
    public synchronized boolean hasLock(String jobIdentifier, String contextId) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh != null && jlh.getLockHolders().contains(jobIdentifier + CONTEXT_ID + contextId);
    }

    @Override
    public synchronized void reset() {
        LOGGER.debug("Clearing all locks");
        jobLocksByLockName.clear();
        jobLocksByIdentifier.clear();
    }

    @Override
    public synchronized boolean resetLock(String lockName) {
        LOGGER.debug(String.format("Clearing lock for lock name: %s", lockName));
        if (lockName != null && jobLocksByLockName.get(lockName) != null) {
            jobLocksByLockName.get(lockName).getLockHolders().clear();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void setJobLockCacheService(JobLockCacheService jobLockCacheService) {
        // only set it if not already set
        if (this.jobLockCacheService == null) {
            this.jobLockCacheService = jobLockCacheService;
        }
    }

    @Override
    public synchronized List<SchedulerJob> getJobsForIdentifier(String jobIdentifier) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);
        return jlh == null ? Collections.emptyList() : jlh.getSchedulerJobs();
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

    private void saveJobLockCacheRecord() {
        JobLockCacheRecord record = new JobLockCacheRecordImpl();
        record.setJobLockCache(this);
        jobLockCacheService.save(record);
    }

    @Override
    public void addQueuedSchedulerJobInitiationEvent(String jobIdentifier, SchedulerJobInitiationEvent event) {
        if(!this.queuedSchedulerJobInitiationEvents.containsKey(jobIdentifier)) {
            List<SchedulerJobInitiationEvent> events = new ArrayList<>();
            events.add(event);
            this.queuedSchedulerJobInitiationEvents.put(jobIdentifier, events);
        }
        else {
            if(!this.queuedSchedulerJobInitiationEvents.get(jobIdentifier).contains(event)) {
                this.queuedSchedulerJobInitiationEvents.get(jobIdentifier).add(event);
            }
        }
    }

    @Override
    public SchedulerJobInitiationEvent getNextQueuedSchedulerJobInitiationEvent(String jobIdentifier) {
        if(this.queuedSchedulerJobInitiationEvents.containsKey(jobIdentifier)) {
            if(!this.queuedSchedulerJobInitiationEvents.get(jobIdentifier).isEmpty()) {
                return this.queuedSchedulerJobInitiationEvents.get(jobIdentifier).remove(0);
            }
            else {
                return null;
            }
        }

        return null;
    }
}
