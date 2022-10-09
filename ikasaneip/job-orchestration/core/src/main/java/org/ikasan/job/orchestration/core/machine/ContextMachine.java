package org.ikasan.job.orchestration.core.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.component.endpoint.bigqueue.service.BigQueueDirectoryManagementServiceImpl;
import org.ikasan.job.orchestration.core.component.converter.ContextInstanceToContextInstanceStatusConverter;
import org.ikasan.job.orchestration.core.notification.MonitorManagement;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.bigqueue.service.BigQueueDirectoryManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextMachine {
    private Logger logger = LoggerFactory.getLogger(ContextMachine.class);
    private ContextInstance contextInstance;
    private JobLogicMachine jobLogicMachine;
    private ContextInstanceToContextInstanceStatusConverter statusConverter;
    private List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners;
    private ExecutorService statusListenerExecutor;
    private ExecutorService schedulerInitiatorEventRaisedListenerExecutor;
    private ExecutorService contextExecutor;
    private IBigQueue inboundQueue;
    private IBigQueue outboundQueue;
    private ListenableFuture<byte[]> inboundListenableFuture;
    private ListenableFuture<byte[]> outboundListenableFuture;
    private ObjectMapper objectMapper;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextService scheduledContextService;
    private SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener;
    private final JobLockCacheInitialisationService jobLockCacheInitialisationService;
    private ContextTemplate context;
    private int attempts;
    private long maxWait;
    private DryRunParameters dryRunParameters;
    private Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstances;
    private Map<String, ModuleMetaData> agents;
    private String queueDir;
    private JobLockCache jobLockCache;

    private OutboundQueueMessageRunner outboundQueueMessageRunner;

    private InboundQueueMessageRunner inboundQueueMessageRunner;

    public ContextMachine(ContextTemplate context, ContextInstance contextInstance, ScheduledContextInstanceService scheduledContextInstanceService,
                          Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstances, String queueDir,
                          Map<String, ModuleMetaData> agents, JobLockCache jobLockCache,
                          ContextParametersInstanceService contextParametersInstanceService,
                          ScheduledContextService scheduledContextService, SchedulerJobInstanceService schedulerJobInstanceService,
                          JobLockCacheInitialisationService jobLockCacheInitialisationService) {
        this.context = context;
        this.contextInstance = contextInstance;
        this.internalEventDrivenJobInstances = internalEventDrivenJobInstances;
        this.agents = agents;
        this.queueDir = queueDir;
        this.statusConverter = new ContextInstanceToContextInstanceStatusConverter();
        this.contextInstanceStateChangeEventListeners = new ArrayList<>();
        this.statusListenerExecutor = Executors.newSingleThreadExecutor();
        this.contextExecutor = Executors.newSingleThreadExecutor();
        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newSingleThreadExecutor();
        this.objectMapper = ObjectMapperFactory.newInstance();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.scheduledContextService = scheduledContextService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.jobLockCacheInitialisationService = jobLockCacheInitialisationService;
        this.jobLockCache = jobLockCache;
        this.jobLogicMachine = new JobLogicMachine(this.agents, this.jobLockCache, contextParametersInstanceService);
    }

    /**
     *
     * @throws IOException
     */
    public void init() throws IOException {
        String inboundQueueName = getInboundQueueName();
        String outboundQueueName = getOutboundQueueName();
        this.inboundQueue = new BigQueueImpl(this.queueDir, inboundQueueName);
        this.outboundQueue = new BigQueueImpl(this.queueDir, outboundQueueName);

        this.addInboundListener();
        this.addOutboundListener();
        this.saveContext();

        this.attempts = 0;
        this.maxWait = 10000L;
    }

    public void registerToNotificationMonitors() {
        MonitorManagement.startMonitoring(this);
    }

    private String getOutboundQueueName() {
        return "outbound-" + this.contextInstance.getId() + "-queue";
    }

    private String getInboundQueueName() {
        return "inbound-" + this.contextInstance.getId() + "-queue";
    }

    /**
     *
     * @throws JsonProcessingException
     */
    public void resetContextInstance() throws JsonProcessingException, SchedulerJobInstanceInitialisationException {
        if(this.context != null) {
            ContextService contextService = new ContextService();
            this.context = scheduledContextService.findByName(this.context.getName()).getContext();
            this.contextInstance = contextService.getContextInstance(contextService.getContextTemplateString(this.context));
            this.contextInstance.setId(UUID.randomUUID().toString());

            List<SchedulerJobInstance> schedulerJobInstances = this.schedulerJobInstanceService
                .initialiseSchedulerJobInstancesForContext(this.contextInstance);

            this.internalEventDrivenJobInstances  = schedulerJobInstances.stream()
                .filter(job -> job instanceof InternalEventDrivenJobInstance)
                .map(job -> (InternalEventDrivenJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.internalEventDrivenJobInstances.entrySet().forEach(job -> {
                if(job.getValue().isSkip()) {
                    ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), this.contextInstance);
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
                if(job.getValue().isHeld()) {
                    ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), this.contextInstance);
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isHeld());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            });

            this.saveContext();

            this.jobLockCacheInitialisationService.initialiseJobLockCache(this.context, true);
        }
    }

    /**
     *
     * @throws IOException
     */
    public void teardown() throws IOException {
        try {
            InstanceStatus previousStatus = contextInstance.getStatus();
            this.contextInstance.setStatus(InstanceStatus.ENDED);
            InstanceStatus newStatus = contextInstance.getStatus();
            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl
                (contextInstance, previousStatus, newStatus));

            this.saveContext();

            if (this.inboundQueue != null) {
                this.inboundQueueMessageRunner.stop();
                this.inboundQueue.close();
            }
            if (this.outboundQueue != null) {
                this.outboundQueueMessageRunner.stop();
                this.outboundQueue.close();
            }

            this.statusListenerExecutor.shutdownNow();
            this.contextExecutor.shutdownNow();
            this.schedulerInitiatorEventRaisedListenerExecutor.shutdownNow();

            if (contextInstanceStateChangeEventListeners != null) {
                contextInstanceStateChangeEventListeners.clear();
                this.contextInstanceStateChangeEventListeners = null;
            }

            this.statusListenerExecutor = null;
            this.schedulerInitiatorEventRaisedListenerExecutor = null;

            this.objectMapper = null;
            this.scheduledContextInstanceService = null;

            this.schedulerJobInitiationEventRaisedListener = null;
            this.context = null;
            this.dryRunParameters = null;
            this.internalEventDrivenJobInstances = null;
            this.agents = null;
            this.jobLockCache = null;
            this.contextExecutor = null;

            this.inboundListenableFuture = null;
            this.outboundListenableFuture = null;

            BigQueueDirectoryManagementService bigQueueDirectoryManagementService
                = new BigQueueDirectoryManagementServiceImpl(this.queueDir);
            if (this.inboundQueue != null) {
                this.inboundQueue.removeAll();
                this.inboundQueue.gc();
                bigQueueDirectoryManagementService.deleteQueue(getInboundQueueName());
            }
            if (this.outboundQueue != null) {
                this.outboundQueue.removeAll();
                this.outboundQueue.gc();
                bigQueueDirectoryManagementService.deleteQueue(getOutboundQueueName());
            }

            this.inboundQueue = null;
            this.outboundQueue = null;
            this.queueDir = null;

            this.contextInstance = null;
            this.jobLogicMachine = null;
            this.statusConverter = null;
        } catch (Exception e) {
            logger.warn(String.format("Could not tear down context machine: Error [%s]", e.getMessage()));
        }
    }

    /**
     *
     * @param listener
     */
    public void setSchedulerJobInitiationEventRaisedListener(SchedulerJobInitiationEventRaisedListener listener) {
        this.schedulerJobInitiationEventRaisedListener = listener;
    }

    /**
     *
     * @param bigQueueMessage
     * @return
     */
    public void eventReceived(String bigQueueMessage) throws IOException {
        this.inboundQueue.enqueue(bigQueueMessage.getBytes());
    }

    public void raiseEvent(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) throws IOException {
        BigQueueMessageBuilder<String> bigQueueMessageBuilder = new BigQueueMessageBuilder();
        bigQueueMessageBuilder.withMessage(this.objectMapper.writeValueAsString(contextualisedScheduledProcessEvent))
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedTime(System.currentTimeMillis());
        this.inboundQueue.enqueue(this.objectMapper.writeValueAsBytes(bigQueueMessageBuilder.build()));
    }


    /**
     * Get the context status by context name.
     *
     * @param contextName
     * @return
     */
    public InstanceStatus getContextStatus(String contextName) {
        ContextInstance instance = this.getContextInstanceByName(contextName, this.contextInstance);

        if(instance != null) {
            return instance.getStatus();
        }

        return null;
    }

    /**
     * Get the job status by context name and job name.
     *
     * @param contextName
     * @return
     */
    public InstanceStatus getJobStatus(String contextName, String jobIdentifier) {
        ContextInstance instance = this.getContextInstanceByName(contextName, this.contextInstance);

        SchedulerJobInstance schedulerJobInstance = instance.getScheduledJobsMap().get(jobIdentifier);

        if(schedulerJobInstance != null) {
            return schedulerJobInstance.getStatus();
        }

        return null;
    }

    /**
     *
     * @return
     */
    public ContextInstanceStatus getContextInstanceStatus() {
        return this.statusConverter.convert(this.contextInstance);
    }

    /**
     * Get the context by name.
     *
     * @param contextName
     * @return
     */
    public ContextInstance getContext(String contextName) {
        return this.getContextInstanceByName(contextName, this.contextInstance);
    }

    /**
     * Get the context by name.
     *
     * @return
     */
    public ContextInstance getContext() {
        return this.contextInstance;
    }

    /**
     *
     * @param listener
     */
    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.jobLogicMachine.addSchedulerJobStateChangeEventListener(listener);
    }

    /**
     *
     * @param listener
     */
    public void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.jobLogicMachine.removeSchedulerJobStateChangeEventListener(listener);
    }

    /**
     *
     * @param listener
     */
    public void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        if(!contextInstanceStateChangeEventListeners.contains(listener)) {
            this.contextInstanceStateChangeEventListeners.add(listener);
        }
    }

    /**
     *
     * @param listener
     */
    public void removeContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        if(contextInstanceStateChangeEventListeners.contains(listener)) {
            this.contextInstanceStateChangeEventListeners.remove(listener);
        }
    }

    /**
     *
     * @param dryRunParameters
     */
    public void setDryRunParameters(DryRunParameters dryRunParameters) {
        this.dryRunParameters = dryRunParameters;
    }

    /**
     *
     * @return
     */
    public boolean isDryRun() {
        return this.dryRunParameters != null;
    }

    /**
     *
     * @param jobIdentifier
     */
    public void skipJob(String jobIdentifier, String childContextName,  boolean skipFlag) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if(((!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) && (!schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED))) && skipFlag)
                || (!schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED) && !skipFlag)) {
                throw new ContextMachineException(String.format("Attempting to set skip flag to [%s] on job[%s], " +
                        "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot have the skip flag set."
                    , skipFlag, jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
            }

            InstanceStatus previousState = schedulerJobInstance.getStatus();

            schedulerJobInstance.setSkip(skipFlag);
            if(skipFlag) {
                schedulerJobInstance.setStatus(InstanceStatus.SKIPPED);
            }
            else {
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
            }
            this.saveContext();
            logger.info(String.format("Successfully set skip flag to [%s] on job[%s]. Context[%s], Context Instance[%s]."
                , skipFlag, jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));

            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));
        }
        else {
            throw new ContextMachineException(String.format("Attempting to set skip flag on job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     *
     * @param jobIdentifier
     * @param childContextName
     */
    public void holdJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if(!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) &&
                !schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED)) {
                throw new ContextMachineException(String.format("Attempting to hold job[%s], " +
                        "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be put on hold."
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
            }
            InstanceStatus previousState = schedulerJobInstance.getStatus();
            schedulerJobInstance.setHeld(true);
            schedulerJobInstance.setStatus(InstanceStatus.ON_HOLD);
            this.saveContext();
            logger.info(String.format("Successfully held job[%s]. Context[%s], Context Instance[%s]."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));

            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));
        }
        else {
            throw new ContextMachineException(String.format("Attempting to hold job[%s], however this job does not " +
                "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    public void resetJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if(!schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE) &&
                !schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
                throw new ContextMachineException(String.format("Attempting to reset job[%s], " +
                        "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be reset."
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
            }
            InstanceStatus previousState = schedulerJobInstance.getStatus();
            schedulerJobInstance.setStatus(InstanceStatus.WAITING);
            schedulerJobInstance.setInitiationEventRaised(false);

            ContextInstance child = ContextHelper.getChildContextInstance(childContextName, this.contextInstance);
            if(child.getStatus().equals(InstanceStatus.COMPLETE)) {
                child.setStatus(InstanceStatus.RUNNING);
                this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl(child, InstanceStatus.COMPLETE, InstanceStatus.RUNNING));
            }


            this.saveContext();
            logger.info(String.format("Successfully reset job[%s]. Context[%s], Context Instance[%s]."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));

            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));
        }
        else {
            throw new ContextMachineException(String.format("Attempting to reset job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     *
     * @param jobIdentifier
     * @throws IOException
     */
    public void releaseJob(String jobIdentifier, String childContextName) throws IOException {
        SchedulerJobInitiationEvent event = this.contextInstance.getHeldJobs().get(jobIdentifier+ "_" + childContextName);
        if(event != null) {
            InternalEventDrivenJobInstance instance = this.internalEventDrivenJobInstances.get(jobIdentifier);
            if(instance != null && instance.isTargetResidingContextOnly()) {
                event.getChildContextNames().clear();
                event.getChildContextNames().add(childContextName);
            }
            this.contextInstance.getHeldJobs().remove(jobIdentifier + "_" + childContextName);

            BigQueueMessage bigQueueMessage
                = new BigQueueMessageBuilder<>()
                .withMessage(event)
                .withMessageProperties(Map.of("contextName", this.context.getName(),
                                              "contextInstanceId", this.contextInstance.getId()))
                .build();

            String serialised = objectMapper.writeValueAsString(bigQueueMessage);
            logger.info("Enqueue job initiation event: " + serialised);
            outboundQueue.enqueue(serialised.getBytes());
            logger.info("Outbound queue size: " + outboundQueue.size());
            SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
            InstanceStatus previousState = schedulerJobInstance.getStatus();
            schedulerJobInstance.setHeld(false);
            schedulerJobInstance.setStatus(InstanceStatus.RELEASED);
            this.saveContext();
            logger.info(String.format("Successfully released job[%s]. Context[%s], Context Instance[%s]."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));
        }
        else {
            SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);

            if(schedulerJobInstance != null) {
                if(!schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
                    throw new ContextMachineException(String.format("Attempting to release job[%s], " +
                            "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be released."
                        , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
                }
                InstanceStatus previousState = schedulerJobInstance.getStatus();
                schedulerJobInstance.setHeld(false);
                schedulerJobInstance.setStatus(InstanceStatus.RELEASED);
                this.saveContext();
                logger.info(String.format("Successfully released job[%s]. Context[%s], Context Instance[%s]."
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
                jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                    , previousState, schedulerJobInstance.getStatus()));
            }
            else {
                StringBuffer heldJobs = new StringBuffer();
                this.contextInstance.getHeldJobs().entrySet().forEach(entry -> heldJobs.append(entry.getKey()).append(", "));
                String heldJobsString = heldJobs.toString().trim();
                if (heldJobsString.endsWith(",")) {
                    heldJobsString = heldJobsString.substring(0, heldJobsString.length() - 1);
                }

                throw new ContextMachineException(String.format("Attempting to release job[%s], however this job is not " +
                        "currently held in context[%s] with instance id[%s]. Current held jobs[%s]. Nor is the job found " +
                        "in the context or any of its nested contexts!"
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), heldJobsString));
            }
        }
    }

    /**
     * This method is responsible for providing a view onto jobs that can be run based on the receipt of an input event,
     * without impacting the state of the underlying context data model.
     *
     * @param contextualisedScheduledProcessEvent
     * @return
     */
    public List<SchedulerJobInitiationEvent> getEventsThatCanRun(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) {
        MutableBoolean lockRaised = new MutableBoolean(false);

        List<SchedulerJobInitiationEvent> events = this.getInitiationEvents(this.contextInstance
            , contextualisedScheduledProcessEvent, lockRaised, false);

        List<SchedulerJobInitiationEvent> finalEvents = new ArrayList<>();

        events.forEach(event -> {
            if(event.getInternalEventDrivenJob() != null) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, event.getInternalEventDrivenJob().getChildContextName(),
                    event.getInternalEventDrivenJob().getIdentifier());

                if (schedulerJobInstance != null && schedulerJobInstance.isHeld()) {
                    this.contextInstance.getHeldJobs().put(schedulerJobInstance.getIdentifier() + "_" + event.getInternalEventDrivenJob().getChildContextName(), event);
                } else {
                    finalEvents.add(event);
                }
            }
            else {
                logger.warn(String.format("Could not load internal event driven job for initiation event JobName[%s], SchedulerJobInitiationEvent[%s]"
                    , event.getJobName(), event.toString()));
            }
        });

        return events;
    }

    /**
     *
     * @param scheduledProcessEvent
     * @return
     */
    protected List<SchedulerJobInitiationEvent> eventReceived(ContextualisedScheduledProcessEvent scheduledProcessEvent) {
        logger.info("Context Machine Received Event [{}]", scheduledProcessEvent);

        ContextInstance previousContextInstance = this.contextInstance;

        MutableBoolean lockRaised = new MutableBoolean(false);

        List<SchedulerJobInitiationEvent> events = this.getInitiationEvents(this.contextInstance, scheduledProcessEvent, lockRaised, true);

        List<SchedulerJobInitiationEvent> finalEvents = new ArrayList<>();

        events.forEach(event -> {
            if(event.getInternalEventDrivenJob() != null) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, event.getInternalEventDrivenJob().getChildContextName(),
                    event.getInternalEventDrivenJob().getIdentifier());

                if (schedulerJobInstance != null && schedulerJobInstance.isHeld()) {
                    this.contextInstance.getHeldJobs().put(schedulerJobInstance.getIdentifier() + "_" + event.getInternalEventDrivenJob().getChildContextName(), event);
                } else {
                    finalEvents.add(event);
                }
            }
            else {
                logger.warn(String.format("Could not load internal event driven job for initiation event JobName[%s], SchedulerJobInitiationEvent[%s]"
                    , event.getJobName(), event.toString()));
            }
        });

        saveInstanceAuditRecord(scheduledProcessEvent, finalEvents, previousContextInstance, this.contextInstance);

        return finalEvents;
    }

    private void saveInstanceAuditRecord(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                         List<SchedulerJobInitiationEvent> finalEvents,
                                         ContextInstance previousContextInstance,
                                         ContextInstance updatedContextInstance) {
        ScheduledContextInstanceAuditAggregate contextInstanceAudit = new ScheduledContextInstanceAuditAggregateImpl();
        contextInstanceAudit.setProcessEvent(scheduledProcessEvent);
        contextInstanceAudit.setSchedulerJobInitiationEvents(finalEvents);

        ScheduledContextInstanceAuditAggregateRecord auditRecord = new ScheduledContextInstanceAuditAggregateRecordImpl();
        auditRecord.setContextName(this.contextInstance.getName());
        auditRecord.setContextInstanceId(this.contextInstance.getId());
        auditRecord.setScheduledProcessEventName(scheduledProcessEvent.getJobName());
        auditRecord.setScheduledContextInstanceAuditAggregate(contextInstanceAudit);
        scheduledContextInstanceService.saveAudit(auditRecord, previousContextInstance, updatedContextInstance);
    }

    /**
     * Helper method to determine if there are any SchedulerJobInitiationEvent to be raised. This method employs recursion to determine
     * which context, if any, that the job associated with the scheduled process event is associated with. It then delegates to the
     * JobLogicMachine to determine if there are any SchedulerJobInitiationEvent to raise.
     *
     * @param contextInstance
     * @param scheduledProcessEvent
     * @return
     */
    private List<SchedulerJobInitiationEvent> getInitiationEvents(ContextInstance contextInstance, ContextualisedScheduledProcessEvent scheduledProcessEvent
        , MutableBoolean lockRaised, boolean markAsRaised) {
        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

        if(!contextInstance.getStatus().equals(InstanceStatus.COMPLETE)
             && contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {

             // Delegate to the JobLogicMachine to determine if any SchedulerJobInitiationEvents are
             // required to be raised.
             List<SchedulerJobInitiationEvent> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                 , contextInstance, this.dryRunParameters, this.internalEventDrivenJobInstances, this.contextInstance.getContextParameters()
                 , this.contextInstance, lockRaised, markAsRaised);

             // Update the context status after event received and attached
             // to the job instance.
             this.setContextStatus(contextInstance);

             if(contextInstance.getContexts() == null || contextInstance.getContexts().isEmpty()) {
                 return events;
             }
             else {
                 results.addAll(events);
             }
        }


        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(ContextInstance instance: contextInstance.getContexts()) {
                // Recursively work our way through all nested contexts to determine if any job initiation events need to be raised.
                results.addAll(this.getInitiationEvents(instance, scheduledProcessEvent,lockRaised, markAsRaised));
                this.setContextStatus(contextInstance);
            }
        }

        return results;
    }

    /**
     * Helper method to recursively get a ContextInstance by its name.
     *
     * @param contextName
     * @param contextInstance
     * @return
     */
    private ContextInstance getContextInstanceByName(String contextName, ContextInstance contextInstance) {
        if(contextInstance.getName().equals(contextName)) {
            return contextInstance;
        }

        if(contextInstance.getContexts() != null) {
            for(ContextInstance context: contextInstance.getContexts()) {
                ContextInstance result = getContextInstanceByName(contextName, context);

                if(result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Helper method to set the context status.
     *
     * @param contextInstance
     */
    private void setContextStatus(ContextInstance contextInstance) {
        AtomicBoolean allJobsComplete = new AtomicBoolean(true);
        AtomicBoolean anyRunningOrCompletedJobs = new AtomicBoolean(false);
        AtomicBoolean anyErrorJobs = new AtomicBoolean(false);
        AtomicBoolean allContextsComplete = new AtomicBoolean(true);
        AtomicBoolean anyRunningOrCompletedContexts = new AtomicBoolean(false);
        AtomicBoolean anyErrorContexts = new AtomicBoolean(false);

        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {

            contextInstance.getScheduledJobs().forEach(job -> {
                if (!job.getStatus().equals(InstanceStatus.COMPLETE)
                    && !job.getStatus().equals(InstanceStatus.SKIPPED)
                    && !job.getStatus().equals(InstanceStatus.SKIPPED_RUNNING)
                    && !job.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
                    allJobsComplete.set(false);
                }
                if (job.getStatus().equals(InstanceStatus.RUNNING)
                    || job.getStatus().equals(InstanceStatus.COMPLETE)
                    || job.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
                    anyRunningOrCompletedJobs.set(true);
                }
                if (job.getStatus().equals(InstanceStatus.ERROR)) {
                    anyErrorJobs.set(true);
                }
            });
        }

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {

            contextInstance.getContexts().forEach(context -> {
                if (!context.getStatus().equals(InstanceStatus.COMPLETE)) {
                    allContextsComplete.set(false);
                }
                if (context.getStatus().equals(InstanceStatus.RUNNING)
                    || context.getStatus().equals(InstanceStatus.COMPLETE)) {
                    anyRunningOrCompletedContexts.set(true);
                }
                if (context.getStatus().equals(InstanceStatus.ERROR)) {
                    anyErrorContexts.set(true);
                }
            });
        }

        InstanceStatus previousStatus = contextInstance.getStatus();

        if (anyErrorJobs.get() || anyErrorContexts.get()) {
            contextInstance.setStatus(InstanceStatus.ERROR);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        } else if(allJobsComplete.get() && allContextsComplete.get()) {
            contextInstance.setStatus(InstanceStatus.COMPLETE);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        } else if(anyRunningOrCompletedJobs.get() || anyRunningOrCompletedContexts.get()){
            contextInstance.setStatus(InstanceStatus.RUNNING);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        }

        InstanceStatus newStatus = contextInstance.getStatus();

        if(!previousStatus.equals(newStatus)) {
            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl(contextInstance, previousStatus, newStatus));
        }
    }

    public void addQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event) {
        ContextInstance childContextInstance = ContextHelper.getChildContextInstance(event.getInternalEventDrivenJob().getChildContextName()
            , this.contextInstance);

        this.jobLogicMachine.addQueuedSchedulerJobInitiationEvent(childContextInstance, this.contextInstance
            , event.getInternalEventDrivenJob().getIdentifier(), event);

        this.setContextStatus(childContextInstance);

        this.saveContext();
    }

    private void issueContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event) {
        this.statusListenerExecutor.submit(() -> this.contextInstanceStateChangeEventListeners
            .forEach(listener -> listener.onContextInstanceStateChangeEvent(event)));
    }

    private void saveContext() {
        ScheduledContextInstanceRecord scheduledContextInstanceRecord
            = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(this.contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(this.contextInstance);
        scheduledContextInstanceRecord.setTimestamp(this.contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(this.contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    private SchedulerJobInstance getSchedulerJob(ContextInstance contextInstance, String childContextName, String jobIdentifier) {
        if(contextInstance.getScheduledJobsMap() != null && contextInstance.getScheduledJobsMap().containsKey(jobIdentifier) && contextInstance.getName().equals(childContextName)) {
            return contextInstance.getScheduledJobsMap().get(jobIdentifier);
        }
        else if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            for(ContextInstance contextInstance1: contextInstance.getContexts()) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance1, childContextName, jobIdentifier);

                if(schedulerJobInstance != null) {
                    return schedulerJobInstance;
                }
            }

            return null;
        }

        return null;
    }

    protected class InboundQueueMessageRunner implements Runnable {
        private final AtomicBoolean running = new AtomicBoolean(true);

        @Override
        public void run() {
            try {
                if (!this.running.get()) {
                    return;
                }

                byte[] event = inboundQueue.peek();

                if(event == null) {
                    return;
                }

                BigQueueMessage<ContextualisedScheduledProcessEvent> bigQueueMessage
                     = objectMapper.readValue(event, BigQueueMessageImpl.class);

                ContextualisedScheduledProcessEvent scheduledProcessEvent
                    = objectMapper.readValue(String.valueOf(bigQueueMessage.getMessage()), ContextualisedScheduledProcessEventImpl.class);

                List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = eventReceived(scheduledProcessEvent);

                saveContext();

                for(SchedulerJobInitiationEvent schedulerJobInitiationEvent: schedulerJobInitiationEvents) {

                    BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage
                        = new BigQueueMessageBuilder<SchedulerJobInitiationEvent>().withMessage(schedulerJobInitiationEvent)
                        .withMessageProperties(
                            Map.of("contextName", schedulerJobInitiationEvent.getContextName(),
                                "contextInstanceId", schedulerJobInitiationEvent.getContextInstanceId()))
                        .build();

                    String serialised = objectMapper.writeValueAsString(outgoingBigQueueMessage);
                    logger.info("Enqueue job initiation event: " + serialised);
                    outboundQueue.enqueue(serialised.getBytes());
                    logger.info("Outbound queue size: " + outboundQueue.size());
                }

                inboundQueue.dequeue();
                inboundQueue.gc();
            }
            catch (Exception e) {
                // do something
                e.printStackTrace();
            }
            finally {
                addInboundListener();
            }
        }

        public void stop() {
            this.running.set(false);
        }

        public void start() {
            this.running.set(true);
        }
    }

    protected class OutboundQueueMessageRunner implements Runnable {
        private final AtomicBoolean running = new AtomicBoolean(true);

        @Override
        public void run() {
            if (!this.running.get()) {
                return;
            }

            boolean exception = false;
            BigQueueMessage bigQueueMessage = null;
            try {
                byte[] event = outboundQueue.peek();
                if(event == null) {
                    return;
                }

                bigQueueMessage = objectMapper.readValue(event, BigQueueMessageImpl.class);
                String messageAsString = new String(objectMapper.writeValueAsBytes(bigQueueMessage.getMessage()));
                SchedulerJobInitiationEvent schedulerJobInitiationEvent
                    = objectMapper.readValue(messageAsString, SchedulerJobInitiationEventImpl.class);

                if(schedulerJobInitiationEventRaisedListener != null) {
                    schedulerJobInitiationEventRaisedListener.onSchedulerJobInitiationEventRaised(schedulerJobInitiationEvent);
                }

                // We've been successful so set the attempts back to 0.
                attempts = 0;
            }
            catch (Exception e) {
                e.printStackTrace();
                exception = true;
                try {
                    // If an exception occurs trying to raise the event, then put the message onto the back of the queue.
                    outboundQueue.enqueue(outboundQueue.dequeue());
                    outboundQueue.gc();

                    // We are using an exponential retry back off which is calculated here.
                    long sleepTime = 500L*attempts*1;

                    if(sleepTime > maxWait) {
                        sleepTime = maxWait;
                    }
                    Thread.sleep(sleepTime);
                    attempts++;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            finally {
                // We only dequeue messages when there has been no exception.
                if(!exception) {
                    try {
                        outboundQueue.dequeue();
                        outboundQueue.gc();
                        logger.info("Dequeue event: " + bigQueueMessage);
                        logger.info("Outbound queue size: " + outboundQueue.size());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                addOutboundListener();
            }
        }

        public void stop() {
            this.running.set(false);
        }

        public void start() {
            this.running.set(true);
        }
    }

    private void addInboundListener() {
        inboundListenableFuture = this.inboundQueue.peekAsync();
        this.inboundQueueMessageRunner = new InboundQueueMessageRunner();
        inboundListenableFuture.addListener(inboundQueueMessageRunner, this.contextExecutor);
    }

    protected void addOutboundListener() {
        outboundListenableFuture = outboundQueue.peekAsync();
        this.outboundQueueMessageRunner = new OutboundQueueMessageRunner();
        outboundListenableFuture.addListener(outboundQueueMessageRunner, schedulerInitiatorEventRaisedListenerExecutor);
    }
}
