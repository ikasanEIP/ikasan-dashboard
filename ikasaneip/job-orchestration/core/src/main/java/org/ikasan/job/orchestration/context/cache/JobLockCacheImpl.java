package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.core.listener.JobLockCacheEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class JobLockCacheImpl implements JobLockCache, JobLockCacheEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheImpl.class);
    public static final String CONTEXT_ID = ":context-id:";

    private List<JobLockCacheEventListener> jobLockCacheEventListeners;
    private final static JobLockCacheImpl INSTANCE = new JobLockCacheImpl();

    private final JobLockCacheData jobLockCacheData;

    private JobLockCacheService jobLockCacheService;

    private JobLockCacheEventBroadcaster<JobLockCacheEvent> jobLockCacheEventBroadcaster;

    private ExecutorService executor;

    private JobLockCacheImpl() {
        this.jobLockCacheData = new JobLockCacheDataImpl();
        this.jobLockCacheEventListeners = new LinkedList<>();
        this.addJobLockCacheEventListener(this);
        // todo make pool size configurable
        this.executor = Executors.newFixedThreadPool(5);
    }

    private JobLockCacheRecord jobLockCacheRecord = null;

    public static JobLockCacheImpl instance() {
        return INSTANCE;
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
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName().get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new JobLockHolderImpl();
                jobLockHolder.setLockName(jobLock.getName());
                jobLockHolder.setLockCount(jobLock.getLockCount());
                for (Map.Entry<String, List<SchedulerJob>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                }
            } else {
                for (Map.Entry<String, List<SchedulerJob>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                }
            }
            this.jobLockCacheData.getJobLocksByLockName().put(jobLock.getName(), jobLockHolder);

            List<SchedulerJob> jobs = this.jobLockCacheData.getJobLocksByLockName().get(jobLock.getName())
                .getSchedulerJobs()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            for (SchedulerJob schedulerJob : jobs) {
                this.jobLockCacheData.getJobLocksByIdentifier().put(schedulerJob.getIdentifier(), jobLock.getName());
            }

            saveJobLockCacheRecord();
            LOGGER.debug(String.format("Added job lock: %s", jobLock.getName()));
        }
    }

    @Override
    public boolean doesJobParticipateInLock(String jobIdentifier, String contextName) {
        AtomicBoolean participatesInLock = new AtomicBoolean(false);
        this.jobLockCacheData.getJobLocksByLockName().entrySet().forEach(entry -> {
            entry.getValue().getSchedulerJobs().values().forEach(jobList -> {
                jobList.forEach(job -> {
                    if(job.getIdentifier().equals(jobIdentifier)) {
                        participatesInLock.set(true);
                    }
                });
            });
        });

        return participatesInLock.get();
    }

    @Override
    public synchronized boolean lock(String jobIdentifier, String contextName) {
        boolean locked = false;
        LOGGER.debug(String.format("Locking jobIdentifier: %s contextName: %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null && !locked(jobIdentifier, contextName)) {
                jobLockHolder.addLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                saveJobLockCacheRecord();
                locked = true;

                JobLockCacheEvent event = new JobLockCacheEventImpl(jobLockHolder.getLockName(), jobIdentifier, contextName
                    , JobLockCacheEvent.EventType.LOCK_OBTAINED);
                this.executor.submit(() -> this.jobLockCacheEventListeners
                    .forEach(listener -> listener.onJobLockCacheEvent(event)));
            }
        }
        String message = locked ? "Successfully locked " : "Failed to lock ";
        LOGGER.debug(String.format("%s jobIdentifier: %s contextName %s", message, jobIdentifier, contextName));
        return locked;
    }

    @Override
    public synchronized boolean release(String jobIdentifier, String contextName) {
        boolean removed = false;
        LOGGER.debug(String.format("Releasing lock for jobIdentifier: %s  contextName %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                removed = jobLockHolder.removeLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                if (removed) {
                    saveJobLockCacheRecord();
                    JobLockCacheEvent event = new JobLockCacheEventImpl(jobLockHolder.getLockName(), jobIdentifier, contextName
                        , JobLockCacheEvent.EventType.LOCK_RELEASED);
                    this.executor.submit(() -> this.jobLockCacheEventListeners
                        .forEach(listener -> listener.onJobLockCacheEvent(event)));
                }
            }
        }
        String message = removed ? "Successfully released " : "Failed to release ";
        LOGGER.debug(String.format("%s jobIdentifier: %s  contextName %s", message, jobIdentifier, contextName));
        return removed;
    }

    @Override
    public synchronized boolean locked(String jobIdentifier, String contextName) {
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
        this.jobLockCacheData.getJobLocksByLockName().clear();
        this.jobLockCacheData.getJobLocksByIdentifier().clear();
    }

    @Override
    public synchronized boolean resetLock(String lockName) {
        LOGGER.debug(String.format("Clearing lock for lock name: %s", lockName));
        if (lockName != null && this.jobLockCacheData.getJobLocksByLockName().get(lockName) != null) {
            this.jobLockCacheData.getJobLocksByLockName().get(lockName).getLockHolders().clear();
            this.jobLockCacheData.getJobLocksByLockName().get(lockName).getSchedulerJobInitiationEventWaitQueue().clear();

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

    private JobLockHolder getJobLockHolderForJobIdentifier(String jobIdentifier) {
        JobLockHolder jlh = null;
        if (jobIdentifier != null &&  this.jobLockCacheData.getJobLocksByIdentifier().containsKey(jobIdentifier)
            && this.jobLockCacheData.getJobLocksByLockName().containsKey(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier))) {
            jlh = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
        }
        return jlh;
    }

    private boolean workingCountIsGreaterThanOrEqualToLockCount(JobLockHolder jlh) {
        return jlh.getLockHolders().size() >= jlh.getLockCount();
    }

    private void saveJobLockCacheRecord() {
        if(this.jobLockCacheRecord == null) {
            this.jobLockCacheRecord = new JobLockCacheRecordImpl();
        }
        this.jobLockCacheRecord.setJobLockCache(this.jobLockCacheData);
        this.jobLockCacheService.save(this.jobLockCacheRecord);
    }

    @Override
    public void addQueuedSchedulerJobInitiationEvent(String jobIdentifier, String contextName, SchedulerJobInitiationEvent event) {
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                ContextualisedSchedulerJobInitiationEvent contextualisedSchedulerJobInitiationEvent
                    = new ContextualisedSchedulerJobInitiationEventImpl();
                contextualisedSchedulerJobInitiationEvent.setContextName(contextName);
                contextualisedSchedulerJobInitiationEvent.setSchedulerJobInitiationEvent(event);
                jobLockHolder.getSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                saveJobLockCacheRecord();
                JobLockCacheEvent jobLockCacheEvent = new JobLockCacheEventImpl(jobLockHolder.getLockName(), event.getInternalEventDrivenJob().getIdentifier()
                    , event.getInternalEventDrivenJob().getChildContextName(), JobLockCacheEvent.EventType.JOB_ADDED_TO_JOB_LOCK_QUEUE);
                this.executor.submit(() -> this.jobLockCacheEventListeners
                    .forEach(listener -> listener.onJobLockCacheEvent(jobLockCacheEvent)));
            }
        }
    }

    @Override
    public ContextualisedSchedulerJobInitiationEvent pollSchedulerJobInitiationEventWaitQueue(String jobIdentifier, String contextName) {
        ContextualisedSchedulerJobInitiationEvent removed = null;
        LOGGER.debug(String.format("Releasing lock for jobIdentifier: %s  contextName %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                removed = jobLockHolder.getSchedulerJobInitiationEventWaitQueue().poll();
                if (removed != null) {
                    saveJobLockCacheRecord();
                    JobLockCacheEvent jobLockCacheEvent = new JobLockCacheEventImpl(jobLockHolder.getLockName(), removed.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getIdentifier()
                        , removed.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getChildContextName(), JobLockCacheEvent.EventType.JOB_REMOVED_FROM_JOB_LOCK_QUEUE);
                    this.executor.submit(() -> this.jobLockCacheEventListeners
                        .forEach(listener -> listener.onJobLockCacheEvent(jobLockCacheEvent)));
                }
            }
        }

        String message = removed != null ? "Successfully removed waiting initiation event "
            + removed : "No queued initiation events.";
        LOGGER.debug(String.format("%s jobIdentifier: %s  contextName %s", message, jobIdentifier, contextName));
        return removed;
    }

    @Override
    public void setJobLockCacheRecord(JobLockCacheRecord jobLockCacheRecord) {
        this.jobLockCacheRecord = jobLockCacheRecord;
        if(jobLockCacheRecord != null) {
            this.jobLockCacheData.setJobLocksByLockName(jobLockCacheRecord.getJobLockCache().getJobLocksByLockName());
            this.jobLockCacheData.setJobLocksByIdentifier(jobLockCacheRecord.getJobLockCache().getJobLocksByIdentifier());
        }
    }

    @Override
    public void removeJobsLocksForContext(Context context) {
        if(this.jobLockCacheData != null) {
            this.jobLockCacheData.getJobLocksByLockName().values().forEach(lockHolder -> {
                lockHolder.removeSchedulerJobsForContext(context);
            });

            ConcurrentHashMap<String, JobLockHolder> transientJobLocksByName = new ConcurrentHashMap<>();
            this.jobLockCacheData.getJobLocksByLockName().entrySet().forEach(entry -> {
                if(entry.getValue().getSchedulerJobs().size() > 0) {
                    transientJobLocksByName.put(entry.getKey(), entry.getValue());
                }
            });
            this.jobLockCacheData.setJobLocksByLockName(transientJobLocksByName);

            this.jobLockCacheData.getJobLocksByIdentifier().clear();
            this.jobLockCacheData.getJobLocksByLockName().entrySet().forEach(entry -> {
                List<SchedulerJob> jobs = this.jobLockCacheData.getJobLocksByLockName().get(entry.getKey())
                    .getSchedulerJobs()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

                for (SchedulerJob schedulerJob : jobs) {
                    this.jobLockCacheData.getJobLocksByIdentifier().put(schedulerJob.getIdentifier(), entry.getKey());
                }
            });

            this.saveJobLockCacheRecord();
        }
    }

    public JobLockCacheData getJobLockCacheData() {
        return jobLockCacheData;
    }

    @Override
    public void onJobLockCacheEvent(JobLockCacheEvent jobLockCacheEvent) {
        if(this.jobLockCacheEventBroadcaster != null) {
            this.jobLockCacheEventBroadcaster.broadcast(jobLockCacheEvent);
        }
    }

    @Override
    public void addJobLockCacheEventListener(JobLockCacheEventListener listener) {
        this.jobLockCacheEventListeners.add(listener);
    }

    /**
     * Set the event broadcaster on the cache.
     *
     * @param jobLockCacheEventBroadcaster
     */
    public void setJobLockCacheEventBroadcaster(JobLockCacheEventBroadcaster<JobLockCacheEvent> jobLockCacheEventBroadcaster) {
        this.jobLockCacheEventBroadcaster = jobLockCacheEventBroadcaster;
    }
}
