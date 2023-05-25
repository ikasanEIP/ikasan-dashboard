package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.core.listener.JobLockCacheEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class JobLockCacheImpl implements JobLockCache, JobLockCacheEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheImpl.class);
    public static final String CONTEXT_ID = ":context-id:";

    private List<JobLockCacheEventListener> jobLockCacheEventListeners;
    private final static JobLockCacheImpl INSTANCE = new JobLockCacheImpl();

    private final JobLockCacheData jobLockCacheData;

    private JobLockCacheService jobLockCacheService;

    private JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster;

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
        if(this.jobLockCacheData.getExclusiveLockHolder() == null) {
            this.jobLockCacheData.setExclusiveLockHolder(new JobLockHolderImpl());
        }
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
                jobLockHolder.setExclusiveJobLock(jobLock.isExclusiveJobLock());
                for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                }
            } else {
                for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.setLockCount(jobLock.getLockCount());
                    jobLockHolder.setExclusiveJobLock(jobLock.isExclusiveJobLock());
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
            if(jobLockHolder != null && jobLockHolder.isExclusiveJobLock()) {
                if(this.canTakeExclusiveLock() && jobLockCacheData.getExclusiveLockHolder().getLockHolders().size() == 0) {
                    LOGGER.info(String.format("Taking exclusive lock! jobIdentifier: %s contextName: %s"
                        , jobIdentifier, contextName));
                    jobLockCacheData.getExclusiveLockHolder().addLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                    saveJobLockCacheRecord();
                    locked = true;

                    this.publishJobLockCacheEvent(jobLockCacheData.getExclusiveLockHolder().getLockName(), jobIdentifier, contextName
                        , JobLockCacheEvent.EventType.LOCK_OBTAINED);
                }
            }
            else if (jobLockHolder != null && !locked(jobIdentifier, contextName)) {
                jobLockHolder.addLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                saveJobLockCacheRecord();
                locked = true;

                this.publishJobLockCacheEvent(jobLockHolder.getLockName(), jobIdentifier, contextName
                    , JobLockCacheEvent.EventType.LOCK_OBTAINED);
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
        if (jobIdentifier != null && contextName != null
            && this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier) != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                if(jobLockHolder.isExclusiveJobLock()) {
                    removed = this.jobLockCacheData.getExclusiveLockHolder().removeLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                }
                else {
                    removed = jobLockHolder.removeLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                }

                if (removed) {
                    saveJobLockCacheRecord();
                    this.publishJobLockCacheEvent(jobLockHolder.getLockName(), jobIdentifier, contextName
                        , JobLockCacheEvent.EventType.LOCK_RELEASED);
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

        if(jlh != null && jlh.isExclusiveJobLock()) {
            return !canTakeExclusiveLock();
        }
        else {
            return jlh != null && (workingCountIsGreaterThanToLockCount(jlh, jobIdentifier)
                || !this.jobLockCacheData.getExclusiveLockHolder().getLockHolders().isEmpty());
        }
    }

    @Override
    public synchronized boolean hasLock(String jobIdentifier, String contextId) {
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier);

        if(jlh != null && jlh.isExclusiveJobLock()) {
            return this.jobLockCacheData.getExclusiveLockHolder().getLockHolders()
                .contains(jobIdentifier + CONTEXT_ID + contextId);
        }
        else {
            return jlh != null && jlh.getLockHolders().contains(jobIdentifier + CONTEXT_ID + contextId);
        }
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
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName().get(lockName);
            if(jobLockHolder.isExclusiveJobLock()) {
                this.jobLockCacheData.getExclusiveLockHolder().getLockHolders().clear();
                this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().clear();
            }
            else {
                jobLockHolder.getLockHolders().clear();
                jobLockHolder.getSchedulerJobInitiationEventWaitQueue().clear();
            }

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
    public void addQueuedSchedulerJobInitiationEvent(String jobIdentifier, String contextName, SchedulerJobInitiationEvent event) {
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                ContextualisedSchedulerJobInitiationEvent contextualisedSchedulerJobInitiationEvent
                    = new ContextualisedSchedulerJobInitiationEventImpl();
                contextualisedSchedulerJobInitiationEvent.setContextName(contextName);
                contextualisedSchedulerJobInitiationEvent.setSchedulerJobInitiationEvent(event);

                if(jobLockHolder.isExclusiveJobLock()) {
                    this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                }
                else {
                    jobLockHolder.getSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                }

                saveJobLockCacheRecord();
                this.publishJobLockCacheEvent(jobLockHolder.getLockName(), event.getInternalEventDrivenJob().getIdentifier()
                    , event.getInternalEventDrivenJob().getChildContextName(), JobLockCacheEvent.EventType.JOB_ADDED_TO_JOB_LOCK_QUEUE);
            }
        }
    }

    @Override
    public List<ContextualisedSchedulerJobInitiationEvent> pollSchedulerJobInitiationEventWaitQueue(String jobIdentifier, String contextName) {
        List<ContextualisedSchedulerJobInitiationEvent> removed = null;
        LOGGER.debug(String.format("Releasing lock for jobIdentifier: %s  contextName %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null) {
            JobLockHolder jobLockHolder = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {

                if(jobLockHolder.isExclusiveJobLock()) {
                    if (!this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().isEmpty()) {
                        removed = List.of(this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().poll());
                    }
                    else {
                        removed = new ArrayList<>();
                        for(JobLockHolder jlh: this.jobLockCacheData.getJobLocksByLockName().values()) {
                            if(!jlh.isExclusiveJobLock() && !jlh.getSchedulerJobInitiationEventWaitQueue().isEmpty()) {
                                removed.add(jlh.getSchedulerJobInitiationEventWaitQueue().poll());
                            }
                        }
                    }
                }
                else {
                    if (jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size() > 0) {
                        removed = List.of(jobLockHolder.getSchedulerJobInitiationEventWaitQueue().poll());
                    }
                    else if (this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().size() > 0) {
                        removed = List.of(this.jobLockCacheData.getExclusiveLockSchedulerJobInitiationEventWaitQueue().poll());
                    }
                }

                if (removed != null) {
                    saveJobLockCacheRecord();

                    removed.forEach(contextualisedSchedulerJobInitiationEvent -> {
                        this.publishJobLockCacheEvent(jobLockHolder.getLockName(), contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getIdentifier()
                            , contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getChildContextName(), JobLockCacheEvent.EventType.JOB_REMOVED_FROM_JOB_LOCK_QUEUE);
                    });
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
    public void setJobLockCacheEventBroadcaster(JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster) {
        this.jobLockCacheEventBroadcaster = jobLockCacheEventBroadcaster;
    }

    /**
     * Helper method to publish job lock cache events to listeners.
     *
     * @param jobLockName
     * @param jobIdentifier
     * @param contextName
     */
    private void publishJobLockCacheEvent(String jobLockName, String jobIdentifier, String contextName, JobLockCacheEvent.EventType eventType) {
        JobLockCacheEvent event = new JobLockCacheEventImpl(jobLockName, jobIdentifier, contextName
            , eventType);
        this.executor.submit(() -> this.jobLockCacheEventListeners
            .forEach(listener -> listener.onJobLockCacheEvent(event)));
    }

    /**
     * Helper method to determine if an exclusive lock can be taken.
     *
     * @return
     */
    private synchronized boolean canTakeExclusiveLock() {
        AtomicBoolean canTakeExclusiveLock = new AtomicBoolean(true);

        AtomicBoolean areThereAnyExclusiveLocks = new AtomicBoolean(false);
        this.jobLockCacheData.getJobLocksByLockName().values().forEach(jobLockHolder -> {
            if(jobLockHolder.isExclusiveJobLock()) {
                areThereAnyExclusiveLocks.set(true);
            }
        });

        if(!areThereAnyExclusiveLocks.get()) {
            return canTakeExclusiveLock.get();
        }

        this.jobLockCacheData.getJobLocksByLockName().entrySet().forEach(entry -> {
            if(entry.getValue().getLockHolders().size() > 0) {
                canTakeExclusiveLock.set(false);
            }
        });

        if(this.jobLockCacheData.getExclusiveLockHolder().getLockHolders().size() > 0) {
            canTakeExclusiveLock.set(false);
        }

        return canTakeExclusiveLock.get();
    }

    /**
     * Method to determine if there are any non exclusive lock holders.
     *
     * @return
     */
    private synchronized boolean areNonExclusiveLockHolders() {
        AtomicBoolean areNonExclusiveLockHolders = new AtomicBoolean(false);

        this.jobLockCacheData.getJobLocksByLockName().entrySet().forEach(entry -> {
            if(entry.getValue().getLockHolders().size() > 0 || entry.getValue().getSchedulerJobInitiationEventWaitQueue().size() > 0) {
                areNonExclusiveLockHolders.set(true);
            }
        });

        return areNonExclusiveLockHolders.get();
    }

    /**
     * Get the job lock holder based on the job identifier.
     *
     * @param jobIdentifier
     * @return
     */
    private JobLockHolder getJobLockHolderForJobIdentifier(String jobIdentifier) {
        JobLockHolder jlh = null;
        if (jobIdentifier != null &&  this.jobLockCacheData.getJobLocksByIdentifier().containsKey(jobIdentifier)
            && this.jobLockCacheData.getJobLocksByLockName().containsKey(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier))) {
            jlh = this.jobLockCacheData.getJobLocksByLockName()
                .get(this.jobLockCacheData.getJobLocksByIdentifier().get(jobIdentifier));
        }
        return jlh;
    }

    /**
     * Determine if the lock count exceeds the number of locks that can be taken.
     *
     * @param jlh
     * @return
     */
    private boolean workingCountIsGreaterThanToLockCount(JobLockHolder jlh, String jobIdentifier) {
        Map<String, SchedulerJobLockParticipant> schedulerJobLockParticipantMap
            = new HashMap<>();

        AtomicReference<SchedulerJobLockParticipant> jobLockParticipant = new AtomicReference<>();

        jlh.getSchedulerJobs().entrySet().forEach(entry -> {
            entry.getValue().forEach(job -> {
                if(!schedulerJobLockParticipantMap.containsKey(job.getIdentifier())) {
                    schedulerJobLockParticipantMap.put(job.getIdentifier(), job);
                }

                if(jobLockParticipant.get() == null && job.getIdentifier().equals(jobIdentifier)) {
                    jobLockParticipant.set(job);
                }
            });
        });

        AtomicLong lockParticipantCount = new AtomicLong();
        schedulerJobLockParticipantMap.values().forEach(schedulerJobLockParticipant -> {
            jlh.getLockHolders().forEach(holder -> {
                if(holder.contains(schedulerJobLockParticipant.getIdentifier())) {
                    lockParticipantCount.addAndGet(schedulerJobLockParticipant.getLockCount());
                }
            });
        });

        if(jobLockParticipant != null) {
            lockParticipantCount.addAndGet(jobLockParticipant.get().getLockCount());
        }

        return lockParticipantCount.get() > jlh.getLockCount();
    }

    /**
     * Helper method to save the underlying cache record to the persistent store.
     */
    private void saveJobLockCacheRecord() {
        if(this.jobLockCacheRecord == null) {
            this.jobLockCacheRecord = new JobLockCacheRecordImpl();
        }
        this.jobLockCacheRecord.setJobLockCache(this.jobLockCacheData);
        this.jobLockCacheService.save(this.jobLockCacheRecord);
    }
}
