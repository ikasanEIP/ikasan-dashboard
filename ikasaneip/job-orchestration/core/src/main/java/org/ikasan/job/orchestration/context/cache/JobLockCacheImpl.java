package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.context.util.JobThreadFactory;
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
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class JobLockCacheImpl implements JobLockCache, JobLockCacheEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockCacheImpl.class);
    public static final String CONTEXT_ID = ":context-id:";

    private List<JobLockCacheEventListener> jobLockCacheEventListeners;
    private static JobLockCacheImpl INSTANCE = new JobLockCacheImpl();

    private final ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap;

    private JobLockCacheService jobLockCacheService;

    private JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster;

    private ExecutorService executor;

    private final ConcurrentHashMap<String, JobLockCacheRecord> jobLockCacheRecordMap;

    /**
     * Constructor for JobLockCacheImpl class.
     * Initializes the jobLockCacheData, jobLockCacheEventListeners, and executor.
     * Also adds itself as an event listener.
     * Uses a fixed thread pool with a size of 5 for background tasks.
     * It is recommended to make the pool size configurable.
     */
    private JobLockCacheImpl() {
        this.jobLockCacheEventListeners = new LinkedList<>();
        this.jobLockCacheDataMap = new ConcurrentHashMap<>();
        this.jobLockCacheRecordMap = new ConcurrentHashMap<>();
        this.addJobLockCacheEventListener(this);
        // todo make pool size configurable
        this.executor = Executors.newFixedThreadPool(5, new JobThreadFactory("JobLockCacheImpl"));
    }

    /**
     * Retrieves the singleton instance of JobLockCacheImpl class.
     *
     * @return the singleton instance of JobLockCacheImpl
     */
    public static JobLockCacheImpl instance() {
        if(INSTANCE == null) INSTANCE = new JobLockCacheImpl();
        return INSTANCE;
    }

    @Override
    public synchronized void addLocks(List<JobLock> jobLocks, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        LOGGER.debug("Add jobs locks!");
        if(this.getJobLockCacheData(environment).getExclusiveLockHolder() == null) {
            this.getJobLockCacheData(environment).setExclusiveLockHolder(new JobLockHolderImpl());
        }
        if (jobLocks != null) {
            jobLocks.forEach(jobLock -> this.addLock(jobLock, environment));
        }
    }

    /**
     * Adds a JobLock to the JobLockCache. This method synchronizes access to ensure thread safety.
     *
     * @param jobLock the JobLock to be added
     */
    private synchronized void addLock(JobLock jobLock, String environment) {
        if (jobLock != null) {
            LOGGER.debug(String.format("Adding job lock: [%s]", jobLock.getName()));
            if(jobLock.getJobs() != null) {
                jobLock.getJobs().entrySet().forEach(entry -> {
                    entry.getValue().forEach(job -> {
                        LOGGER.debug(String.format("Adding job[%s], in context[%s] to lock: [%s]"
                            , job.getJobName(), entry.getKey(), jobLock.getName()));
                    });
                });
            }
            // we need jobLocksByLockName to create the global lock holder added later in jobLocksByIdentifier
            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName().get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new JobLockHolderImpl();
                jobLockHolder.setLockName(jobLock.getName());
                jobLockHolder.setLockCount(jobLock.getLockCount());
                jobLockHolder.setExclusiveJobLock(jobLock.isExclusiveJobLock());
                if(jobLock.getJobs() != null) {
                    for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                        jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                if(jobLock.getJobs() != null) {
                    for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                        jobLockHolder.setLockCount(jobLock.getLockCount());
                        jobLockHolder.setExclusiveJobLock(jobLock.isExclusiveJobLock());
                        jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                    }
                }
            }
            this.getJobLockCacheData(environment).getJobLocksByLockName().put(jobLock.getName(), jobLockHolder);

            List<SchedulerJob> jobs = this.getJobLockCacheData(environment).getJobLocksByLockName().get(jobLock.getName())
                .getSchedulerJobs()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            for (SchedulerJob schedulerJob : jobs) {
                this.getJobLockCacheData(environment).getJobLocksByIdentifier().put(schedulerJob.getIdentifier(), jobLock.getName());
            }

            saveJobLockCacheRecord(environment);
            LOGGER.debug(String.format("Added job lock: %s", jobLock.getName()));
        }
    }

    @Override
    public boolean doesJobParticipateInLock(String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        AtomicBoolean participatesInLock = new AtomicBoolean(false);
        this.getJobLockCacheData(environment).getJobLocksByLockName().entrySet().forEach(entry -> {
            entry.getValue().getSchedulerJobs().values().forEach(jobList -> {
                jobList.forEach(job -> {
                    if(job.getIdentifier().equals(jobIdentifier)) {
                        participatesInLock.set(true);
                    }
                });
            });
        });

        LOGGER.debug(String.format("Determining if jobIdentifier: [%s] contextName: [%s] participates in lock. Result[%s]"
            , jobIdentifier, contextName, participatesInLock.get()));
        return participatesInLock.get();
    }

    @Override
    public synchronized boolean lock(String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        boolean locked = false;
        LOGGER.debug(String.format("Locking jobIdentifier: %s contextName: %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null) {
            if(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier) == null) {
                LOGGER.info("Cannot get lock holder for job[{}] in context[{}]!", jobIdentifier, contextName);
                return false;
            }

            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName()
                .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier));
            if(jobLockHolder != null && jobLockHolder.isExclusiveJobLock()) {
                if(this.canTakeExclusiveLock(environment) && this.getJobLockCacheData(environment)
                    .getExclusiveLockHolder().getLockHolders().size() == 0) {
                    LOGGER.info(String.format("Taking exclusive lock! jobIdentifier: %s contextName: %s"
                        , jobIdentifier, contextName));
                    this.getJobLockCacheData(environment).getExclusiveLockHolder().addLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                    saveJobLockCacheRecord(environment);
                    locked = true;

                    this.publishJobLockCacheEvent(this.getJobLockCacheData(environment).getExclusiveLockHolder().getLockName(), jobIdentifier, contextName, environment
                        , JobLockCacheEvent.EventType.LOCK_OBTAINED);
                }
            }
            else if (jobLockHolder != null && !locked(jobIdentifier, contextName, environment)) {
                jobLockHolder.addLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                saveJobLockCacheRecord(environment);
                locked = true;

                this.publishJobLockCacheEvent(jobLockHolder.getLockName(), jobIdentifier, contextName, environment
                    , JobLockCacheEvent.EventType.LOCK_OBTAINED);
            }
        }
        String message = locked ? "Successfully locked " : "Failed to lock ";
        LOGGER.debug(String.format("%s jobIdentifier: %s contextName %s", message, jobIdentifier, contextName));
        return locked;
    }

    @Override
    public synchronized boolean release(String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        AtomicBoolean removed = new AtomicBoolean(false);
        LOGGER.debug(String.format("Releasing lock for jobIdentifier: %s  contextName %s", jobIdentifier, contextName));
        if (jobIdentifier != null && contextName != null
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier) != null) {
            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName()
                .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                if(jobLockHolder.isExclusiveJobLock()) {
                    removed.set(this.getJobLockCacheData(environment).getExclusiveLockHolder().removeLockHolder(jobIdentifier + CONTEXT_ID + contextName));
                }
                else {
                    removed.set(jobLockHolder.removeLockHolder(jobIdentifier + CONTEXT_ID + contextName));
                }

                if (removed.get()) {
                    saveJobLockCacheRecord(environment);
                    this.publishJobLockCacheEvent(jobLockHolder.getLockName(), jobIdentifier, contextName, environment
                        , JobLockCacheEvent.EventType.LOCK_RELEASED);
                }
            }
        }
        else if (jobIdentifier != null && contextName != null
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier) == null) {
            this.getJobLockCacheData(environment).getJobLocksByLockName().values().forEach(lh -> {
                if(lh.getLockHolders().contains(jobIdentifier + CONTEXT_ID + contextName)) {
                    lh.removeLockHolder(jobIdentifier + CONTEXT_ID + contextName);
                    removed.set(true);
                }
            });
        }
        String message = removed.get() ? "Successfully released " : "Failed to release ";
        LOGGER.debug(String.format("%s jobIdentifier: %s  contextName %s", message, jobIdentifier, contextName));
        return removed.get();
    }

    @Override
    public synchronized boolean locked(String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier, environment);

        if(jlh != null && jlh.isExclusiveJobLock()) {
            boolean locked = !canTakeExclusiveLock(environment);
            LOGGER.debug(String.format("Determining if job[%s], context[%s], exclusive lock is locked. Result[%s]"
                , jobIdentifier, contextName, locked));
            return locked;
        }
        else {
            boolean locked = jlh != null && (workingCountIsGreaterThanToLockCount(jlh, jobIdentifier, contextName)
                || !this.getJobLockCacheData(environment).getExclusiveLockHolder().getLockHolders().isEmpty());
            LOGGER.debug(String.format("Determining if job[%s], context[%s], non exclusive lock is locked. Result[%s]"
                , jobIdentifier, contextName, locked));
            return locked;
        }
    }

    @Override
    public synchronized boolean hasLock(String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        JobLockHolder jlh = getJobLockHolderForJobIdentifier(jobIdentifier, environment);

        if(jlh != null && jlh.isExclusiveJobLock()) {
            boolean hasLock = this.getJobLockCacheData(environment).getExclusiveLockHolder().getLockHolders()
                .contains(jobIdentifier + CONTEXT_ID + contextName);
            LOGGER.debug(String.format("Determining if job[%s], context[%s], exclusive lock currently holds lock. Result[%s]"
                , jobIdentifier, contextName, hasLock));
            return hasLock;
        }
        else {
            boolean hasLock =  jlh != null && jlh.getLockHolders().contains(jobIdentifier + CONTEXT_ID + contextName);
            LOGGER.debug(String.format("Determining if job[%s], context[%s], non exclusive lock currently holds lock. Result[%s]"
                , jobIdentifier, contextName, hasLock));
            return hasLock;
        }
    }

    @Override
    public synchronized void reset() {
        LOGGER.debug("Clearing all locks");
        this.jobLockCacheDataMap.keySet().forEach(environment -> this.reset(environment));
    }

    @Override
    public synchronized void reset(String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        LOGGER.debug("Clearing all locks");
        this.getJobLockCacheData(environment).getJobLocksByLockName().clear();
        this.getJobLockCacheData(environment).getJobLocksByIdentifier().clear();
        this.saveJobLockCacheRecord(environment);
    }

    @Override
    public synchronized boolean resetLock(String lockName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        LOGGER.debug(String.format("Clearing lock for lock name: %s", lockName));
        if (lockName != null && this.getJobLockCacheData(environment).getJobLocksByLockName().get(lockName) != null) {
            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName().get(lockName);
            if(jobLockHolder.isExclusiveJobLock()) {
                this.getJobLockCacheData(environment).getExclusiveLockHolder().getLockHolders().clear();
                this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().clear();
            }
            else {
                jobLockHolder.getLockHolders().clear();
                jobLockHolder.getSchedulerJobInitiationEventWaitQueue().clear();
            }

            this.saveJobLockCacheRecord(environment);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void setJobLockCacheService(JobLockCacheService jobLockCacheService) {
        // only set it if not already set
        if (this.jobLockCacheService == null) {
            LOGGER.debug("Setting job lock cache service");
            this.jobLockCacheService = jobLockCacheService;
        }
    }

    @Override
    public synchronized void addQueuedSchedulerJobInitiationEvent(String jobIdentifier, String contextName
        , SchedulerJobInitiationEvent event, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        if (jobIdentifier != null && contextName != null && this.getJobLockCacheData(environment).getJobLocksByIdentifier() != null
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier().containsKey(jobIdentifier)) {
            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName()
                .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {
                ContextualisedSchedulerJobInitiationEvent contextualisedSchedulerJobInitiationEvent
                    = new ContextualisedSchedulerJobInitiationEventImpl();
                contextualisedSchedulerJobInitiationEvent.setContextName(contextName);
                contextualisedSchedulerJobInitiationEvent.setSchedulerJobInitiationEvent(event);

                if(jobLockHolder.isExclusiveJobLock()) {
                    this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                }
                else {
                    jobLockHolder.getSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                }

                saveJobLockCacheRecord(environment);
                this.publishJobLockCacheEvent(jobLockHolder.getLockName(), event.getInternalEventDrivenJob().getIdentifier()
                    , event.getInternalEventDrivenJob().getChildContextName(), environment, JobLockCacheEvent.EventType.JOB_ADDED_TO_JOB_LOCK_QUEUE);
            }
        }
    }

    @Override
    public synchronized void removeQueuedSchedulerJob(SchedulerJobInstance schedulerJobInstance, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        if (schedulerJobInstance != null) {
            if(this.getJobLockCacheData(environment).getJobLocksByIdentifier().containsKey(schedulerJobInstance.getIdentifier())) {
                JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName()
                    .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(schedulerJobInstance.getIdentifier()));
                if (jobLockHolder != null) {
                    if(jobLockHolder.isExclusiveJobLock()) {
                        this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().removeIf(record
                            -> {
                            if (record.getSchedulerJobInitiationEvent().getContextInstanceId().equals(schedulerJobInstance.getContextInstanceId())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getJobName().equals(schedulerJobInstance.getJobName())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getContextName().equals(schedulerJobInstance.getContextName())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getChildContextName().equals(schedulerJobInstance.getChildContextName())
                                && schedulerJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
                                LOGGER.info(String.format("Removing queued Scheduler Job Initiation Event for Exclusive Job Lock Cache Queue. Job Name[%s], Job Plan Name[%s]," +
                                        " Child Context Name[%s], Job Plan Instance Id[%s]", schedulerJobInstance.getJobName(), schedulerJobInstance.getContextName()
                                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getContextInstanceId()));
                                return true;
                            }

                            return false;
                        });
                    } else {
                        jobLockHolder.getSchedulerJobInitiationEventWaitQueue().removeIf(record
                            -> {
                            if (record.getSchedulerJobInitiationEvent().getContextInstanceId().equals(schedulerJobInstance.getContextInstanceId())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getJobName().equals(schedulerJobInstance.getJobName())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getContextName().equals(schedulerJobInstance.getContextName())
                                && record.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getChildContextName().equals(schedulerJobInstance.getChildContextName())
                                && schedulerJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
                                LOGGER.info(String.format("Removing queued Scheduler Job Initiation Event for Job Lock Cache Queue. Job Name[%s], Job Plan Name[%s]," +
                                    " Child Context Name[%s], Job Plan Instance Id[%s]", schedulerJobInstance.getJobName(), schedulerJobInstance.getContextName()
                                    , schedulerJobInstance.getChildContextName(), schedulerJobInstance.getContextInstanceId()));
                                return true;
                            }

                            return false;
                        });
                    }


                    if (schedulerJobInstance.getStatus().equals(InstanceStatus.RUNNING)) {
                        boolean removed = jobLockHolder.removeLockHolder(schedulerJobInstance.getIdentifier() + CONTEXT_ID + schedulerJobInstance.getContextName());
                        if(removed) {
                            LOGGER.info(String.format("Removed running lock holder: %s", schedulerJobInstance.getIdentifier() + CONTEXT_ID + schedulerJobInstance.getContextName()));
                        }
                        else {
                            LOGGER.info(String.format("Could not remove running lock holder: %s", schedulerJobInstance.getIdentifier() + CONTEXT_ID + schedulerJobInstance.getContextName()));
                        }
                    }

                    saveJobLockCacheRecord(environment);
                    this.publishJobLockCacheEvent(jobLockHolder.getLockName(), schedulerJobInstance.getIdentifier()
                        , schedulerJobInstance.getChildContextName(), environment, JobLockCacheEvent.EventType.JOB_REMOVED_FROM_JOB_LOCK_QUEUE);
                }
            }
        }
    }

    @Override
    public synchronized List<ContextualisedSchedulerJobInitiationEvent> pollSchedulerJobInitiationEventWaitQueue
        (String jobIdentifier, String contextName, String environment) {
        this.initialiseJobLockCacheDataForEnvironment(environment);
        List<ContextualisedSchedulerJobInitiationEvent> removed = null;
        if (jobIdentifier != null && contextName != null
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier() != null
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier().containsKey(jobIdentifier)) {
            JobLockHolder jobLockHolder = this.getJobLockCacheData(environment).getJobLocksByLockName()
                .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier));
            if (jobLockHolder != null) {

                if(jobLockHolder.isExclusiveJobLock()) {
                    if (!this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().isEmpty()) {
                        removed = List.of(this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().poll());
                    }
                    else {
                        removed = new ArrayList<>();
                        for(JobLockHolder jlh: this.getJobLockCacheData(environment).getJobLocksByLockName().values()) {
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
                    else if (this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().size() > 0) {
                        removed = List.of(this.getJobLockCacheData(environment).getExclusiveLockSchedulerJobInitiationEventWaitQueue().poll());
                    }
                }

                if (removed != null) {
                    saveJobLockCacheRecord(environment);

                    removed.forEach(contextualisedSchedulerJobInitiationEvent -> {
                        this.publishJobLockCacheEvent(jobLockHolder.getLockName(), contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getIdentifier()
                            , contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getInternalEventDrivenJob().getChildContextName(), environment, JobLockCacheEvent.EventType.JOB_REMOVED_FROM_JOB_LOCK_QUEUE);
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
    public synchronized void setJobLockCacheRecord(JobLockCacheRecord jobLockCacheRecord, String environment) {
        environment = environment != null ? environment : JobLockCacheRecord.DEFAULT_ENVIRONMENT;

        if(!this.jobLockCacheRecordMap.containsKey(environment)) {
            LOGGER.debug(String.format("Setting job lock cache record", jobLockCacheRecord.getJobLockCache()));
            this.jobLockCacheRecordMap.put(environment, jobLockCacheRecord);
            this.getJobLockCacheData(environment).setJobLocksByLockName(jobLockCacheRecord.getJobLockCache().getJobLocksByLockName());
            this.getJobLockCacheData(environment).setJobLocksByIdentifier(jobLockCacheRecord.getJobLockCache().getJobLocksByIdentifier());
        }
    }

    @Override
    public synchronized void removeJobsLocksForContext(Context context) {
        this.initialiseJobLockCacheDataForEnvironment(context.getEnvironmentGroup());
        String environment = context.getEnvironmentGroup() != null ? context.getEnvironmentGroup() : JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        if(this.jobLockCacheDataMap.get(environment) != null) {
            this.jobLockCacheDataMap.get(environment).getJobLocksByLockName().entrySet().forEach(lockHolder -> {
                lockHolder.getValue().getSchedulerJobs().entrySet().forEach(entry -> {
                    entry.getValue().forEach(job -> {
                        if(job.getContextName().equals(context.getName())) {
                            LOGGER.debug(String.format("Removing job[%s], in context[%s], child[%s] from lock: [%s]"
                                , job.getJobName(), context.getName(), entry.getKey(), lockHolder.getKey()));
                        }
                    });
                });
                lockHolder.getValue().removeSchedulerJobsForContext(context);
            });

            ConcurrentHashMap<String, JobLockHolder> transientJobLocksByName = new ConcurrentHashMap<>();
            this.jobLockCacheDataMap.get(environment).getJobLocksByLockName().entrySet().forEach(entry -> {
                if(entry.getValue().getSchedulerJobs().size() > 0) {
                    transientJobLocksByName.put(entry.getKey(), entry.getValue());
                }
            });
            this.jobLockCacheDataMap.get(environment).setJobLocksByLockName(transientJobLocksByName);

            this.jobLockCacheDataMap.get(environment).getJobLocksByIdentifier().clear();
            this.jobLockCacheDataMap.get(environment).getJobLocksByLockName().entrySet().forEach(entry -> {
                List<SchedulerJob> jobs = this.jobLockCacheDataMap.get(environment).getJobLocksByLockName().get(entry.getKey())
                    .getSchedulerJobs()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

                for (SchedulerJob schedulerJob : jobs) {
                    this.jobLockCacheDataMap.get(environment).getJobLocksByIdentifier().put(schedulerJob.getIdentifier(), entry.getKey());
                }
            });

            this.saveJobLockCacheRecord(environment);
        }
    }

    /**
     * Initializes the job lock cache data for the given environment if it does not already exist in the cache.
     *
     * @param environment the environment for which to initialize the job lock cache data
     */
    private void initialiseJobLockCacheDataForEnvironment(String environment) {
        if(environment == null) environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;

        if(!this.jobLockCacheDataMap.containsKey(environment)) {
            this.jobLockCacheDataMap.put(environment, new JobLockCacheDataImpl());
        }
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
     * Sets the JobLockCacheEventBroadcaster for this JobLockCacheImpl instance.
     *
     * @param jobLockCacheEventBroadcaster the JobLockCacheEventBroadcaster to be set
     */
    public void setJobLockCacheEventBroadcaster(JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster) {
        this.jobLockCacheEventBroadcaster = jobLockCacheEventBroadcaster;
    }


    /**
     * Publishes a job lock cache event to the listeners asynchronously.
     *
     * @param jobLockName The name of the job lock.
     * @param jobIdentifier The identifier of the job.
     * @param contextName The name of the context.
     * @param eventType The type of event to be published.
     */
    private void publishJobLockCacheEvent(String jobLockName, String jobIdentifier, String contextName, String environment, JobLockCacheEvent.EventType eventType) {
        JobLockCacheEvent event = new JobLockCacheEventImpl(jobLockName, jobIdentifier, contextName, environment
            , eventType);
        this.executor.submit(() -> this.jobLockCacheEventListeners
            .forEach(listener -> listener.onJobLockCacheEvent(event)));
    }


    /**
     * Determines whether the current thread can take an exclusive lock based on the existing job locks.
     * This method checks if there are any exclusive locks and if there are any non-exclusive lock holders that prevent taking the lock.
     *
     * @return true if the exclusive lock can be taken, false otherwise
     */
    private synchronized boolean canTakeExclusiveLock(String environment) {
        AtomicBoolean canTakeExclusiveLock = new AtomicBoolean(true);

        AtomicBoolean areThereAnyExclusiveLocks = new AtomicBoolean(false);
        this.getJobLockCacheData(environment).getJobLocksByLockName().values().forEach(jobLockHolder -> {
            if(jobLockHolder.isExclusiveJobLock()) {
                areThereAnyExclusiveLocks.set(true);
            }
        });

        if(!areThereAnyExclusiveLocks.get()) {
            return canTakeExclusiveLock.get();
        }

        this.getJobLockCacheData(environment).getJobLocksByLockName().entrySet().forEach(entry -> {
            if(entry.getValue().getLockHolders().size() > 0) {
                canTakeExclusiveLock.set(false);
            }
        });

        if(this.getJobLockCacheData(environment).getExclusiveLockHolder().getLockHolders().size() > 0) {
            canTakeExclusiveLock.set(false);
        }

        return canTakeExclusiveLock.get();
    }


    /**
     * Checks if there are any non-exclusive lock holders currently holding locks in the JobLockCacheData.
     *
     * @return true if there are non-exclusive lock holders, false otherwise
     */
    private synchronized boolean areNonExclusiveLockHolders(String environment) {
        AtomicBoolean areNonExclusiveLockHolders = new AtomicBoolean(false);

        this.getJobLockCacheData(environment).getJobLocksByLockName().entrySet().forEach(entry -> {
            if(entry.getValue().getLockHolders().size() > 0 || entry.getValue().getSchedulerJobInitiationEventWaitQueue().size() > 0) {
                areNonExclusiveLockHolders.set(true);
            }
        });

        return areNonExclusiveLockHolders.get();
    }


    /**
     * Retrieves the JobLockHolder object for a specific job identifier.
     *
     * @param jobIdentifier the identifier of the job to retrieve the JobLockHolder for
     * @return the JobLockHolder object associated with the provided job identifier, or null if not found
     */
    private JobLockHolder getJobLockHolderForJobIdentifier(String jobIdentifier, String environment) {
        if(environment == null) environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        JobLockHolder jlh = null;
        if (jobIdentifier != null && this.jobLockCacheDataMap.containsKey(environment)
            && this.getJobLockCacheData(environment).getJobLocksByIdentifier().containsKey(jobIdentifier)
            && this.getJobLockCacheData(environment).getJobLocksByLockName().containsKey(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier))) {
            jlh = this.getJobLockCacheData(environment).getJobLocksByLockName()
                .get(this.getJobLockCacheData(environment).getJobLocksByIdentifier().get(jobIdentifier));
        }
        return jlh;
    }


    /**
     * Checks if the total count of locks held by participants, including the specified job, is greater than the lock
     * count set in the JobLockHolder.
     *
     * @param jlh The JobLockHolder containing information about job locks and participants
     * @param jobIdentifier The identifier of the job to check
     * @param contextName The name of the context related to the job
     * @return true if the total lock count by participants is greater than the lock count in JobLockHolder, false otherwise
     */
    private boolean workingCountIsGreaterThanToLockCount(JobLockHolder jlh, String jobIdentifier, String contextName) {
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
                if(holder.substring(0, holder.indexOf(CONTEXT_ID)).equals(schedulerJobLockParticipant.getIdentifier())) {
                    lockParticipantCount.addAndGet(schedulerJobLockParticipant.getLockCount());
                }
            });
        });

        if(jobLockParticipant != null) {
            lockParticipantCount.addAndGet(jobLockParticipant.get().getLockCount());
        }

        if(jlh.getLockHolders().isEmpty()) {
            return false;
        }
        else {
            return lockParticipantCount.get() > jlh.getLockCount();
        }
    }

    /**
     * Retrieves the JobLockCacheData for the specified environment.
     *
     * @param environment the environment for which to retrieve the JobLockCacheData
     * @return the JobLockCacheData for the specified environment, or the default JobLockCacheData if environment is null
     */
    public JobLockCacheData getJobLockCacheData(String environment) {
        environment = environment != null ? environment : JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        if(!this.jobLockCacheDataMap.containsKey(environment)) {
            this.jobLockCacheDataMap.put(environment, new JobLockCacheDataImpl());
        }

        return this.jobLockCacheDataMap.get(environment);
    }

    /**
     * Helper method to save the underlying cache record to the persistent store.
     */
    private void saveJobLockCacheRecord(String environment) {
        if(this.jobLockCacheService == null) return;
        if(environment == null) environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        if(!this.jobLockCacheRecordMap.containsKey(environment)) {
            JobLockCacheRecord jobLockCacheRecord = new JobLockCacheRecordImpl();
            jobLockCacheRecord.setEnvironment(environment);
            jobLockCacheRecordMap.put(environment, jobLockCacheRecord);
        }

        JobLockCacheRecord jobLockCacheRecord = this.jobLockCacheRecordMap.get(environment);
        jobLockCacheRecord.setJobLockCache(this.getJobLockCacheData(environment));
        this.jobLockCacheService.save(jobLockCacheRecord);
    }
}
