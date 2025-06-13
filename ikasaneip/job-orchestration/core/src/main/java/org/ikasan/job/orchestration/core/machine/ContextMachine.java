package org.ikasan.job.orchestration.core.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.StopWatch;
import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.component.endpoint.bigqueue.service.BigQueueDirectoryManagementServiceImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.JobThreadFactory;
import org.ikasan.job.orchestration.core.component.converter.ContextInstanceToContextInstanceStatusConverter;
import org.ikasan.job.orchestration.core.notification.MonitorManagement;
import org.ikasan.job.orchestration.model.context.ContextTransition;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstancesInitialisationParametersImpl;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.job.orchestration.service.BigQueueContextMachineManagementServiceImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.bigqueue.service.BigQueueDirectoryManagementService;
import org.ikasan.spec.bigqueue.service.BigQueueManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
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
import org.ikasan.spec.scheduled.instance.service.*;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;

public class ContextMachine {
    private Logger logger = LoggerFactory.getLogger(ContextMachine.class);

    public static final String MANUAL_SUBMISSION = "group (manual fire)";

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
    private final ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;
    private final ContextParametersInstanceService contextParametersInstanceService;
    private ContextTemplate context;
    private int attempts;
    private long maxWait;
    private DryRunParameters dryRunParameters;
    /**
     * Key = jobId + childContextName.
     */
    private Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstances;
    private Map<String, GlobalEventJobInstance> globalEventJobInstanceMap;
    private Map<String, ContextStartJobInstance> contextStartJobInstanceMap;
    private Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap;
    private Map<String, LocalEventJobInstance> localEventJobInstanceMap;
    private Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap;
    private Map<String, BridgingJobInstance> bridgingJobInstanceMap;
    private Map<String, ModuleMetaData> agents;
    private ModuleMetaDataService moduleMetaDataService;
    private String queueDir;
    private JobLockCache jobLockCache;

    private OutboundQueueMessageRunner outboundQueueMessageRunner;

    private InboundQueueMessageRunner inboundQueueMessageRunner;
    private ContextStateHelper contextStateHelper;
    private JobUtilsService jobUtilsService;
    private boolean tornDown = false;

    public ContextMachine(ContextTemplate context, ContextInstance contextInstance, ScheduledContextInstanceService scheduledContextInstanceService,
                          Map<String, GlobalEventJobInstance> globalEventJobInstanceMap,
                          Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap,
                          Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobInstances,
                          Map<String, ContextStartJobInstance> contextStartJobInstanceMap,
                          Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap,
                          Map<String, LocalEventJobInstance> localEventJobInstanceMap,
                          Map<String, BridgingJobInstance> bridgingJobInstanceMap,
                          String queueDir,
                          Map<String, ModuleMetaData> agents, ModuleMetaDataService moduleMetaDataService, JobLockCache jobLockCache,
                          ContextParametersInstanceService contextParametersInstanceService,
                          ScheduledContextService scheduledContextService, SchedulerJobInstanceService schedulerJobInstanceService,
                          JobLockCacheInitialisationService jobLockCacheInitialisationService,
                          ContextInstancePublicationService<ContextInstance> contextInstancePublicationService,
                          JobUtilsService jobUtilsService) {
        this.context = context;
        this.contextInstance = contextInstance;
        ContextHelper.enrichJobs(contextInstance);
        this.internalEventDrivenJobInstances = internalEventDrivenJobInstances;
        this.globalEventJobInstanceMap = globalEventJobInstanceMap;
        if (this.globalEventJobInstanceMap == null) {
            this.globalEventJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.quartzScheduleDrivenJobInstanceMap = quartzScheduleDrivenJobInstanceMap;
        if (this.quartzScheduleDrivenJobInstanceMap == null) {
            this.quartzScheduleDrivenJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.contextStartJobInstanceMap = contextStartJobInstanceMap;
        if (this.contextStartJobInstanceMap == null) {
            this.contextStartJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.contextTerminalJobInstanceMap = contextTerminalJobInstanceMap;
        if (this.contextTerminalJobInstanceMap == null) {
            this.contextTerminalJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.localEventJobInstanceMap = localEventJobInstanceMap;
        if (this.localEventJobInstanceMap == null) {
            this.localEventJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.bridgingJobInstanceMap = bridgingJobInstanceMap;
        if (this.bridgingJobInstanceMap == null) {
            this.bridgingJobInstanceMap = new HashMap<>(); // Empty Hashmap if the value is null.
        }
        this.agents = agents;
        this.moduleMetaDataService = moduleMetaDataService;
        this.queueDir = queueDir;
        this.statusConverter = new ContextInstanceToContextInstanceStatusConverter();
        this.contextInstanceStateChangeEventListeners = new ArrayList<>();
        this.statusListenerExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-StatusChangeListener"));
        this.contextExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-ContextExecutor"));
        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-EventRaisedListener"));
        this.objectMapper = ObjectMapperFactory.newInstance();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.scheduledContextService = scheduledContextService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.jobLockCacheInitialisationService = jobLockCacheInitialisationService;
        this.contextInstancePublicationService = contextInstancePublicationService;
        this.contextParametersInstanceService = contextParametersInstanceService;
        this.jobLockCache = jobLockCache;
        this.jobUtilsService = jobUtilsService;
        this.jobLogicMachine = new JobLogicMachine(this.agents, this.moduleMetaDataService, this.jobLockCache, contextParametersInstanceService);
        this.contextStateHelper = new ContextStateHelper();
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

    public void unregisterToNotificationMonitors() {
        logger.info("Call to stop monitoring for the context {} and instanceId {}", this.contextInstance.getName(), this.contextInstance.getId());
        MonitorManagement.stopMonitoring(this);
    }

    public String getOutboundQueueName() {
        return "outbound-" + this.contextInstance.getId() + "-queue";
    }

    public String getInboundQueueName() {
        return "inbound-" + this.contextInstance.getId() + "-queue";
    }

    public IBigQueue getInboundQueue() {
        return this.inboundQueue;
    }

    public IBigQueue getOutboundQueue() {
        return this.outboundQueue;
    }

    /**
     * Helper method to reset the context instance held by the context machine.
     *
     * @throws JsonProcessingException
     */
    public void resetContextInstance(boolean holdCommandJobs, boolean initiateWithSameParameters,
                                     List<ContextParameterInstance> contextParameterInstances) throws IOException, SchedulerJobInstanceInitialisationException {
        if(this.context != null) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String contextName = this.contextInstance.getName();

            // remove the context instance from the agents
            this.removeContextInstanceFromAgents();

            stopWatch.stop();
            logger.info("Removed contexts from agents! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();

            stopWatch.start();
            this.killRunningJobs();
            this.teardownBigQueue();
            stopWatch.stop();
            logger.info("Cleaned up old instance running jobs and bigqueue! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();
            ContextService contextService = new ContextService();
            this.context = scheduledContextService.findByName(contextName).getContext();

            ContextInstance previousContextInstance = SerializationUtils.clone(this.contextInstance);

            this.contextInstance = contextService.getContextInstance(contextService.getContextTemplateString(this.context));
            this.contextInstance.setId(UUID.randomUUID().toString());
            ContextHelper.enrichJobs(contextInstance);

            this.init();

            stopWatch.stop();
            logger.info("Initialised the new context! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();

            SchedulerJobInstancesInitialisationParameters parameters
                = new SchedulerJobInstancesInitialisationParametersImpl(holdCommandJobs);
            List<SchedulerJobInstance> schedulerJobInstances = this.schedulerJobInstanceService
                .initialiseSchedulerJobInstancesForContext(this.context, this.contextInstance, parameters);

            this.internalEventDrivenJobInstances  = schedulerJobInstances.stream()
                .filter(job -> job instanceof InternalEventDrivenJobInstance)
                .map(job -> (InternalEventDrivenJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.globalEventJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof GlobalEventJobInstance)
                .map(job -> (GlobalEventJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.contextStartJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof ContextStartJobInstance)
                .map(job -> (ContextStartJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.contextTerminalJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof ContextTerminalJobInstance)
                .map(job -> (ContextTerminalJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.localEventJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof LocalEventJobInstance)
                .map(job -> (LocalEventJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.bridgingJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof BridgingJobInstance)
                .map(job -> (BridgingJobInstance)job)
                .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            if(holdCommandJobs) {
                ContextHelper.holdAllJobs(this.contextInstance, this.internalEventDrivenJobInstances);
            }

            this.internalEventDrivenJobInstances.entrySet().forEach(job -> {
                if(job.getValue().isSkip()) {
                    ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), this.contextInstance);
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
                if(job.getValue().isHeld()) {
                    ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), this.contextInstance);
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setHeld(job.getValue().isHeld());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            });

            this.globalEventJobInstanceMap.entrySet().forEach(job -> {
                if(job.getValue().isSkip()) {
                    ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), this.contextInstance);
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            });

            stopWatch.stop();
            logger.info("Sorted out the new job instances! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();

            if(contextParameterInstances != null) {
                this.contextInstance.setContextParameters(contextParameterInstances);
            } else if (initiateWithSameParameters) {
                this.contextInstance.setContextParameters(previousContextInstance.getContextParameters());
            } else {
                contextParametersInstanceService.populateContextParametersOnContextInstance(this.contextInstance
                    , this.internalEventDrivenJobInstances);
            }

            stopWatch.stop();
            logger.info("Sorted out the context parameters! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();

            // Propagate the new context instance to all agents.
            this.propagateContextInstanceToAgents();

            stopWatch.stop();
            logger.info("Sent new context instance to agents! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();

            // Initialise the job lock cache for the new instance
            this.jobLockCacheInitialisationService.initialiseJobLockCache(this.context, true);

            this.contextInstance.setStartTime(System.currentTimeMillis());
            this.contextInstance.setProjectedEndTime(CronUtils.getEpochMilliOfPreviousFireTime(this.contextInstance.getTimeWindowStart()) + this.contextInstance.getContextTtlMilliseconds());

            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl
                (previousContextInstance.getId(), previousContextInstance, previousContextInstance.getStatus(), InstanceStatus.ENDED));

            this.saveContext();

            stopWatch.stop();
            logger.info("Saved the new context instances! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();
        }
    }

    /**
     * Removes the previous context instance from all agents.
     */
    private void removeContextInstanceFromAgents() {
        // Remove the previous context instance from all agents
        for (var agent : this.agents.entrySet()) {
            // Find the url from solr. If it does not exist (maybe due to accidental removal) then use what's given at the start of the Context Instance creation
            String url;
            ModuleMetaData agentMetaFromSolr = moduleMetaDataService.findById(agent.getKey());
            if (agentMetaFromSolr == null || StringUtils.isBlank(agentMetaFromSolr.getUrl())) {
                url = agent.getValue().getUrl();
            } else {
                url = agentMetaFromSolr.getUrl();
            }
            this.contextInstancePublicationService.remove(url, this.contextInstance);
        }
    }

    /**
     * Propagates the new context instance to all agents.
     */
    private void propagateContextInstanceToAgents() {
        // Propagate the new context instance to all agents.
        for (var agent : this.agents.entrySet()) {
            // Find the url from solr. If it does not exist (maybe due to accidental removal) then use what's given at the start of the Context Instance creation
            String url;
            ModuleMetaData agentMetaFromSolr = moduleMetaDataService.findById(agent.getKey());
            if (agentMetaFromSolr == null || StringUtils.isBlank(agentMetaFromSolr.getUrl())) {
                url = agent.getValue().getUrl();
            } else {
                url = agentMetaFromSolr.getUrl();
            }
            this.contextInstancePublicationService.publish(url, this.contextInstance);
        }
    }

    /**
     * Method to tear down big queue only.
     *
     * @throws IOException
     */
    private void teardownBigQueue() throws IOException {
        BigQueueManagementService bigQueueManagementService =
            new BigQueueContextMachineManagementServiceImpl(getInboundQueueName(),
                inboundQueue, getOutboundQueueName(), outboundQueue);

        BigQueueDirectoryManagementService bigQueueDirectoryManagementService
            = new BigQueueDirectoryManagementServiceImpl(bigQueueManagementService, this.queueDir);

        if (this.inboundQueue != null) {
            this.inboundQueueMessageRunner.stop();
            this.inboundQueue.close();
            this.inboundQueue.removeAll();
            this.inboundQueue.gc();
            bigQueueDirectoryManagementService.deleteQueue(getInboundQueueName());
            this.inboundQueueMessageRunner.start();
        }
        if (this.outboundQueue != null) {
            this.outboundQueueMessageRunner.stop();
            this.outboundQueue.close();
            this.outboundQueue.removeAll();
            this.outboundQueue.gc();
            bigQueueDirectoryManagementService.deleteQueue(getOutboundQueueName());
            this.outboundQueueMessageRunner.start();
        }
    }

    /**
     * Method to tear down context machine internals.
     *
     * @throws IOException
     */
    public void teardown() throws IOException {
        this.tornDown = true;
        try {
            InstanceStatus previousStatus = contextInstance.getStatus();
            this.contextInstance.setStatus(InstanceStatus.ENDED);
            this.contextInstance.setEndTime(System.currentTimeMillis());
            InstanceStatus newStatus = contextInstance.getStatus();
            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl
                (contextInstance.getId(), contextInstance, previousStatus, newStatus));

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
            this.globalEventJobInstanceMap = null;
            this.agents = null;
            this.jobLockCache = null;
            this.contextExecutor = null;

            this.inboundListenableFuture = null;
            this.outboundListenableFuture = null;

            this.jobLogicMachine.getExecutor().shutdownNow(); // remove the executor threads on this Job Logic Machine
            this.jobLogicMachine = null;

            BigQueueManagementService bigQueueManagementService =
                new BigQueueContextMachineManagementServiceImpl(getInboundQueueName(),
                    inboundQueue, getOutboundQueueName(), outboundQueue);

            BigQueueDirectoryManagementService bigQueueDirectoryManagementService
                = new BigQueueDirectoryManagementServiceImpl(bigQueueManagementService, this.queueDir);
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
        // If the context machine is torn down we ignore the message.
        if(!this.tornDown && this.inboundQueue != null) {
            this.inboundQueue.enqueue(bigQueueMessage.getBytes());
        }
        else {
            logger.warn("Ignoring inbound message[{}], tornDown[{}]].", bigQueueMessage, tornDown);
        }
    }

    public void raiseEvent(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) throws IOException {
        BigQueueMessageBuilder<String> bigQueueMessageBuilder = new BigQueueMessageBuilder();
        bigQueueMessageBuilder.withMessage(this.objectMapper.writeValueAsString(contextualisedScheduledProcessEvent))
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedTime(System.currentTimeMillis());

        // We have the edge case where the inbound queue might have been torn down when an event
        // is raised. If it has the inbound queue could be null so let's protect ourselves against
        // that.
        if(this.inboundQueue != null) {
            this.inboundQueue.enqueue(this.objectMapper.writeValueAsBytes(bigQueueMessageBuilder.build()));
        }
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
     * Retrieves the status of the current context instance.
     *
     * @return The status of the current context instance.
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
     * Method to disable the quartz based jobs associated with the context instance.
     */
    public void disableQuartzBasedJobs() {
        this.contextInstance.setQuartzScheduleDrivenJobsDisabledForContext(true);
        this.quartzScheduleDrivenJobInstanceMap.values().forEach(quartzScheduleDrivenJobInstance -> {
            SchedulerJobInstance schedulerJobInstance = ContextHelper.getSchedulerJobInstance(quartzScheduleDrivenJobInstance.getJobName(),
                quartzScheduleDrivenJobInstance.getChildContextName(), contextInstance);

            if(schedulerJobInstance != null) {
                schedulerJobInstance.setStatus(InstanceStatus.DISABLED);
            }
        });
        this.saveContext();
    }

    /**
     * Method to enable the quartz based jobs associated with the context instance.
     */
    public void enableQuartzBasedJobs() {
        this.contextInstance.setQuartzScheduleDrivenJobsDisabledForContext(false);
        this.quartzScheduleDrivenJobInstanceMap.values().forEach(quartzScheduleDrivenJobInstance -> {
            SchedulerJobInstance schedulerJobInstance = ContextHelper.getSchedulerJobInstance(quartzScheduleDrivenJobInstance.getJobName(),
                quartzScheduleDrivenJobInstance.getChildContextName(), contextInstance);

            if(schedulerJobInstance != null) {
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
            }
        });
        this.saveContext();
    }

    public void runContextUntilManuallyEnded() {
        this.contextInstance.setRunContextUntilManuallyEnded(true);
        this.saveContext();
    }

    /**
     * Method to set a job as skipped.
     *
     * @param jobIdentifier
     * @param childContextName
     * @param skipFlag
     */
    public void skipJob(String jobIdentifier, String childContextName,  boolean skipFlag) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + childContextName) &&
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-" + childContextName).isTargetResidingContextOnly()) {
                // if a job is targeting a specific context, we only hold it for that context!
                this._skipJob(List.of(schedulerJobInstance), skipFlag, false);
            } else {
                List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, jobIdentifier);
                this._skipJob(jobInstances, skipFlag, false);
            }
        }
        else {
            throw new ContextMachineException(String.format("Attempting to set skip flag on job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     * Method to set a job as skipped for all jobs under a context.
     *
     * @param childContextName
     * @param skipFlag
     */
    public void skipJobs(String childContextName,  boolean skipFlag) {
        Map<String, SchedulerJobInstance> schedulerJobInstanceMap
            = ContextHelper.getAllJobs(ContextHelper.getChildContextInstance(childContextName,contextInstance));

        schedulerJobInstanceMap.values().forEach(schedulerJobInstance -> {
            Map<String, SchedulerJob> jobs = new HashMap<>();
            this.internalEventDrivenJobInstances.entrySet().forEach(entry -> {
                jobs.put(entry.getKey(), entry.getValue());
            });
            this.globalEventJobInstanceMap.entrySet().forEach(entry -> {
                jobs.put(entry.getKey(), entry.getValue());
            });
            List<ContextTransition> contextTransitions = ContextHelper.determineIfSchedulerJobsTransitionFromOtherContexts(this.contextInstance, schedulerJobInstance.getJobName(),
                schedulerJobInstance.getChildContextName(), jobs);
            if ((this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-"
                + schedulerJobInstance.getChildContextName()) ||
                this.globalEventJobInstanceMap.containsKey(JobConstants.GLOBAL_EVENT + "-" + schedulerJobInstance.getJobName() + "-"
                    + schedulerJobInstance.getChildContextName())) && contextTransitions.isEmpty()) {
                if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-"
                    + schedulerJobInstance.getChildContextName()) && this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-"
                    + schedulerJobInstance.getChildContextName()).isTargetResidingContextOnly()) {
                    // if a job is targeting a specific context, we only hold it for that context!
                    this._skipJob(List.of(schedulerJobInstance), skipFlag, true);
                } else {
                    List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, schedulerJobInstance.getIdentifier());
                    this._skipJob(jobInstances, skipFlag, true);
                }
            }
        });
    }

    /**
     * Helper method to do the heavy lifting of skipping jobs.
     *
     * @param jobs
     * @param skipFlag
     */
    private void _skipJob(List<SchedulerJobInstance> jobs,  boolean skipFlag, boolean ignoreException) {
        jobs.forEach(schedulerJobInstance -> {
            if(((!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) && (!schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED))) && skipFlag)
                || (!schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED) && !skipFlag)) {
                if(!ignoreException) {
                    throw new ContextMachineException(String.format("Attempting to set skip flag to [%s] on job[%s], " +
                            "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot have the skip flag set."
                        , skipFlag, schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId()
                        , schedulerJobInstance.getStatus()));
                }
                else {
                    return;
                }
            }
            SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId(),
                schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());
            SchedulerJobInstance dbInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            dbInstance.setSkip(skipFlag);

            InstanceStatus previousState = schedulerJobInstance.getStatus();
            schedulerJobInstance.setSkip(skipFlag);
            if(skipFlag) {
                schedulerJobInstance.setStatus(InstanceStatus.SKIPPED);
                dbInstance.setStatus(InstanceStatus.SKIPPED);
                schedulerJobInstanceRecord.setStatus(InstanceStatus.SKIPPED.name());
            }
            else {
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                dbInstance.setStatus(InstanceStatus.WAITING);
                schedulerJobInstanceRecord.setStatus(InstanceStatus.WAITING.name());
            }

            schedulerJobInstanceRecord.setSchedulerJobInstance(dbInstance);
            this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

            this.recursivelySetContextStatus(this.contextInstance, true);
            this.setContextStatus(contextInstance, true);
            this.saveContext();
            logger.info(String.format("Successfully set skip flag to [%s] on job[%s]. Context[%s], Child Context[%s], Context Instance[%s]."
                , skipFlag, schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), schedulerJobInstance.getChildContextName(), this.contextInstance.getId()));

            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));

            if(this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier()
                + "-" + schedulerJobInstance.getChildContextName())) {
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier()
                    + "-" + schedulerJobInstance.getChildContextName()).setSkip(skipFlag);
            }
        });
    }

    /**
     * Helper method to hold a job.
     *
     * @param jobIdentifier
     * @param childContextName
     */
    public void holdJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + childContextName) &&
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-" + childContextName).isTargetResidingContextOnly()) {
                // if a job is targeting a specific context, we only hold it for that context!
                this._holdJob(List.of(schedulerJobInstance));
            } else {
                List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, jobIdentifier);
                this._holdJob(jobInstances);
            }
        }
        else {
            throw new ContextMachineException(String.format("Attempting to hold job[%s], however this job does not " +
                "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     * Do the heavy lifting for holding a job!
     *
     * @param jobs
     */
    private void _holdJob(List<SchedulerJobInstance> jobs) {
        jobs.forEach(schedulerJobInstance -> {
            if(!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) &&
                !schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED)) {
                throw new ContextMachineException(String.format("Attempting to hold job[%s], " +
                        "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be put on hold."
                    , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
            }
            InstanceStatus previousState = schedulerJobInstance.getStatus();
            schedulerJobInstance.setHeld(true);
            schedulerJobInstance.setStatus(InstanceStatus.ON_HOLD);
            schedulerJobInstance.setContextInstanceId(this.contextInstance.getId());

            SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId(),
                schedulerJobInstance.getJobName(), schedulerJobInstance.getChildContextName());
            SchedulerJobInstance dbInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
            dbInstance.setHeld(true);
            dbInstance.setStatus(InstanceStatus.ON_HOLD);
            schedulerJobInstanceRecord.setSchedulerJobInstance(dbInstance);
            this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

            this.saveContext();
            logger.info(String.format("Successfully held job[%s]. Context[%s], Context Instance[%s]."
                , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId()));

            jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                , previousState, schedulerJobInstance.getStatus()));
        });
    }


    public void resetJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + childContextName) &&
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-" + childContextName).isTargetResidingContextOnly()) {
                // if a job is targeting a specific context, we only release it for that context!
                this._resetJob(List.of(schedulerJobInstance));
            } else {
                List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, jobIdentifier);
                this._resetJob(jobInstances);
            }
        }
        else {
            throw new ContextMachineException(String.format("Attempting to reset job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    private void _resetJob(List<SchedulerJobInstance> jobs) {
        jobs.forEach(schedulerJobInstance -> {
            if(schedulerJobInstance.getChildContextName() == null) return;
            if (schedulerJobInstance != null) {
                if (!schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE) &&
                    !schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR) &&
                    !schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
                    throw new ContextMachineException(String.format("Attempting to reset job[%s], " +
                            "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be reset."
                        , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
                }
                InstanceStatus previousState = schedulerJobInstance.getStatus();
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                schedulerJobInstance.setInitiationEventRaised(false);
                schedulerJobInstance.setErrorAcknowledged(false);


                this.saveContext();
                logger.info(String.format("Successfully reset job[%s]. Context[%s], Context Instance[%s]."
                    , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId()));

                if(this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName())) {
                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findById(schedulerJobInstance.getJobName()
                        + "_" + schedulerJobInstance.getContextInstanceId()
                        + "_" + schedulerJobInstance.getChildContextName()
                        + "_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
                    if(schedulerJobInstanceRecord != null) {
                        InternalEventDrivenJobInstance instance = (InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
                        instance.setKilled(false);
                        instance.setErrorAcknowledged(false);
                        instance.setErrorAcknowledgeUser(null);
                        instance.setErrorAcknowledgeTimestamp(0L);
                        instance.setStatus(InstanceStatus.WAITING);
                        schedulerJobInstanceRecord.setSchedulerJobInstance(instance);
                        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                        jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(instance, this.contextInstance
                            , previousState, schedulerJobInstance.getStatus()));
                    }
                }
                if(this.bridgingJobInstanceMap.containsKey(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName())) {
                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findById(schedulerJobInstance.getJobName()
                        + "_" + schedulerJobInstance.getContextInstanceId()
                        + "_" + schedulerJobInstance.getChildContextName()
                        + "_" + JobConstants.BRIDGING_JOB_INSTANCE);
                    if(schedulerJobInstanceRecord != null) {
                        BridgingJobInstance instance = (BridgingJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
                        instance.setStatus(InstanceStatus.WAITING);
                        schedulerJobInstanceRecord.setSchedulerJobInstance(instance);
                        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                        jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(instance, this.contextInstance
                            , previousState, schedulerJobInstance.getStatus()));
                    }
                }
            } else {
                throw new ContextMachineException(String.format("Attempting to reset job[%s], however this job does not " +
                        "appear in context[%s] with instance id[%s], or any of its nested contexts."
                    , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), this.contextInstance.getId()));
            }
        });

        if(!jobs.isEmpty()) {
            this.recursivelySetContextStatus(this.contextInstance, true);
            this.contextInstance.setStatus(InstanceStatus.WAITING);
            this.setContextStatus(this.contextInstance, true);
            this.saveContext();
        }
    }

    public void releaseJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + childContextName) &&
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-" + childContextName).isTargetResidingContextOnly()) {
                // if a job is targeting a specific context, we only release it for that context!
                this._releaseJob(List.of(schedulerJobInstance));
            } else {
                List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, jobIdentifier);
                this._releaseJob(jobInstances);
            }
        }
        else {
            throw new ContextMachineException(String.format("Attempting to hold job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     *
     * @param jobs
     */
    private void _releaseJob(List<SchedulerJobInstance> jobs) {
        jobs.forEach(schedulerJobInstance -> {
            if(schedulerJobInstance.getChildContextName() == null) return;
            SchedulerJobInitiationEvent event = this.contextInstance.getHeldJobs().get(schedulerJobInstance.getIdentifier() + "_" + schedulerJobInstance.getChildContextName());
            if(event != null) {
                InternalEventDrivenJobInstance instance = this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier());
                if(instance != null && instance.isTargetResidingContextOnly()) {
                    event.getChildContextNames().clear();
                    event.getChildContextNames().add(schedulerJobInstance.getChildContextName());
                }
                this.contextInstance.getHeldJobs().remove(schedulerJobInstance.getIdentifier()
                    + "_" + schedulerJobInstance.getChildContextName());

                BigQueueMessage bigQueueMessage
                    = new BigQueueMessageBuilder<>()
                    .withMessage(event)
                    .withMessageProperties(Map.of("contextName", this.context.getName(),
                        CONTEXT_INSTANCE_ID, this.contextInstance.getId()))
                    .build();

                try {
                    String serialised = objectMapper.writeValueAsString(bigQueueMessage);
                    logger.debug("Enqueue job initiation event: " + serialised);
                    outboundQueue.enqueue(serialised.getBytes());
                    logger.debug("Outbound queue size: " + outboundQueue.size());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ContextMachineException(String.format("Attempting to release job[%s] currently held in context[%s] " +
                            "with instance id[%s]. Could not enqueue the outbound job initiation event!", schedulerJobInstance.getChildContextName()
                        , this.contextInstance.getName(), this.contextInstance.getId()));
                }

                InstanceStatus previousState = schedulerJobInstance.getStatus();
                schedulerJobInstance.setHeld(false);
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                schedulerJobInstance.setContextInstanceId(this.contextInstance.getId());
                this.saveContext();
                logger.info(String.format("Successfully released job[%s]. Context[%s], ChildContext[%s] Context Instance[%s]."
                    , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), schedulerJobInstance.getChildContextName(), this.contextInstance.getId()));
                jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, this.contextInstance
                    , previousState, schedulerJobInstance.getStatus()));
            }
            else {
                if(schedulerJobInstance != null) {
                    if(schedulerJobInstance.getStatus().equals(InstanceStatus.RUNNING)) return;
                    if(!schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD) && !schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
                        throw new ContextMachineException(String.format("Attempting to release job[%s], " +
                                "in context[%s], childContext[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be released."
                            , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), schedulerJobInstance.getChildContextName()
                            , this.contextInstance.getId(), schedulerJobInstance.getStatus()));
                    }
                    InstanceStatus previousState = schedulerJobInstance.getStatus();
                    schedulerJobInstance.setHeld(false);
                    schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                    schedulerJobInstance.setContextInstanceId(this.contextInstance.getId());
                    this.saveContext();
                    logger.info(String.format("Successfully released job[%s]. Context[%s], ChildContext[%s], Context Instance[%s]."
                        , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), schedulerJobInstance.getChildContextName(), this.contextInstance.getId()));
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
                        , schedulerJobInstance.getChildContextName(), this.contextInstance.getName(), this.contextInstance.getId(), heldJobsString));
                }
            }
        });
    }

    /**
     * Acknowledge any errors encountered while processing a scheduler job.
     * If the scheduler job instance is targeting the residing only in the target context, the error is acknowledged in that context.
     * If the scheduler job instance is residing in multiple child contexts, the error is acknowledged in each child context.
     *
     * @param schedulerJobInstance the instance of the scheduler job where the error occurred
     */
    public void acknowledgeSchedulerJobError(InternalEventDrivenJobInstance schedulerJobInstance) {
        if(schedulerJobInstance.isTargetResidingContextOnly()) {
            ContextInstance child = ContextHelper.getChildContextInstance(schedulerJobInstance.getChildContextName(), this.contextInstance);
            this.acknowledgeSchedulerJobError(child, schedulerJobInstance.getIdentifier());
        }
        else {
            schedulerJobInstance.getChildContextNames().forEach(name -> {
                ContextInstance child = ContextHelper.getChildContextInstance(name, this.contextInstance);
                this.acknowledgeSchedulerJobError(child, schedulerJobInstance.getIdentifier());
            });
        }

        this.recursivelySetContextStatus(this.contextInstance, true);
        this.contextInstance.setStatus(InstanceStatus.WAITING);
        this.setContextStatus(this.contextInstance, true);
        this.saveContext();
    }

    /**
     * Acknowledges an error in a scheduler job.
     *
     * @param child a ContextInstance object representing the context in which the scheduler job is running
     * @param identifier the identifier of the scheduler job that encountered the error
     */
    private void acknowledgeSchedulerJobError(ContextInstance child, String identifier) {
        SchedulerJobInstance schedulerJobInstance = child.getScheduledJobsMap().get(identifier);
        if(schedulerJobInstance != null) {
            schedulerJobInstance.setErrorAcknowledged(true);
        }
    }

    /**
     * Kills all currently running jobs within the context instance.
     */
    public void killRunningJobs() {
        List<SchedulerJobInstance> runningJobs = this.contextInstance.getAllSchedulerJobInstances().stream()
            .filter(internalEventDrivenJobInstance -> internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.RUNNING))
            .collect(Collectors.toList());

        Map<String, ModuleMetaData> agents = new HashMap<>();
        List<Long> killedPids = new ArrayList<>();
        runningJobs.forEach(job -> {
            ModuleMetaData agent = null;
            try {
                if(job.getScheduledProcessEvent() != null &&
                    job.getScheduledProcessEvent().getPid() > 0 &&
                    !killedPids.contains(job.getScheduledProcessEvent().getPid())) {
                    // Let's only query for the agent metadata if necessary.
                    if (!agents.containsKey(job.getAgentName())) {
                        agents.put(job.getAgentName(), this.moduleMetaDataService.findById(job.getAgentName()));
                    }

                    agent = agents.get(job.getAgentName());
                    logger.info(String.format("Killing job[%s] with pid[%s] on agent[%s]."
                        , job.getJobName(), job.getScheduledProcessEvent().getPid(), agent.getUrl()));
                    jobUtilsService.killJob(agent.getUrl(), job.getScheduledProcessEvent().getPid(), true);

                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName(this.contextInstance.getId(),
                        job.getJobName(), job.getChildContextName());
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.KILLED.name());
                    InternalEventDrivenJobInstance dbInstance = (InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
                    dbInstance.setStatus(InstanceStatus.KILLED);
                    dbInstance.setKilled(true);

                    schedulerJobInstanceRecord.setSchedulerJobInstance(dbInstance);
                    this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                    jobLogicMachine.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(dbInstance, this.contextInstance
                        , InstanceStatus.RUNNING, InstanceStatus.ERROR));

                    killedPids.add(job.getScheduledProcessEvent().getPid());
                }
            }
            catch (Exception e) {
                // We are just going to put a warn message as the job may have already finished
                logger.info(String.format("Failed to kill job[%s] with pid[%s] on agent[%s]. The job may have already ended " +
                        "when the request to kill the job was issued. Error Message: %s"
                    , job.getJobName(), job.getScheduledProcessEvent().getPid(), agent.getUrl(), e.getMessage()));
            }
        });
    }

    /**
     * This method is responsible for providing a view onto jobs that can be run based on the receipt of an input event,
     * without impacting the state of the underlying context data model.
     *
     * @param contextualisedScheduledProcessEvent
     * @return
     */
    public List<SchedulerJobInitiationEvent> getEventsThatCanRun(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) {
        List<InternalEventDrivenJobInstance> instances;
        if(contextualisedScheduledProcessEvent.getInternalEventDrivenJob().isTargetResidingContextOnly()) {
            instances = new ArrayList<>();
            instances.add(contextualisedScheduledProcessEvent.getInternalEventDrivenJob());
        }
        else {
            instances = this.internalEventDrivenJobInstances.values().stream()
                .filter(internalEventDrivenJobInstance -> internalEventDrivenJobInstance.getIdentifier()
                    .equals(contextualisedScheduledProcessEvent.getInternalEventDrivenJob().getIdentifier()))
                .collect(Collectors.toList());
        }

        MutableBoolean lockRaised = new MutableBoolean(false);

        List<SchedulerJobInitiationEvent> events = new ArrayList<>();

        instances.forEach(internalEventDrivenJobInstance -> {
            ContextualisedScheduledProcessEvent event = new ContextualisedScheduledProcessEventImpl();
            event.setJobStarting(false);
            event.setJobName(contextualisedScheduledProcessEvent.getJobName());
            event.setAgentName(contextualisedScheduledProcessEvent.getAgentName());
            event.setContextName(contextualisedScheduledProcessEvent.getContextName());
            event.setInternalEventDrivenJob(internalEventDrivenJobInstance);
            event.setRaisedDueToFailureResubmission(true);

            events.addAll(this.getInitiationEvents(this.contextInstance
                , event, lockRaised, false));
        });

        List<SchedulerJobInitiationEvent> finalEvents = new ArrayList<>();

        events.forEach(event -> {
            if(event.getInternalEventDrivenJob() != null) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, event.getInternalEventDrivenJob().getChildContextName(),
                    event.getInternalEventDrivenJob().getIdentifier());

                if (schedulerJobInstance != null && !schedulerJobInstance.isHeld()) {
                    finalEvents.add(event);
                }
            }
            else {
                // Check if this event is a global event when the internalEventDriveJob is not defined
                SchedulerJobInstance schedulerJobInstance = null;

                // globalEventJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
                // check the jobName in the values within the globalEventJobInstanceMap and if found, it is safe to add the Event to the finalEvents. 
                for (Map.Entry<String, GlobalEventJobInstance> globalEvents : globalEventJobInstanceMap.entrySet()) {
                    if (StringUtils.equals(globalEvents.getValue().getJobName(), event.getJobName())) {
                        schedulerJobInstance = globalEvents.getValue();
                        break;
                    }
                }

                if(schedulerJobInstance == null) {
                    for (Map.Entry<String, ContextTerminalJobInstance> contextTerminalJobInstanceEntry : contextTerminalJobInstanceMap.entrySet()) {
                        if (StringUtils.equals(contextTerminalJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                            schedulerJobInstance = contextTerminalJobInstanceEntry.getValue();
                            break;
                        }
                    }
                }

                if(schedulerJobInstance == null) {
                    for (Map.Entry<String, BridgingJobInstance> contextTerminalJobInstanceEntry : bridgingJobInstanceMap.entrySet()) {
                        if (StringUtils.equals(contextTerminalJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                            schedulerJobInstance = contextTerminalJobInstanceEntry.getValue();
                            break;
                        }
                    }
                }

                if(schedulerJobInstance == null) {
                    for (Map.Entry<String, LocalEventJobInstance> contextTerminalJobInstanceEntry : localEventJobInstanceMap.entrySet()) {
                        if (StringUtils.equals(contextTerminalJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                            schedulerJobInstance = contextTerminalJobInstanceEntry.getValue();
                            break;
                        }
                    }
                }

                if (schedulerJobInstance != null) {
                    finalEvents.add(event);
                } else {
                    logger.warn(String.format("Could not load internal event driven job for initiation event JobName[%s], SchedulerJobInitiationEvent[%s]"
                        , event.getJobName(), event.toString()));
                }
            }
        });

        return finalEvents;
    }

    /**
     * Indicates if this machine is supports a given agent
     * @param agentName to be checked
     * @return true if the contextMachine is used to service the given agent.
     */
    public boolean servesAgent(String agentName) {
        if (agents != null) {
            return agents.keySet().contains(agentName);
        } else {
            return false;
        }
    }


    /**
     * Processes the received event and returns a list of SchedulerJobInitiationEvent.
     *
     * @param scheduledProcessEvent The received ContextualisedScheduledProcessEvent to process.
     *
     * @return A list of SchedulerJobInitiationEvent after processing the event.
     */
    protected List<SchedulerJobInitiationEvent> eventReceived(ContextualisedScheduledProcessEvent scheduledProcessEvent) {
        logger.info("Context Machine Received Event [{}]", scheduledProcessEvent);

        ContextInstance previousContextInstance = this.contextInstance;

        MutableBoolean lockRaised = new MutableBoolean(false);

        List<SchedulerJobInitiationEvent> events = new ArrayList<>();
        if(scheduledProcessEvent.getJobGroup() != null &&
            !scheduledProcessEvent.getJobGroup().equals(MANUAL_SUBMISSION) &&
            this.contextInstance.isQuartzScheduleDrivenJobsDisabledForContext() &&
            this.quartzScheduleDrivenJobInstanceMap.containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {
            logger.info("Ignoring quartz scheduled job [{}] for context [{}] with instance id [{}]. Quartz based scheduler jobs" +
                " are ignored for this context.", scheduledProcessEvent.getJobName(), this.contextInstance.getName(), this.contextInstance.getId());
        }
        else {
            events = this.getInitiationEvents(this.contextInstance, scheduledProcessEvent, lockRaised, true);
        }

        List<SchedulerJobInitiationEvent> finalEvents = new ArrayList<>();

        events.forEach(event -> {
            if(event.getInternalEventDrivenJob() != null) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, event.getInternalEventDrivenJob().getChildContextName(),
                    event.getInternalEventDrivenJob().getIdentifier());

                if (schedulerJobInstance != null && schedulerJobInstance.isHeld()) {
                    this.contextInstance.getHeldJobs().put(schedulerJobInstance.getIdentifier()
                        + "_" + event.getInternalEventDrivenJob().getChildContextName(), event);
                } else {
                    finalEvents.add(event);
                }
            }
            else {
                this.addFinalGlobalEvents(event, scheduledProcessEvent, finalEvents);
                this.addFinalContextStartEvents(event,scheduledProcessEvent, finalEvents);
                this.addFinalContextTerminalEvents(event, scheduledProcessEvent, finalEvents);
                this.addFinalLocalEvents(event, scheduledProcessEvent, finalEvents);
                this.addFinalBridgingEvents(event, scheduledProcessEvent, finalEvents);
            }
        });

        saveInstanceAuditRecord(scheduledProcessEvent, finalEvents, previousContextInstance, this.contextInstance);

        this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl(this.contextInstance.getId(),
            this.contextInstance, this.contextInstance.getStatus(), this.contextInstance.getStatus()));
        return finalEvents;
    }

    /**
     * Adds the given SchedulerJobInitiationEvent to the finalEvents list if the event's jobName matches the jobName of any
     * GlobalEventJobInstance in the globalEventJobInstanceMap.
     *
     * @param event                   The SchedulerJobInitiationEvent to be added.
     * @param scheduledProcessEvent  The ContextualisedScheduledProcessEvent associated with the event.
     * @param finalEvents             The list of SchedulerJobInitiationEvents to add the event to.
     */
    private void addFinalGlobalEvents(SchedulerJobInitiationEvent event, ContextualisedScheduledProcessEvent scheduledProcessEvent,
                     List<SchedulerJobInitiationEvent> finalEvents) {
        GlobalEventJobInstance globalEventJobInstance = null;

        // globalEventJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the globalEventJobInstanceMap and if found, it is safe to add the Event to the finalEvents.
        for (Map.Entry<String, GlobalEventJobInstance> globalEvents : globalEventJobInstanceMap.entrySet()) {
            if (StringUtils.equals(globalEvents.getValue().getJobName(), event.getJobName())) {
                globalEventJobInstance = globalEvents.getValue();
                globalEventJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
                break;
            }
        }

        if (globalEventJobInstance != null) {
            finalEvents.add(event);
        }
    }

    /**
     * Adds the SchedulerJobInitiationEvent to the finalEvents list if the jobName of the event matches
     * the jobName of any contextStartJobInstance in the contextStartJobInstanceMap.
     *
     * @param event The SchedulerJobInitiationEvent to be checked and potentially added to the finalEvents list.
     * @param scheduledProcessEvent The ContextualisedScheduledProcessEvent associated with the event.
     * @param finalEvents The list to which the event will be added if a match is found.
     */
    private void addFinalContextStartEvents(SchedulerJobInitiationEvent event, ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                      List<SchedulerJobInitiationEvent> finalEvents) {
        ContextStartJobInstance contextStartJobInstance = null;

        // contextStartJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the contextStartJobInstanceMap and if found, it is safe to add the Event to the finalEvents.
        for (Map.Entry<String, ContextStartJobInstance> contextStartJobInstanceEntry : this.contextStartJobInstanceMap.entrySet()) {
            if (StringUtils.equals(contextStartJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                contextStartJobInstance = contextStartJobInstanceEntry.getValue();
                contextStartJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
                break;
            }
        }

        if (contextStartJobInstance != null) {
            finalEvents.add(event);
        }
    }

    /**
     * Adds the given SchedulerJobInitiationEvent to the list of final events if the event's job name matches
     * the job name associated with a ContextTerminalJobInstance in the contextTerminalJobInstanceMap.
     *
     * @param event The SchedulerJobInitiationEvent to be added to the list
     * @param scheduledProcessEvent The ContextualisedScheduledProcessEvent associated with the event
     * @param finalEvents The list of final events to which the event will be added
     */
    private void addFinalContextTerminalEvents(SchedulerJobInitiationEvent event, ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                            List<SchedulerJobInitiationEvent> finalEvents) {
        ContextTerminalJobInstance contextStartJobInstance = null;

        // contextTerminalJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the contextTerminalJobInstanceMap and if found, it is safe to add the Event to the finalEvents.
        for (Map.Entry<String, ContextTerminalJobInstance> contextStartJobInstanceEntry : this.contextTerminalJobInstanceMap.entrySet()) {
            if (StringUtils.equals(contextStartJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                contextStartJobInstance = contextStartJobInstanceEntry.getValue();
                contextStartJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
                break;
            }
        }

        if (contextStartJobInstance != null) {
            finalEvents.add(event);
        }
    }

    /**
     * Adds the given SchedulerJobInitiationEvent to the list of final events if the event's job name matches
     * the job name associated with a LocalEventJobInstance in the localEventJobInstanceMap.
     *
     * @param event The SchedulerJobInitiationEvent to be added to the list
     * @param scheduledProcessEvent The ContextualisedScheduledProcessEvent associated with the event
     * @param finalEvents The list of final events to which the event will be added
     */
    private void addFinalLocalEvents(SchedulerJobInitiationEvent event, ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                               List<SchedulerJobInitiationEvent> finalEvents) {
        LocalEventJobInstance localEventJobInstance = null;

        // localEventJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the localEventJobInstanceMap and if found, it is safe to add the Event to the finalEvents.
        for (Map.Entry<String, LocalEventJobInstance> contextStartJobInstanceEntry : this.localEventJobInstanceMap.entrySet()) {
            if (StringUtils.equals(contextStartJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                localEventJobInstance = contextStartJobInstanceEntry.getValue();
                localEventJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
                break;
            }
        }

        if (localEventJobInstance != null) {
            finalEvents.add(event);
        }
    }

    private void addFinalBridgingEvents(SchedulerJobInitiationEvent event, ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                     List<SchedulerJobInitiationEvent> finalEvents) {
        BridgingJobInstance bridgingJobInstance = null;

        // bridgingJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the bridgingJobInstanceMap and if found, it is safe to add the Event to the finalEvents.
        for (Map.Entry<String, BridgingJobInstance> contextStartJobInstanceEntry : this.bridgingJobInstanceMap.entrySet()) {
            if (StringUtils.equals(contextStartJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                bridgingJobInstance = contextStartJobInstanceEntry.getValue();
                bridgingJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
                break;
            }
        }

        if (bridgingJobInstance != null) {
            finalEvents.add(event);
        }
    }

    /**
     * Saves the audit record for a scheduled process event.
     *
     * @param scheduledProcessEvent The contextualized scheduled process event.
     * @param finalEvents The list of scheduler job initiation events.
     * @param previousContextInstance The previous context instance.
     * @param updatedContextInstance The updated context instance.
     */
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
        if(scheduledProcessEvent.getInternalEventDrivenJob() != null) {
            if(scheduledProcessEvent.isJobStarting() == false) {
                if(scheduledProcessEvent.isSuccessful()) {
                    auditRecord.setStatus(InstanceStatus.COMPLETE.name());
                }
                else {
                    auditRecord.setStatus(InstanceStatus.ERROR.name());
                }
            }
            else {
                auditRecord.setStatus(InstanceStatus.RUNNING.name());
            }
            auditRecord.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            auditRecord.setRepeatingJob(scheduledProcessEvent.getInternalEventDrivenJob().isJobRepeatable());
        }
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

        this.getInitiationEvents(results, contextInstance, scheduledProcessEvent, lockRaised, markAsRaised);

        return results;
    }

    /**
     * Retrieves the SchedulerJobInitiationEvents for a given ContextInstance and ContextualisedScheduledProcessEvent.
     *
     * @param results The list to which the SchedulerJobInitiationEvents should be added.
     * @param contextInstance The ContextInstance for which the SchedulerJobInitiationEvents are retrieved.
     * @param scheduledProcessEvent The ContextualisedScheduledProcessEvent for which the SchedulerJobInitiationEvents are retrieved.
     * @param lockRaised Indicates if the lock was raised for the job.
     * @param markAsRaised Indicates if the events should be marked as raised.
     */
    private void getInitiationEvents(List<SchedulerJobInitiationEvent> results, ContextInstance contextInstance, ContextualisedScheduledProcessEvent scheduledProcessEvent
        , MutableBoolean lockRaised, boolean markAsRaised) {

        if(contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
            + "-" + scheduledProcessEvent.getJobName())) {

            if(scheduledProcessEvent.getInternalEventDrivenJob() != null && scheduledProcessEvent.getInternalEventDrivenJob().isTargetResidingContextOnly()
                && !scheduledProcessEvent.getInternalEventDrivenJob().getChildContextName().equals(contextInstance.getName())) {
                // do nothing if targeted job not relevant for its targeted context.
            }
            else {
                // Delegate to the JobLogicMachine to determine if any SchedulerJobInitiationEvents are
                // required to be raised.
                List<SchedulerJobInitiationEvent> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                    , contextInstance, this.dryRunParameters, this.globalEventJobInstanceMap, this.internalEventDrivenJobInstances
                    , this.contextStartJobInstanceMap, this.contextTerminalJobInstanceMap, this.localEventJobInstanceMap, this.bridgingJobInstanceMap
                    , this.contextInstance.getContextParameters(), this.contextInstance, lockRaised, markAsRaised);

                // Update the context status after event received and attached
                // to the job instance.
                this.setContextStatus(contextInstance, false);
                results.addAll(events);
            }
        }


        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(ContextInstance instance: contextInstance.getContexts()) {
                // Recursively work our way through all nested contexts to determine if any job initiation events need to be raised.
                results.addAll(this.getInitiationEvents(instance, scheduledProcessEvent,lockRaised, markAsRaised));
                this.setContextStatus(contextInstance, false);
            }
        }
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

    private void recursivelySetContextStatus(ContextInstance contextInstance, boolean broadcastStateChange) {
        if(contextInstance.getContexts() != null) {
            contextInstance.getContexts().forEach(context -> {
                this.recursivelySetContextStatus(context, broadcastStateChange);
                this.setContextStatus(context, broadcastStateChange);
            });
        }
    }

    /**
     * Helper method to set the context status.
     *
     * @param contextInstance
     */
    private void setContextStatus(ContextInstance contextInstance, boolean broadcastStateChange) {
        AtomicBoolean allJobsComplete = new AtomicBoolean(true);
        AtomicBoolean allLogicSatisfied = new AtomicBoolean(true);
        AtomicBoolean anyRunningOrCompletedOrQueuedJobs = new AtomicBoolean(false);
        AtomicBoolean anyErrorJobs = new AtomicBoolean(false);
        AtomicBoolean allContextsComplete = new AtomicBoolean(true);
        AtomicBoolean anyRunningOrCompletedContexts = new AtomicBoolean(false);
        AtomicBoolean anyErrorContexts = new AtomicBoolean(false);

        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
            // Get all jobs that are not part of a logical construct
            Map<String, SchedulerJob> jobsOutsideLogicConstructs
                = ContextHelper.getJobsOutsideLogicalGrouping(contextInstance);

            // Confirm that all jobs outside logical constructs are complete.
            jobsOutsideLogicConstructs.entrySet().forEach(entry -> {
                if(((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.COMPLETE) ||
                    ((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.SKIPPED) ||
                    ((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) return;

                if (!((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.COMPLETE)
                    && !((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.SKIPPED)
                    && !((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)
                    && !((SchedulerJobInstance)entry.getValue()).getStatus().equals(InstanceStatus.ERROR) &&
                        (((SchedulerJobInstance)entry.getValue()).isErrorAcknowledged() == null) ||
                            ((SchedulerJobInstance)entry.getValue()).isErrorAcknowledged() != null
                                && !((SchedulerJobInstance)entry.getValue()).isErrorAcknowledged()) {
                    allJobsComplete.set(false);
                }
            });

            // Confirm that all logical constructs have been satisfied. We do not want to include
            // jobs whose execution originated from outside the context.
            Map<String, SchedulerJobInstance> deepCopy = contextInstance.getScheduledJobsMap().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> SerializationUtils.clone(e.getValue())));
            deepCopy.values().forEach(schedulerJobInstance -> {
                if(this.internalEventDrivenJobInstances != null) {
                    List<SchedulerJobInstance> precedingJobs = ContextHelper.getPrecedingJobsFromOutsideContext(this.contextInstance, schedulerJobInstance.getJobName(), contextInstance.getName()
                        , this.internalEventDrivenJobInstances.entrySet()
                            .stream()
                            .map(entry -> Map.entry(entry.getKey(), (InternalEventDrivenJob) entry.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                    if (!precedingJobs.isEmpty()) schedulerJobInstance.setStatus(InstanceStatus.COMPLETE);
                }
            });

            allLogicSatisfied.set(this.contextStateHelper.isAllLogicSatisfied
                (contextInstance, deepCopy));

            // Now determine if there running or queued jobs or those
            // in a error state.
            contextInstance.getScheduledJobs().forEach(job -> {
                if(this.internalEventDrivenJobInstances != null) {
                    // We're not interested in assessing the status of jobs that are initiated outside the context.
                    List<ContextTransition> contextTransitions = ContextHelper.determineIfJobsTransitionFromOtherContexts(this.contextInstance, job.getJobName(), contextInstance.getName()
                        , this.internalEventDrivenJobInstances.entrySet()
                            .stream()
                            .map(entry -> Map.entry(entry.getKey(), (InternalEventDrivenJob) entry.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                    if (!contextTransitions.isEmpty()) return;
                }

                if (job.getStatus().equals(InstanceStatus.RUNNING)
                    || job.getStatus().equals(InstanceStatus.COMPLETE)
                    || job.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
                    anyRunningOrCompletedOrQueuedJobs.set(true);
                }

                if (job.getStatus().equals(InstanceStatus.ERROR)) {
                    if(job.isErrorAcknowledged() == null) {
                        anyErrorJobs.set(true);
                    }
                    else if(job.isErrorAcknowledged() != null
                        && !job.isErrorAcknowledged()) {
                        anyErrorJobs.set(true);
                    }
                    else if(job.isErrorAcknowledged() != null
                        && job.isErrorAcknowledged()) {
                        anyRunningOrCompletedContexts.set(true);
                    }
                }
            });
        }

        // Determine the state of all nested context instances.
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

        // Armed with the information acquired above, determine the state of
        // the current context instance.
        InstanceStatus previousStatus = contextInstance.getStatus();

        if (anyErrorJobs.get() || anyErrorContexts.get()) {
            contextInstance.setStatus(InstanceStatus.ERROR);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        } else if(allJobsComplete.get() && allContextsComplete.get() && allLogicSatisfied.get()) {
            contextInstance.setStatus(InstanceStatus.COMPLETE);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        } else if(anyRunningOrCompletedOrQueuedJobs.get() || anyRunningOrCompletedContexts.get()) {
            contextInstance.setStatus(InstanceStatus.RUNNING);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        }
        else {
            contextInstance.setStatus(InstanceStatus.WAITING);
            contextInstance.setUpdatedDateTime(System.currentTimeMillis());
        }

        InstanceStatus newStatus = contextInstance.getStatus();

        // If the context instance has had as state change, notify all interested parties.
        if(!previousStatus.equals(newStatus) || broadcastStateChange) {
            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl(this.contextInstance.getId()
                , contextInstance, previousStatus, newStatus));
        }
    }

    public void addQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event) {
        ContextInstance childContextInstance = ContextHelper.getChildContextInstance(event.getInternalEventDrivenJob().getChildContextName()
            , this.contextInstance);

        this.jobLogicMachine.addQueuedSchedulerJobInitiationEvent(childContextInstance, this.contextInstance
            , event.getInternalEventDrivenJob().getIdentifier(), event);

        this.setContextStatus(childContextInstance, false);

        this.saveContext();
    }

    private void issueContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event) {
        if(!event.getPreviousStatus().equals(event.getNewStatus())) {
            logger.debug("Issuing context instance state change event: " + event.getContextInstance().getName() + " " + event.getNewStatus());
            this.statusListenerExecutor.submit(() -> this.contextInstanceStateChangeEventListeners
                .forEach(listener -> listener.onContextInstanceStateChangeEvent(event)));
        }
    }

    public void saveContext() {
        ScheduledContextInstanceRecord scheduledContextInstanceRecord
            = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(this.contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(this.contextInstance);
        scheduledContextInstanceRecord.setTimestamp(this.contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(this.contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    private SchedulerJobInstance getSchedulerJob(ContextInstance contextInstance, String childContextName, String jobIdentifier) {
        if(contextInstance.getScheduledJobsMap() != null && contextInstance.getScheduledJobsMap().containsKey(jobIdentifier)
            && contextInstance.getName().equals(childContextName)) {
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

    private List<SchedulerJobInstance> getSchedulerJobs(ContextInstance contextInstance, String jobIdentifier) {
        List<SchedulerJobInstance> results = new ArrayList<>();

        this.getSchedulerJobs(contextInstance, jobIdentifier, results);

        return results;
    }

    private void getSchedulerJobs(ContextInstance contextInstance, String jobIdentifier, List<SchedulerJobInstance> results) {
        if(contextInstance.getScheduledJobsMap() != null && contextInstance.getScheduledJobsMap().containsKey(jobIdentifier)) {
            results.add(contextInstance.getScheduledJobsMap().get(jobIdentifier));
        }

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            for(ContextInstance contextInstance1: contextInstance.getContexts()) {
                this.getSchedulerJobs(contextInstance1, jobIdentifier, results);
            }
        }
    }

    /**
     * Helper method to broadcast global events to all running instances of a context optionally within an
     * environment.
     *
     * @param schedulerJobInitiationEvent
     * @param ignoreEnvironmentGroup set to true to target all active context instance regardless of what group it belongs to.
     * @param forceSending set to true to override the checking of globalEventJobInstance, used when event comes from outside the current context machine
     * @throws IOException
     */
    public void broadcastGlobalEvents(SchedulerJobInitiationEvent schedulerJobInitiationEvent, boolean ignoreEnvironmentGroup, boolean forceSending) throws IOException {
        /* This block of code will Orchestrate when the event is a Global Event. This will create a
         * ContextualisedScheduledProcessEvent for the global event and send it to all active Contexts available in the
         * ContextMachineCache for a given environment group. This event will be set to Success. */

        // Check if this event is a global event when the internalEventDriveJob is not defined
        GlobalEventJobInstance globalEventJobInstance = null;

        // globalEventJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the globalEventJobInstanceMap and if found then allow us to create the Events for
        // all the available context instance running.
        if (globalEventJobInstanceMap != null && globalEventJobInstanceMap.size() != 0) {
            for (Map.Entry<String, GlobalEventJobInstance> globalEvents : globalEventJobInstanceMap.entrySet()) {
                if (StringUtils.equals(globalEvents.getValue().getJobName(), schedulerJobInitiationEvent.getJobName())) {
                    globalEventJobInstance = globalEvents.getValue();
                    break;
                }
            }
        }

        // Only attempt to send the global event to other context instance if the global event exist in this context, or if "forceSending" is set to true.
        if (globalEventJobInstance != null  || forceSending) {
            logger.info("Job [{}] is a Global Event Job - Do not send to the agent [{}] and attempt to send to all Active Contexts by Environment Group",
                schedulerJobInitiationEvent.getJobName(), schedulerJobInitiationEvent.getAgentUrl());
            logger.info("[{}] Context is part of the EnvironmentGroup [{}]. ignoreEnvironmentGroup is set to [{}]. " +
                    "Will send to Contexts with the same Environment Group if ignoreEnvironmentGroup = false",
                context.getName(), context.getEnvironmentGroup(), ignoreEnvironmentGroup);

            // Get all active context instances for the given environment group. If ignoreEnvironmentGroup is true, then get everything running
            List<String> contextInstanceInContextMachineCache =
                ContextMachineCache.instance().getListOfContextInstanceIdByEnvironmentGroup(context.getEnvironmentGroup(), ignoreEnvironmentGroup);

            // Search each context instance if the Global Event also exist. If it does, create an event for it.
            for(String contextInstanceIdFromCache : contextInstanceInContextMachineCache) {
                ContextMachine contextMachineFromCache = ContextMachineCache.instance().getByContextInstanceId(contextInstanceIdFromCache);
                if(contextMachineFromCache == null) {
                    logger.warn("Unable to find the ContextMachine for the instance [{}] in the cache, skipping sending the Global Event [{}] to it",
                        contextInstanceIdFromCache, schedulerJobInitiationEvent.getJobName());
                    continue;
                }

                if(contextMachineFromCache.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                    logger.info("Will not broadcast global event to instance [{}], Global Event [{}]. The instance is currently in a prepared state.",
                        contextInstanceIdFromCache, schedulerJobInitiationEvent.getJobName());
                    continue;
                }

                // We don't broadcast global events to other contexts if they have been skipped.
                if(schedulerJobInitiationEvent.isSkipped()
                    && !schedulerJobInitiationEvent.getContextInstanceId().equals(contextInstanceIdFromCache)) {
                    continue;
                }

                // Global Job found for this context instance, build the event.
                ContextualisedScheduledProcessEvent globalContextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
                globalContextualisedScheduledProcessEvent.setAgentName(JobConstants.GLOBAL_EVENT);
                globalContextualisedScheduledProcessEvent.setJobName(schedulerJobInitiationEvent.getJobName());
                globalContextualisedScheduledProcessEvent.setSuccessful(true);
                globalContextualisedScheduledProcessEvent.setFireTime(System.currentTimeMillis());
                globalContextualisedScheduledProcessEvent.setContextName(schedulerJobInitiationEvent.getContextName());
                globalContextualisedScheduledProcessEvent.setContextInstanceId(contextInstanceIdFromCache);
                globalContextualisedScheduledProcessEvent.setJobStarting(false);
                globalContextualisedScheduledProcessEvent.setSkipped(schedulerJobInitiationEvent.isSkipped());
                globalContextualisedScheduledProcessEvent.setCatalystEvent(schedulerJobInitiationEvent.getCatalystEvent());
                //No need to set the childContextNames property in ContextualisedScheduledProcessEvent as JobLogicMachine method getJobInitiationEvents should handle it.

                // Event object to JSON and then build the BigQueue message
                String globalContextualisedScheduledProcessEventJson = objectMapper.writeValueAsString(globalContextualisedScheduledProcessEvent);
                BigQueueMessage<String> outgoingBigQueueMessage
                    = new BigQueueMessageBuilder<String>().withMessage(globalContextualisedScheduledProcessEventJson)
                    .withMessageProperties(
                        Map.of("contextName", contextMachineFromCache.getContext().getName(),
                            CONTEXT_INSTANCE_ID, contextMachineFromCache.getContext().getId()))
                    .build();

                // BigQueue message to JSON
                String jsonString = objectMapper.writeValueAsString(outgoingBigQueueMessage);

                // Send the Event to the Context Machine
                contextMachineFromCache.eventReceived(jsonString);
                logger.info("Sending Global Event [{}] to the ContextMachine [{}][{}]", schedulerJobInitiationEvent.getJobName(),
                    contextMachineFromCache.getContext().getName(), contextMachineFromCache.getContext().getId());
            }
        }
    }

    public void broadcastLocalEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) throws IOException {
        /* This block of code will Orchestrate when the event is a Global Event. This will create a
         * ContextualisedScheduledProcessEvent for the global event and send it to all active Contexts available in the
         * ContextMachineCache for a given environment group. This event will be set to Success. */

        // Check if this event is a global event when the internalEventDriveJob is not defined
        SchedulerJobInstance schedulerJobInstance = null;

        // globalEventJobInstanceMap has a key of (JobIdentifier-ContextName) - this may not be available on the event, therefore
        // check the jobName in the values within the globalEventJobInstanceMap and if found then allow us to create the Events for
        // all the available context instance running.
        if (this.contextStartJobInstanceMap != null && !this.contextStartJobInstanceMap.isEmpty()) {
            for (Map.Entry<String, ContextStartJobInstance> startJobInstanceEntry : contextStartJobInstanceMap.entrySet()) {
                if (StringUtils.equals(startJobInstanceEntry.getValue().getJobName(), schedulerJobInitiationEvent.getJobName())) {
                    schedulerJobInstance = startJobInstanceEntry.getValue();
                    break;
                }
            }
        }

        if(schedulerJobInstance == null) {
            if (this.contextTerminalJobInstanceMap != null && !this.contextTerminalJobInstanceMap.isEmpty()) {
                for (Map.Entry<String, ContextTerminalJobInstance> stringContextTerminalJobInstanceEntry : contextTerminalJobInstanceMap.entrySet()) {
                    if (StringUtils.equals(stringContextTerminalJobInstanceEntry.getValue().getJobName(), schedulerJobInitiationEvent.getJobName())) {
                        schedulerJobInstance = stringContextTerminalJobInstanceEntry.getValue();
                        break;
                    }
                }
            }
        }

        if(schedulerJobInstance == null) {
            if (this.localEventJobInstanceMap != null && !this.localEventJobInstanceMap.isEmpty()) {
                for (Map.Entry<String, LocalEventJobInstance> stringLocalEventJobInstanceEntry : localEventJobInstanceMap.entrySet()) {
                    if (StringUtils.equals(stringLocalEventJobInstanceEntry.getValue().getJobName(), schedulerJobInitiationEvent.getJobName())) {
                        schedulerJobInstance = stringLocalEventJobInstanceEntry.getValue();
                        break;
                    }
                }
            }
        }

        if(schedulerJobInstance == null) {
            if (this.bridgingJobInstanceMap != null && !this.bridgingJobInstanceMap.isEmpty()) {
                for (Map.Entry<String, BridgingJobInstance> stringLocalEventJobInstanceEntry : bridgingJobInstanceMap.entrySet()) {
                    if (StringUtils.equals(stringLocalEventJobInstanceEntry.getValue().getJobName(), schedulerJobInitiationEvent.getJobName())) {
                        schedulerJobInstance = stringLocalEventJobInstanceEntry.getValue();
                        break;
                    }
                }
            }
        }

        // Only attempt to send the global event to other context instance if the global event exist in this context, or if "forceSending" is set to true.
        if (schedulerJobInstance != null) {
            // Global Job found for this context instance, build the event.
            ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
            contextualisedScheduledProcessEvent.setAgentName(schedulerJobInitiationEvent.getAgentName());
            contextualisedScheduledProcessEvent.setJobName(schedulerJobInitiationEvent.getJobName());
            contextualisedScheduledProcessEvent.setSuccessful(true);
            contextualisedScheduledProcessEvent.setFireTime(System.currentTimeMillis());
            contextualisedScheduledProcessEvent.setContextName(schedulerJobInitiationEvent.getContextName());
            contextualisedScheduledProcessEvent.setContextInstanceId(this.contextInstance.getId());
            contextualisedScheduledProcessEvent.setJobStarting(false);
            contextualisedScheduledProcessEvent.setSkipped(schedulerJobInitiationEvent.isSkipped());
            contextualisedScheduledProcessEvent.setCatalystEvent(schedulerJobInitiationEvent.getCatalystEvent());
            //No need to set the childContextNames property in ContextualisedScheduledProcessEvent
            //as JobLogicMachine method getJobInitiationEvents should handle it.

            // Event object to JSON and then build the BigQueue message
            String contextualisedScheduledProcessEventJson = objectMapper.writeValueAsString(contextualisedScheduledProcessEvent);
            BigQueueMessage<String> outgoingBigQueueMessage
                = new BigQueueMessageBuilder<String>().withMessage(contextualisedScheduledProcessEventJson)
                .withMessageProperties(
                    Map.of("contextName", this.getContext().getName(),
                        CONTEXT_INSTANCE_ID, this.getContext().getId()))
                .build();

            // BigQueue message to JSON
            String jsonString = objectMapper.writeValueAsString(outgoingBigQueueMessage);

            // Send the Event to the Context Machine
            this.eventReceived(jsonString);
            logger.info("Sending Event [{}] to the ContextMachine [{}][{}]", schedulerJobInitiationEvent.getJobName(),
                this.getContext().getName(), this.getContext().getId());
        }
    }

    protected class InboundQueueMessageRunner implements Runnable {
        private final AtomicBoolean running = new AtomicBoolean(true);

        @Override
        public void run() {
            BigQueueMessage<ContextualisedScheduledProcessEvent> bigQueueMessage = null;
            try {
                if (!this.running.get()) {
                    return;
                }

                byte[] event = inboundQueue.peek();

                if(event == null) {
                    return;
                }

                bigQueueMessage = objectMapper.readValue(event, BigQueueMessageImpl.class);

                ContextualisedScheduledProcessEvent scheduledProcessEvent
                    = objectMapper.readValue(String.valueOf(bigQueueMessage.getMessage()), ContextualisedScheduledProcessEventImpl.class);

                List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = eventReceived(scheduledProcessEvent);

                saveContext();

                for(SchedulerJobInitiationEvent schedulerJobInitiationEvent: schedulerJobInitiationEvents) {

                    if (schedulerJobInitiationEvent.getInternalEventDrivenJob() != null) {
                        BigQueueMessage<SchedulerJobInitiationEvent> outgoingBigQueueMessage
                            = new BigQueueMessageBuilder<SchedulerJobInitiationEvent>().withMessage(schedulerJobInitiationEvent)
                            .withMessageProperties(
                                Map.of("contextName", schedulerJobInitiationEvent.getContextName(),
                                    "contextInstanceId", schedulerJobInitiationEvent.getContextInstanceId()))
                            .build();

                        String serialised = objectMapper.writeValueAsString(outgoingBigQueueMessage);
                        logger.debug("Enqueue job initiation event: " + serialised);
                        outboundQueue.enqueue(serialised.getBytes());
                        logger.debug("Outbound queue size: " + outboundQueue.size());
                    } else {
                        if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.GLOBAL_EVENT)) {
                            broadcastGlobalEvents(schedulerJobInitiationEvent, false, false);
                        }
                        else if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.CONTEXT_START_JOB)) {
                            broadcastLocalEvent(schedulerJobInitiationEvent);
                        }
                        else if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                            broadcastLocalEvent(schedulerJobInitiationEvent);
                        }
                        else if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                            broadcastLocalEvent(schedulerJobInitiationEvent);
                        }
                        else if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
                            broadcastLocalEvent(schedulerJobInitiationEvent);
                        }
                    }
                }

                inboundQueue.dequeue();
                inboundQueue.gc();
            }
            catch (ContextMachineException e) {
                logger.error(String.format("An error has occurred attempting process scheduled process event [%s]"
                    , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);

                // We dequeue context machine exceptions.
                // TODO perhaps we need to park these somewhere similar to excluded events.
                try {
                    inboundQueue.dequeue();
                    inboundQueue.gc();
                    addInboundListener();
                }
                catch (IOException ex) {
                    logger.error(String.format("IOException - An error has occurred attempting to dequeue inbound message [%s]"
                        , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), ex);
                }
            }
            catch (Exception e) {
                logger.error(String.format("Generic Exception - An error has occurred attempting process scheduled process event [%s]"
                    , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);
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
                logger.error(String.format("An error has occurred attempting to raise job initiation event [%s]"
                    , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);
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
                    logger.error(String.format("An error has occurred attempting to enqueue outbound message that is in error [%s]"
                        , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);
                    ex.printStackTrace();
                }
            }
            finally {
                // We only dequeue messages when there has been no exception.
                if(!exception) {
                    try {
                        outboundQueue.dequeue();
                        outboundQueue.gc();
                        logger.debug("Dequeue event: " + bigQueueMessage);
                        logger.debug("Outbound queue size: " + outboundQueue.size());
                    }
                    catch (IOException e) {
                        logger.error(String.format("An error has occurred attempting to dequeue outbound message [%s]"
                            , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);
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
        try {
            inboundListenableFuture = this.inboundQueue.peekAsync();
            this.inboundQueueMessageRunner = new InboundQueueMessageRunner();
            inboundListenableFuture.addListener(inboundQueueMessageRunner, this.contextExecutor);
        }
        catch (Exception e) {
            logger.warn("Could not add inbound listener for context machine. This is likely due to the context instance being ended.");
        }
    }

    protected void addOutboundListener() {
        outboundListenableFuture = outboundQueue.peekAsync();
        this.outboundQueueMessageRunner = new OutboundQueueMessageRunner();
        outboundListenableFuture.addListener(outboundQueueMessageRunner, schedulerInitiatorEventRaisedListenerExecutor);
    }

    protected Map<String, GlobalEventJobInstance> getGlobalEventJobInstanceMap() {
        return globalEventJobInstanceMap;
    }

    public Map<String, InternalEventDrivenJobInstance> getInternalEventDrivenJobInstancesMap() {
        return internalEventDrivenJobInstances;
    }
}
