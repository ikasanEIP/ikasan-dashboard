package org.ikasan.relational.persistence.scheduled.joblock.model;

import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hibernate/PostgreSQL implementation of JobLockCacheData.
 *
 * This class holds the job lock cache data including lock holders by name,
 * lock identifiers, exclusive lock holder, and wait queues.
 */
public class HibernateJobLockCacheData implements JobLockCacheData {

    private ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> jobLocksByIdentifier = new ConcurrentHashMap<>();
    private Queue<ContextualisedSchedulerJobInitiationEvent> exclusiveLockSchedulerJobInitiationEventWaitQueue = new LinkedList<>();
    private JobLockHolder exclusiveLockHolder = new JobLockHolderImpl();

    /**
     * Default constructor
     */
    public HibernateJobLockCacheData() {
    }

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

    @Override
    public Queue<ContextualisedSchedulerJobInitiationEvent> getExclusiveLockSchedulerJobInitiationEventWaitQueue() {
        return exclusiveLockSchedulerJobInitiationEventWaitQueue;
    }

    @Override
    public void setExclusiveLockSchedulerJobInitiationEventWaitQueue(Queue<ContextualisedSchedulerJobInitiationEvent> exclusiveLockSchedulerJobInitiationEventWaitQueue) {
        this.exclusiveLockSchedulerJobInitiationEventWaitQueue = exclusiveLockSchedulerJobInitiationEventWaitQueue;
    }

    @Override
    public JobLockHolder getExclusiveLockHolder() {
        return exclusiveLockHolder;
    }

    @Override
    public void setExclusiveLockHolder(JobLockHolder exclusiveLockHolder) {
        if (exclusiveLockHolder != null) {
            this.exclusiveLockHolder = exclusiveLockHolder;
        }
    }
}
