package org.ikasan.scheduled.job.model;

import java.util.*;

import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class SolrJobLockHolderImpl implements JobLockHolder {
    private String lockName;
    private long lockCount = 1;
    private final Map<String, List<SchedulerJob>> schedulerJobs = new HashMap<>();
    private final Set<String> lockHolders = new HashSet<>();
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

    public Map<String, List<SchedulerJob>> getSchedulerJobs() {
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
    public void addSchedulerJobs(String contextName, List<SchedulerJob> jobs) {
        if(!this.schedulerJobs.containsKey(contextName)) {
            this.schedulerJobs.put(contextName, new ArrayList<>());
        }

        this.schedulerJobs.get(contextName).addAll(jobs);
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
