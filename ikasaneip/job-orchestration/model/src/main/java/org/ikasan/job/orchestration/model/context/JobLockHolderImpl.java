package org.ikasan.job.orchestration.model.context;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.AbstractJobLockHolder;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobLockHolderImpl extends AbstractJobLockHolder implements JobLockHolder {
    private String lockName;
    private long lockCount = 1;
    private final Set<String> lockHolders = new HashSet<>();
    private boolean exclusiveJobLock = false;
    private Queue<ContextualisedSchedulerJobInitiationEvent> queuedSchedulerJobInitiationEvents = new LinkedList<>();

    @Override
    public String getLockName() {
        return lockName;
    }

    @Override
    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    @Override
    public long getLockCount() {
        return lockCount;
    }

    @Override
    public void setLockCount(long lockCount) {
        this.lockCount = lockCount;
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
    public Map<String, List<SchedulerJobLockParticipant>> getSchedulerJobs() {
        return schedulerJobs;
    }

    @Override
    public Set<String> getLockHolders() {
        return lockHolders;
    }

    @Override
    public void addLockHolder(String jobIdentifier) {
        lockHolders.add(jobIdentifier);
    }

    @Override
    public boolean removeLockHolder(String jobIdentifier) {
        return lockHolders.remove(jobIdentifier);
    }

    @Override
    public Queue<ContextualisedSchedulerJobInitiationEvent> getSchedulerJobInitiationEventWaitQueue() {
        return this.queuedSchedulerJobInitiationEvents;
    }

    @Override
    public void setSchedulerJobInitiationEventWaitQueue(Queue<ContextualisedSchedulerJobInitiationEvent> contextualisedSchedulerJobInitiationEventQueue) {
        this.queuedSchedulerJobInitiationEvents = contextualisedSchedulerJobInitiationEventQueue;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
