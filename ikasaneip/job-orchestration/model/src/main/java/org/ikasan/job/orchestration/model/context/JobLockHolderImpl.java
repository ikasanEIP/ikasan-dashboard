package org.ikasan.job.orchestration.model.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class JobLockHolderImpl implements JobLockHolder {
    private String lockName;
    private long lockCount = 1;
    private final Map<String, List<SchedulerJob>> schedulerJobs = new ConcurrentHashMap<>();
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
    public long getLockCount() {
        return lockCount;
    }

    @Override
    public void setLockCount(long lockCount) {
        this.lockCount = lockCount;
    }

    @Override
    public Map<String, List<SchedulerJob>> getSchedulerJobs() {
        return schedulerJobs;
    }

    @Override
    public void addSchedulerJobs(String contextName, List<SchedulerJob> jobs) {
        if(!this.schedulerJobs.containsKey(contextName)) {
            this.schedulerJobs.put(contextName, new ArrayList<>());
        }
        this.schedulerJobs.get(contextName).addAll(jobs);
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
    public ContextualisedSchedulerJobInitiationEvent pollSchedulerJobInitiationEventWaitQueue() {
        return this.queuedSchedulerJobInitiationEvents.poll();
    }

    @Override
    public void addQueuedSchedulerJobInitiationEvent(ContextualisedSchedulerJobInitiationEvent event) {
        this.queuedSchedulerJobInitiationEvents.offer(event);
    }
}
