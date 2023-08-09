package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.context.model.AbstractJobLockHolder;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.*;

public class SolrJobLockHolderImpl extends AbstractJobLockHolder implements JobLockHolder {
    private String lockName;
    private long lockCount = 1;
    private final Set<String> lockHolders = new HashSet<>();

    private boolean exclusiveJobLock = false;
    private Queue<ContextualisedSchedulerJobInitiationEvent> contextualisedSchedulerJobInitiationEvents
        = new LinkedList<>();

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

    public Map<String, List<SchedulerJobLockParticipant>> getSchedulerJobs() {
        return schedulerJobs;
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

    @Override
    public boolean isExclusiveJobLock() {
        return exclusiveJobLock;
    }

    @Override
    public void setExclusiveJobLock(boolean exclusiveJobLock) {
        this.exclusiveJobLock = exclusiveJobLock;
    }

    @Override
    public Queue<ContextualisedSchedulerJobInitiationEvent> getSchedulerJobInitiationEventWaitQueue() {
        return this.contextualisedSchedulerJobInitiationEvents;
    }

    @Override
    public void setSchedulerJobInitiationEventWaitQueue(Queue<ContextualisedSchedulerJobInitiationEvent> contextualisedSchedulerJobInitiationEventQueue) {
        this.contextualisedSchedulerJobInitiationEvents = contextualisedSchedulerJobInitiationEventQueue;
    }
}
