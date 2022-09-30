package org.ikasan.job.orchestration.model.context;

import org.ikasan.spec.scheduled.context.model.AbstractJobLockHolder;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobLockHolderImpl extends AbstractJobLockHolder implements JobLockHolder {
    private String lockName;
    private int lockCount = 1;
    private final Set<String> lockHolders = new HashSet<>();
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
    public int getLockCount() {
        return lockCount;
    }

    @Override
    public void setLockCount(int lockCount) {
        this.lockCount = lockCount;
    }

    @Override
    public Map<String, List<SchedulerJob>> getSchedulerJobs() {
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
}
