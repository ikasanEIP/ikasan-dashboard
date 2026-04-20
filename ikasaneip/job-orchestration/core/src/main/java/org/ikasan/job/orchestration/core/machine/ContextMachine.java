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
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
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
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.bigqueue.service.BigQueueDirectoryManagementService;
import org.ikasan.spec.bigqueue.service.BigQueueManagementService;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;

public class ContextMachine {
    private Logger logger = LoggerFactory.getLogger(ContextMachine.class);

    public static final String MANUAL_SUBMISSION = "group (manual fire)";

    private ContextInstance contextInstance;
    private JobLogicMachine jobLogicMachine;
    private ContextInstanceToContextInstanceStatusConverter statusConverter;
    private List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners;
    private List<ContextInstanceDlqEventBroadcastListener> contextInstanceDlqEventBroadcastListeners;
    private ExecutorService statusListenerExecutor;
    private ExecutorService contextInstanceDlqEventListenerExecutor;
    private ExecutorService schedulerInitiatorEventRaisedListenerExecutor;
    private ExecutorService contextExecutor;
    private IBigQueue inboundQueue;
    private IBigQueue outboundQueue;
    private IBigQueue deadLetterQueue;
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
    private ConcurrentHashMap<String, Integer> bigQueueMessageBlacklist;
    private boolean tornDown = false;
    private int executorWaitTimeoutSeconds = 30;
    private int blackListedMessageMaxRetries = 5;

    /**
     * Initializes a new instance of ContextMachine with the provided parameters.
     * @param context The context template to be used.
     * @param contextInstance The context instance to be used.
     * @param scheduledContextInstanceService The service for scheduled context instances.
     * @param globalEventJobInstanceMap A map containing global event job instances.
     * @param quartzScheduleDrivenJobInstanceMap A map containing Quartz schedule driven job instances.
     * @param internalEventDrivenJobInstances A map containing internal event driven job instances.
     * @param contextStartJobInstanceMap A map containing context start job instances.
     * @param contextTerminalJobInstanceMap A map containing context terminal job instances.
     * @param localEventJobInstanceMap A map containing local event job instances.
     * @param bridgingJobInstanceMap A map containing bridging job instances.
     * @param queueDir The directory for the job queue.
     * @param agents A map containing module metadata for agents.
     * @param moduleMetaDataService The service for module metadata.
     * @param jobLockCache The cache for job locks.
     * @param contextParametersInstanceService The service for context parameters instances.
     * @param scheduledContextService The service for scheduled contexts.
     * @param schedulerJobInstanceService The service for scheduler job instances.
     * @param jobLockCacheInitialisationService The service for initializing job lock cache.
     * @param contextInstancePublicationService The service for context instance publication.
     * @param jobUtilsService The service for job utilities.
     */
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
        this.contextInstanceDlqEventBroadcastListeners = new ArrayList<>();
        this.statusListenerExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-StatusChangeListener"));
        this.contextInstanceDlqEventListenerExecutor = Executors
            .newSingleThreadExecutor(new JobThreadFactory("ContextMachine-ContextInstanceDlqEventListener"));
        this.contextExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-ContextExecutor"));
        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newSingleThreadExecutor(new JobThreadFactory("ContextMachine-EventRaisedListener"));
        this.objectMapper = ConcurrentObjectMapperFactory.newInstance();
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
        this.bigQueueMessageBlacklist = new ConcurrentHashMap<>();
    }



    /**
     * Initialize the queues and setup listeners for inbound and outbound messages.
     * This method sets up the inbound and outbound queues using the provided queue directory,
     * creates listeners for both queues, and initializes other required context for the messaging system.
     * It also sets a default number of attempts and maximum wait time for processing messages.
     *
     * @throws IOException if there are errors during initialization of the queues
     */
    public void init() throws IOException {
        String inboundQueueName = this.getInboundQueueName();
        String outboundQueueName = this.getOutboundQueueName();
        String deadLetterQueueName = this.getDeadLetterQueueName();
        this.inboundQueue = new BigQueueImpl(this.queueDir, inboundQueueName);
        this.outboundQueue = new BigQueueImpl(this.queueDir, outboundQueueName);
        this.deadLetterQueue = new BigQueueImpl(this.queueDir, deadLetterQueueName);

        this.addInboundListener();
        this.addOutboundListener();
        this.saveContext();

        this.attempts = 0;
        this.maxWait = 10000L;
    }

    /**
     * Registers the current instance to notification monitors for monitoring.
     * This method will start monitoring the specified instance using MonitorManagement.
     */
    public void registerToNotificationMonitors() {
        MonitorManagement.startMonitoring(this);
    }

    /**
     * Unregisters the current instance from the notification monitors.
     * This method logs an info message about stopping the monitoring for the context and instanceId.
     * It then calls MonitorManagement to stop monitoring for the current instance.
     */
    public void unregisterToNotificationMonitors() {
        logger.info("Call to stop monitoring for the context {} and instanceId {}", this.contextInstance.getName(), this.contextInstance.getId());
        MonitorManagement.stopMonitoring(this);
    }

    /**
     * This method returns the name of the outbound queue based on the context instance ID.
     * The outbound queue name is constructed by appending "-outbound-" followed by the context instance ID and "-queue".
     *
     * @return The name of the outbound queue.
     */
    public String getOutboundQueueName() {
        return "outbound-" + this.contextInstance.getId() + "-queue";
    }

    /**
     * Retrieves the name of the inbound queue for the current context instance.
     *
     * @return The name of the inbound queue in the format "inbound-{contextId}-queue".
     */
    public String getInboundQueueName() {
        return "inbound-" + this.contextInstance.getId() + "-queue";
    }


    /**
     * Retrieves the name of the dead letter queue associated with the context instance.
     *
     * @return The name of the dead letter queue formatted as "dlq-[contextId]-queue".
     */
    public String getDeadLetterQueueName() {
        return "dlq-" + this.contextInstance.getId() + "-queue";
    }

    /**
     * Retrieve the inbound queue associated with this object.
     *
     * @return The inbound queue.
     */
    public IBigQueue getInboundQueue() {
        return this.inboundQueue;
    }

    /**
     * Retrieves the outbound queue stored in the current instance.
     *
     * @return The outbound queue associated with the current instance.
     */
    public IBigQueue getOutboundQueue() {
        return this.outboundQueue;
    }

    /**
     * Retrieves the Dead Letter Queue from this object.
     *
     * @return The Dead Letter Queue associated with this object.
     */
    public IBigQueue getDeadLetterQueue() {
        return deadLetterQueue;
    }

    /**
     * Sets the wait timeout in seconds for the executor.
     *
     * @param executorWaitTimeoutSeconds the wait timeout value in seconds to be set
     */
    public void setExecutorWaitTimeoutSeconds(int executorWaitTimeoutSeconds) {
        if(executorWaitTimeoutSeconds > 0) {
            this.executorWaitTimeoutSeconds = executorWaitTimeoutSeconds;
        }
    }

    /**
     * Sets the maximum number of retries allowed for blacklisted messages.
     *
     * @param blackListedMessageMaxRetries The maximum number of retries allowed for blacklisted messages
     *                                      before considering them as failed.
     */
    public void setBlackListedMessageMaxRetries(int blackListedMessageMaxRetries) {
        if(blackListedMessageMaxRetries > 0) {
            this.blackListedMessageMaxRetries = blackListedMessageMaxRetries;
        }
    }

    /**
     * Helper method to reset the context instance held by the context machine.
     *
     * @throws JsonProcessingException
     */
    public void resetContextInstance(boolean holdCommandJobs, boolean initiateWithSameParameters,
                                     List<ContextParameterInstance> contextParameterInstances)
        throws IOException, SchedulerJobInstanceInitialisationException, BigQueueNotFoundException {
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
            this.releaseQueuedJobs();
            this.killRunningJobs();
            this.teardownBigQueue();
            stopWatch.stop();
            logger.info("Cleaned up old instance running jobs and bigqueue! Elapsed milli: " + stopWatch.getTime());
            stopWatch.reset();
            stopWatch.start();
            ContextService contextService = new ContextService();
            this.context = scheduledContextService.findByName(contextName).getContext();

            if(ContextMachineCache.instance().getAllByContextName(this.context.getName()).stream()
                .filter(cm -> !cm.getContext().getStatus().equals(InstanceStatus.PREPARED))
                .collect(Collectors.toList()).size() == 1) {
                this.jobLockCacheInitialisationService.removeJobLocksFromCache(this.context);
            }

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
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.globalEventJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof GlobalEventJobInstance)
                .map(job -> (GlobalEventJobInstance)job)
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.contextStartJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof ContextStartJobInstance)
                .map(job -> (ContextStartJobInstance)job)
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.contextTerminalJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof ContextTerminalJobInstance)
                .map(job -> (ContextTerminalJobInstance)job)
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.localEventJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof LocalEventJobInstance)
                .map(job -> (LocalEventJobInstance)job)
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            this.bridgingJobInstanceMap  = schedulerJobInstances.stream()
                .filter(job -> job instanceof BridgingJobInstance)
                .map(job -> (BridgingJobInstance)job)
                .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));

            if(holdCommandJobs) {
                ContextHelper.holdAllJobs(this.contextInstance, this.internalEventDrivenJobInstances.entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue())));
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

            this.contextInstance.setStartTime(System.currentTimeMillis());
            this.contextInstance.setProjectedEndTime(CronUtils.getEpochMilliOfPreviousFireTime(this.contextInstance.getTimeWindowStart(), this.contextInstance.getTimezone())
                + this.contextInstance.getContextTtlMilliseconds());

            this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl
                (previousContextInstance.getId(), previousContextInstance, previousContextInstance.getStatus(), InstanceStatus.ENDED));

            // Initialise the job lock cache for the new instance
            this.jobLockCacheInitialisationService.initialiseJobLockCache(this.context, true);
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
    public void propagateContextInstanceToAgents() {
        if(agents.isEmpty()) {
            logger.warn(String.format("Could not publish context instance[%s] with id[%s], however the agents are empty!"
                , contextInstance.getName(), contextInstance.getId()));
        }
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
            logger.warn(String.format("Publishing context instance[%s] with id[%s] to url[%s]!",
                contextInstance.getName(), contextInstance.getId(), url));
            this.contextInstancePublicationService.publish(url, this.contextInstance);
        }
    }

    /**
     * Method to tear down big queue only.
     *
     * @throws IOException
     */
    private void teardownBigQueue() throws IOException, BigQueueNotFoundException {
        BigQueueManagementService bigQueueManagementService =
            new BigQueueContextMachineManagementServiceImpl(this.getInboundQueueName(),
                this.inboundQueue, this.getOutboundQueueName(), this.outboundQueue, this.getDeadLetterQueueName(),
                this.deadLetterQueue);

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
        if (this.deadLetterQueue != null) {
            this.deadLetterQueue.close();
            this.deadLetterQueue.removeAll();
            this.deadLetterQueue.gc();
            bigQueueDirectoryManagementService.deleteQueue(getDeadLetterQueueName());
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

            this.awaitTerminationAfterShutdown(this.contextExecutor);
            this.awaitTerminationAfterShutdown(this.jobLogicMachine.getExecutor());
            this.awaitTerminationAfterShutdown(this.schedulerInitiatorEventRaisedListenerExecutor);
            this.awaitTerminationAfterShutdown(this.statusListenerExecutor);
            this.awaitTerminationAfterShutdown(this.contextInstanceDlqEventListenerExecutor);

            if (this.inboundQueue != null) {
                this.inboundQueueMessageRunner.stop();
            }
            if (this.outboundQueue != null) {
                this.outboundQueueMessageRunner.stop();
            }

            if (contextInstanceStateChangeEventListeners != null) {
                contextInstanceStateChangeEventListeners.clear();
                this.contextInstanceStateChangeEventListeners = null;
            }

            if (contextInstanceDlqEventBroadcastListeners != null) {
                contextInstanceDlqEventBroadcastListeners.clear();
                this.contextInstanceDlqEventBroadcastListeners = null;
            }


            this.statusListenerExecutor = null;
            this.contextInstanceDlqEventListenerExecutor = null;
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

            this.jobLogicMachine = null;

            BigQueueManagementService bigQueueManagementService =
                new BigQueueContextMachineManagementServiceImpl(this.getInboundQueueName(),
                    this.inboundQueue, this.getOutboundQueueName(), this.outboundQueue,
                    this.getDeadLetterQueueName(), this.deadLetterQueue);

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
            if (this.deadLetterQueue != null) {
                this.deadLetterQueue.removeAll();
                this.deadLetterQueue.gc();
                bigQueueDirectoryManagementService.deleteQueue(getDeadLetterQueueName());
            }

            this.inboundQueue = null;
            this.outboundQueue = null;
            this.deadLetterQueue = null;
            this.queueDir = null;

            this.contextInstance = null;
            this.statusConverter = null;
        } catch (Exception e) {
            logger.warn(String.format("Could not tear down context machine: Error [%s]", e.getMessage()));
        }
    }

    /**
     * Waits for the provided ExecutorService to terminate after shutdown. If termination
     * does not occur within the specified time, forcefully shuts down the ExecutorService.
     *
     * @param threadPool the ExecutorService to wait for termination
     */
    private void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(this.executorWaitTimeoutSeconds, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Sets a listener for events raised when a job is initiated in the scheduler.
     *
     * @param listener the SchedulerJobInitiationEventRaisedListener to be set as the listener
     */
    public void setSchedulerJobInitiationEventRaisedListener(SchedulerJobInitiationEventRaisedListener listener) {
        this.schedulerJobInitiationEventRaisedListener = listener;
    }



    /**
     * Receives an event message and enqueues it to the inbound queue if the context machine is not torn down.
     *
     * @param bigQueueMessage the event message to be enqueued
     * @throws IOException if an I/O error occurs
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

    /**
     * Raises an event by converting the given ContextualisedScheduledProcessEvent to a JSON string and enqueuing it in the inbound queue
     * @param contextualisedScheduledProcessEvent the event to be raised
     * @throws IOException if an I/O error occurs while processing the event
     */
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
     * Retrieves a ContextInstance object based on the provided context name.
     *
     * @param contextName the name of the context to retrieve
     * @return the ContextInstance object corresponding to the provided context name
     */
    public ContextInstance getContext(String contextName) {
        return this.getContextInstanceByName(contextName, this.contextInstance);
    }


    /**
     * Retrieves the context instance associated with this object.
     *
     * @return The context instance.
     */
    public ContextInstance getContext() {
        return this.contextInstance;
    }


    /**
     * Adds a listener to receive events when the state of a scheduler job changes.
     *
     * @param listener The listener to be added
     */
    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.jobLogicMachine.addSchedulerJobStateChangeEventListener(listener);
    }


    /**
     * Removes a SchedulerJobInstanceStateChangeEventListener from the list of listeners.
     *
     * @param listener the SchedulerJobInstanceStateChangeEventListener to be removed
     */
    public void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.jobLogicMachine.removeSchedulerJobStateChangeEventListener(listener);
    }


    /**
     * Adds a listener to be notified of changes in the state of the context instance.
     *
     * @param listener the ContextInstanceStateChangeEventListener to be added
     */
    public void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        if(!contextInstanceStateChangeEventListeners.contains(listener)) {
            this.contextInstanceStateChangeEventListeners.add(listener);
        }
    }


    /**
     * Removes the specified ContextInstanceStateChangeEventListener from the list of listeners.
     *
     * @param listener the ContextInstanceStateChangeEventListener to be removed
     */
    public void removeContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        if(contextInstanceStateChangeEventListeners.contains(listener)) {
            this.contextInstanceStateChangeEventListeners.remove(listener);
        }
    }

    /**
     * Adds a ContextInstanceDlqEventBroadcastListener to the list of listeners.
     *
     * @param listener the ContextInstanceDlqEventBroadcastListener to add
     */
    public void addContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventBroadcastListener listener) {
        if(!this.contextInstanceDlqEventBroadcastListeners.contains(listener)) {
            this.contextInstanceDlqEventBroadcastListeners.add(listener);
        }
    }

    /**
     * Removes a specified ContextInstanceDlqEventBroadcastListener from the list of listeners.
     *
     * @param listener the listener to be removed from the list of listeners
     */
    public void removeContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventBroadcastListener listener) {
        if(this.contextInstanceDlqEventBroadcastListeners.contains(listener)) {
            this.contextInstanceDlqEventBroadcastListeners.remove(listener);
        }
    }



    /**
     * Sets the DryRunParameters for performing a dry run.
     *
     * @param dryRunParameters the DryRunParameters to be set
     */
    public void setDryRunParameters(DryRunParameters dryRunParameters) {
        this.dryRunParameters = dryRunParameters;
    }


    /**
     * Checks if the method is being executed in a dry run mode.
     *
     * @return true if the method is being executed in dry run mode, false otherwise.
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


    /**
     * Sets a flag in the context instance indicating that the context should continue running until manually ended.
     * This method will save the context after updating the flag.
     */
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
     * Skips specified jobs within the given child context.
     *
     * @param childContextName The name of the child context containing the jobs to skip
     * @param skipFlag A boolean flag indicating whether to skip the jobs (true) or not (false)
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
            if(((!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING)
                && (!schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED))) && skipFlag)
                || (!schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED)
                && !schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)
                && !skipFlag)) {
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
                List<SchedulerJobInstance> precedingJobs = ContextHelper.getPrecedingJobs(this.contextInstance, schedulerJobInstance.getJobName()
                    , this.internalEventDrivenJobInstances.entrySet()
                        .stream()
                        .map(entry -> Map.entry(entry.getKey(), (InternalEventDrivenJob) entry.getValue()))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

                AtomicBoolean shouldBeSkipComplete = new AtomicBoolean(false);
                precedingJobs.forEach(job -> {
                    if(this.internalEventDrivenJobInstances.get(job.getIdentifier() + "-" + job.getChildContextName()) != null) {
                        ContextualisedScheduledProcessEventImpl contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
                        contextualisedScheduledProcessEvent.setJobStarting(false);
                        contextualisedScheduledProcessEvent.setJobName(job.getJobName());
                        contextualisedScheduledProcessEvent.setAgentName(job.getAgentName());
                        contextualisedScheduledProcessEvent.setContextName(job.getContextName());
                        contextualisedScheduledProcessEvent.setInternalEventDrivenJob
                            (this.internalEventDrivenJobInstances.get(job.getIdentifier() + "-" + job.getChildContextName()));
                        contextualisedScheduledProcessEvent.setRaisedDueToFailureResubmission(true);

                        try {
                            getEventsThatCanRun(contextualisedScheduledProcessEvent).forEach(event -> {
                                if (event.getInternalEventDrivenJob().getIdentifier().equals(schedulerJobInstance.getIdentifier())) {
                                    shouldBeSkipComplete.set(true);
                                }
                            });
                        } catch (ContextMachineException e) {
                            // safe to ignore this exception
                        }
                    }
                });

                if(precedingJobs.isEmpty() || shouldBeSkipComplete.get()) {
                    schedulerJobInstance.setStatus(InstanceStatus.SKIPPED_COMPLETE);
                    dbInstance.setStatus(InstanceStatus.SKIPPED_COMPLETE);
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.SKIPPED_COMPLETE.name());
                }
                else {
                    schedulerJobInstance.setStatus(InstanceStatus.SKIPPED);
                    dbInstance.setStatus(InstanceStatus.SKIPPED);
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.SKIPPED.name());
                }
            }
            else {
                schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                dbInstance.setStatus(InstanceStatus.WAITING);
                schedulerJobInstanceRecord.setStatus(InstanceStatus.WAITING.name());
            }

            schedulerJobInstanceRecord.setSchedulerJobInstance(dbInstance);
            this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

            if(!this.contextInstance.getStatus().equals(InstanceStatus.PREPARED)) {
                this.recursivelySetContextStatus(this.contextInstance, true);
                this.setContextStatus(contextInstance, true);
                this.saveContext();
            }
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
     * Holds a job identified by jobIdentifier in the specified childContextName.
     * If the job has a specific target residing context, it will only be held for that context.
     * Otherwise, it will be held for all instances in the current context.
     *
     * @param jobIdentifier the identifier of the job to be held
     * @param childContextName the name of the child context where the job should be held
     * @throws ContextMachineException if the specified job cannot be found in the context or its nested contexts
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


    /**
     * Resets the specified job identified by jobIdentifier within the given child context.
     *
     * @param jobIdentifier The unique identifier of the job to reset.
     * @param childContextName The name of the child context within which the job resides.
     */
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

    /**
     * Reset the status of the provided list of SchedulerJobInstance objects.
     *
     * @param jobs List of SchedulerJobInstance objects to reset
     */
    private void _resetJob(List<SchedulerJobInstance> jobs) {
        jobs.forEach(schedulerJobInstance -> {
            if(schedulerJobInstance.getChildContextName() == null) return;
            if (schedulerJobInstance != null) {

                if(this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName())) {
                    ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
                    contextualisedScheduledProcessEvent.setContextInstanceId(this.contextInstance.getId());
                    contextualisedScheduledProcessEvent.setJobName(schedulerJobInstance.getJobName());
                    contextualisedScheduledProcessEvent.setAgentName(schedulerJobInstance.getAgentName());
                    contextualisedScheduledProcessEvent.setChildContextNames(this.internalEventDrivenJobInstances
                        .get(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName()).getChildContextNames());
                    contextualisedScheduledProcessEvent.setContextName(schedulerJobInstance.getContextName());
                    contextualisedScheduledProcessEvent.setInternalEventDrivenJob(this.internalEventDrivenJobInstances
                        .get(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName()));
                    contextualisedScheduledProcessEvent.setJobStarting(false);
                    contextualisedScheduledProcessEvent.setSuccessful(true);

                    ContextInstance childContextInstance = ContextHelper.getChildContextInstance(schedulerJobInstance.getChildContextName(), contextInstance);

                    List<SchedulerJobInitiationEvent> eventsBeforeReset = new ArrayList<>();
                    // Delegate to the job logic machine to determine which events would run if the contextualisedScheduledProcessEvent was raised.
                    jobLogicMachine.getScheduledJobInitiationEventsThatCanBeRaised(contextualisedScheduledProcessEvent
                        , childContextInstance, this.dryRunParameters, this.globalEventJobInstanceMap, this.internalEventDrivenJobInstances
                        , this.contextStartJobInstanceMap, this.contextTerminalJobInstanceMap, this.localEventJobInstanceMap, this.bridgingJobInstanceMap
                        , this.contextInstance.getContextParameters(), this.contextInstance, eventsBeforeReset, false);

                    // Now remove the raised events from the held jobs as we do not want them to run anymore due to the upstream
                    // dependency being reset.
                    eventsBeforeReset.forEach(event -> {
                        if(event.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                            // if the initiation event is for a local event job, we remove any held jobs for all
                            // child contexts in the plan that the local event job may exist in.
                            ContextHelper.getAllContexts(this.contextInstance).keySet().forEach(contextName -> {
                                if (this.contextInstance.getHeldJobs().containsKey(event.getAgentName() + "-" + event.getJobName() + "_" + contextName)) {
                                    logger.info(String.format("Removing held job [%s] in job plan [%s] with id [%s] in child context [%s]," +
                                            " due to scheduler job [%s] being reset!", event.getJobName(), contextInstance.getName(), contextInstance.getId()
                                        , contextName, schedulerJobInstance.getJobName()));
                                    this.contextInstance.getHeldJobs().remove(event.getAgentName() + "-" + event.getJobName() + "_" + contextName);
                                }
                            });
                        }
                        else if(event.getChildContextNames() != null) {
                            event.getChildContextNames().forEach(child -> {
                                if (this.contextInstance.getHeldJobs().containsKey(event.getAgentName() + "-" + event.getJobName() + "_" + child)) {
                                    logger.info(String.format("Removing held job [%s] in job plan [%s] with id [%s] in child context [%s]," +
                                            " due to scheduler job [%s] being reset!", event.getJobName(), contextInstance.getName(), contextInstance.getId()
                                        , child, schedulerJobInstance.getJobName()));
                                    this.contextInstance.getHeldJobs().remove(event.getAgentName() + "-" + event.getJobName() + "_" + child);
                                }
                            });
                        }
                    });
                }

                if (!schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE) &&
                    !schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR) &&
                    !schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) &&
                    !schedulerJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED)) {
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
                        + "_" + this.contextInstance.getId()
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
                else if(this.bridgingJobInstanceMap.containsKey(schedulerJobInstance.getIdentifier() + "-" + schedulerJobInstance.getChildContextName())) {
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
                else {
                    SchedulerJobInstanceRecord schedulerJobInstanceRecord = this.schedulerJobInstanceService.findById(schedulerJobInstance.getJobName()
                        + "_" + schedulerJobInstance.getContextInstanceId()
                        + "_" + schedulerJobInstance.getChildContextName()
                        + "_" + JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
                    if(schedulerJobInstanceRecord != null) {
                        FileEventDrivenJobInstance instance = (FileEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
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
            if (!this.contextInstance.getStatus().equals(InstanceStatus.PREPARED)) {
                this.recursivelySetContextStatus(this.contextInstance, true);
                this.contextInstance.setStatus(InstanceStatus.WAITING);
                this.setContextStatus(this.contextInstance, true);
                this.saveContext();
            }
        }
    }

    /**
     * Releases a job identified by jobIdentifier for a specific child context.
     * If the job is targeting a specific context, it will only be released for that context.
     *
     * @param jobIdentifier The identifier of the job to be released.
     * @param childContextName The name of the child context for which the job is being released.
     */
    public void releaseJob(String jobIdentifier, String childContextName) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, childContextName, jobIdentifier);
        if(schedulerJobInstance != null) {
            if (this.internalEventDrivenJobInstances.containsKey(schedulerJobInstance.getIdentifier() + "-" + childContextName) &&
                this.internalEventDrivenJobInstances.get(schedulerJobInstance.getIdentifier() + "-" + childContextName).isTargetResidingContextOnly()) {
                // if a job is targeting a specific context, we only release it for that context!
                this._releaseJob(List.of(schedulerJobInstance));
            } else {
                List<SchedulerJobInstance> jobInstances = this.getSchedulerJobs(this.contextInstance, jobIdentifier);
                this._releaseJob(jobInstances.stream().filter(job -> job.isHeld()).collect(Collectors.toList()));
            }
        }
        else {
            throw new ContextMachineException(String.format("Attempting to hold job[%s], however this job does not " +
                    "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }


    /**
     * Releases the provided list of SchedulerJobInstance objects.
     *
     * @param jobs List of SchedulerJobInstance objects to be released
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
                schedulerJobInstance.setInitiationEventRaised(false);
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
                    if(schedulerJobInstance.getStatus().equals(InstanceStatus.RUNNING)
                        || (schedulerJobInstance.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB) && schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE))) return;
                    if(!schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD) && !schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
                        throw new ContextMachineException(String.format("Attempting to release job[%s], " +
                                "in context[%s], childContext[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be released."
                            , schedulerJobInstance.getIdentifier(), this.contextInstance.getName(), schedulerJobInstance.getChildContextName()
                            , this.contextInstance.getId(), schedulerJobInstance.getStatus()));
                    }
                    InstanceStatus previousState = schedulerJobInstance.getStatus();
                    schedulerJobInstance.setHeld(false);
                    schedulerJobInstance.setStatus(InstanceStatus.WAITING);
                    schedulerJobInstance.setInitiationEventRaised(false);
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
     * Releases all queued and running jobs associated with the current context instance.
     * Jobs that are in status LOCK_QUEUED or RUNNING will be released by setting the context instance ID
     * and removing them from the JobLockCacheImpl.
     */
    public void releaseQueuedJobs() {
        List<SchedulerJobInstance> runningJobs = this.contextInstance.getAllSchedulerJobInstances().stream()
            .filter(internalEventDrivenJobInstance -> internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.LOCK_QUEUED) ||
                internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.RUNNING))
            .collect(Collectors.toList());

        runningJobs.forEach(job -> {
            job.setContextInstanceId(this.contextInstance.getId());

            // We need to release the lock if a job currently holds it!
            if(JobLockCacheImpl.instance().hasLock(job.getIdentifier(), job.getContextName(), this.contextInstance.getEnvironmentGroup())) {
                JobLockCacheImpl.instance().release(job.getIdentifier(), job.getContextName(), this.contextInstance.getEnvironmentGroup());
            }

            JobLockCacheImpl.instance().removeQueuedSchedulerJob(job, this.contextInstance.getEnvironmentGroup());
        });
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
                    for (Map.Entry<String, BridgingJobInstance> bridgingJobInstanceEntry : bridgingJobInstanceMap.entrySet()) {
                        if (StringUtils.equals(bridgingJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                            schedulerJobInstance = bridgingJobInstanceEntry.getValue();
                            break;
                        }
                    }
                }

                if(schedulerJobInstance == null) {
                    for (Map.Entry<String, LocalEventJobInstance> localEventJobInstanceEntry : localEventJobInstanceMap.entrySet()) {
                        if (StringUtils.equals(localEventJobInstanceEntry.getValue().getJobName(), event.getJobName())) {
                            schedulerJobInstance = localEventJobInstanceEntry.getValue();
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
            } else if (scheduledProcessEvent.getChildContextNames() != null && scheduledProcessEvent.getChildContextNames().size() == 1 &&
                this.localEventJobInstanceMap.containsKey(event.getAgentName() + "-" + event.getJobName() + "-" + scheduledProcessEvent.getChildContextNames().get(0)) &&
                this.getSchedulerJob(contextInstance, scheduledProcessEvent.getChildContextNames().get(0).toString(),
                event.getAgentName() + "-" + event.getJobName()) != null &&
                this.getSchedulerJob(contextInstance, scheduledProcessEvent.getChildContextNames().get(0).toString(),
                    event.getAgentName() + "-" + event.getJobName()).isHeld()) {
                SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, scheduledProcessEvent.getChildContextNames().get(0).toString(),
                    event.getAgentName() + "-" + event.getJobName());
                this.contextInstance.getHeldJobs().put(schedulerJobInstance.getIdentifier()
                    + "_" + schedulerJobInstance.getChildContextName(), event);
            } else {
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

    /**
     * Adds the final bridging events to the list based on the provided SchedulerJobInitiationEvent,
     * ContextualisedScheduledProcessEvent, and a list of final events.
     *
     * @param event The SchedulerJobInitiationEvent to be added to the final events
     * @param scheduledProcessEvent The ContextualisedScheduledProcessEvent associated with the event
     * @param finalEvents The list of final events to which the event will be added if applicable
     */
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
     * Retrieves the ContextInstance with the given name from the hierarchy of given ContextInstance.
     *
     * @param contextName the name of the ContextInstance to retrieve
     * @param contextInstance the root ContextInstance from which to start the search
     * @return the ContextInstance with the specified name, or null if not found
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
     * Recursively sets the status of the given ContextInstance and its child contexts.
     *
     * @param contextInstance the ContextInstance for which to set the status
     * @param broadcastStateChange a boolean indicating whether to broadcast the state change
     */
    private void recursivelySetContextStatus(ContextInstance contextInstance, boolean broadcastStateChange) {
        if(contextInstance.getContexts() != null) {
            contextInstance.getContexts().forEach(context -> {
                this.recursivelySetContextStatus(context, broadcastStateChange);
                this.setContextStatus(context, broadcastStateChange);
            });
        }
    }


    /**
     * Sets the status of the given context instance based on various criteria and conditions.
     *
     * @param contextInstance The context instance for which the status needs to be set
     * @param broadcastStateChange Flag indicating whether to broadcast state change event
     */
    private void setContextStatus(ContextInstance contextInstance, boolean broadcastStateChange) {
        // Status does not change once the instance is ENDED
        if(contextInstance.getStatus().equals(InstanceStatus.ENDED)) return;

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
                .collect(toMap(e -> e.getKey(), e -> SerializationUtils.clone(e.getValue())));
            deepCopy.values().forEach(schedulerJobInstance -> {
                if(this.internalEventDrivenJobInstances != null) {
                    List<SchedulerJobInstance> precedingJobs = ContextHelper.getPrecedingJobsFromOutsideContext(this.contextInstance, schedulerJobInstance.getJobName(), contextInstance.getName()
                        , this.internalEventDrivenJobInstances.entrySet()
                            .stream()
                            .map(entry -> Map.entry(entry.getKey(), (InternalEventDrivenJob) entry.getValue()))
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

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
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

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

    /**
     * Adds a queued scheduler job initiation event to the current context instance.
     *
     * @param event the SchedulerJobInitiationEvent to be added
     */
    public void addQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event) {
        ContextInstance childContextInstance = ContextHelper.getChildContextInstance(event.getInternalEventDrivenJob().getChildContextName()
            , this.contextInstance);

        this.jobLogicMachine.addQueuedSchedulerJobInitiationEvent(childContextInstance, this.contextInstance
            , event.getInternalEventDrivenJob().getIdentifier(), event);

        this.setContextStatus(childContextInstance, false);

        this.saveContext();
    }

    /**
     * Issues a context instance state change event if the previous status is different from the new status.
     * This method logs the event and notifies all context instance state change event listeners in a separate thread.
     *
     * @param event the ContextInstanceStateChangeEvent containing information about the event
     */
    private void issueContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event) {
        if(!event.getPreviousStatus().equals(event.getNewStatus())) {
            logger.debug("Issuing context instance state change event: " + event.getContextInstance().getName() + " " + event.getNewStatus());
            this.statusListenerExecutor.submit(() -> this.contextInstanceStateChangeEventListeners
                .forEach(listener -> listener.onContextInstanceStateChangeEvent(event)));
        }
    }

    /**
     * Method to issue a context instance DLQ (Dead Letter Queue) event by notifying all registered listeners.
     * This method is executed asynchronously using a separate thread to improve performance.
     */
    private void issueContextInstanceDlqEvent() {
        this.statusListenerExecutor.submit(() -> this.contextInstanceDlqEventBroadcastListeners
            .forEach(listener -> listener.receiveBroadcast(this.contextInstance)));
    }

    /**
     * Saves the context instance by creating a new record of ScheduledContextInstanceRecord and
     * populating it with relevant information before saving it using the scheduledContextInstanceService.
     */
    public void saveContext() {
        ScheduledContextInstanceRecord scheduledContextInstanceRecord
            = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(this.contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(this.contextInstance);
        scheduledContextInstanceRecord.setTimestamp(this.contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(this.contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    /**
     * Retrieves the SchedulerJobInstance with the given job identifier from the specified child context within the provided context instance.
     *
     * @param contextInstance The context instance to search for the SchedulerJobInstance.
     * @param childContextName The name of the child context to search within.
     * @param jobIdentifier The identifier of the SchedulerJobInstance to retrieve.
     * @return The SchedulerJobInstance with the specified job identifier in the child context, or null if not found.
     */
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

    /**
     * Retrieves a list of SchedulerJobInstance objects based on the given ContextInstance and job identifier.
     *
     * @param contextInstance The ContextInstance object for which scheduler jobs will be retrieved
     * @param jobIdentifier The identifier of the job to filter results by
     * @return A list of SchedulerJobInstance objects matching the given criteria
     */
    private List<SchedulerJobInstance> getSchedulerJobs(ContextInstance contextInstance, String jobIdentifier) {
        List<SchedulerJobInstance> results = new ArrayList<>();

        this.getSchedulerJobs(contextInstance, jobIdentifier, results);

        return results;
    }

    /**
     * Retrieves all scheduler jobs with the specified job identifier within the given context instance and its child contexts recursively.
     *
     * @param contextInstance the context instance to search for scheduler jobs
     * @param jobIdentifier the identifier of the scheduler job to retrieve
     * @param results the list to store the found SchedulerJobInstance objects
     */
    private void getSchedulerJobs(ContextInstance contextInstance, String jobIdentifier, List<SchedulerJobInstance> results) {
        if(contextInstance.getScheduledJobsMap() != null && contextInstance.getScheduledJobsMap().containsKey(jobIdentifier)) {
            SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(jobIdentifier);
            jobInstance.setContextInstanceId(this.contextInstance.getId());
            results.add(jobInstance);
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

                try {
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
                catch (Exception e) {
                    // we log the error and move onto other job plan instances to broadcast the global event.
                    logger.error(String.format("An error has occurred sending global event[%s] to job plan instance[%s]. Error message[%s]"
                        , schedulerJobInitiationEvent.getJobName(), contextInstanceIdFromCache, e.getMessage()), e);
                }
            }
        }
    }

    /**
     * Broadcasts a local event to all active contexts available in the ContextMachineCache for a given environment group.
     *
     * @param schedulerJobInitiationEvent The SchedulerJobInitiationEvent to broadcast
     * @throws IOException if an I/O error occurs while processing the event
     */
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

    /**
     * Resubmits a message from the Dead Letter Queue (DLQ) to the Inbound Queue.
     *
     * @param messageId unique identifier of the message to resubmit from DLQ
     * @return true if the message was successfully resubmitted, false otherwise
     * @throws IOException if an I/O error occurs during the process
     * @throws BigQueueNotFoundException if the Big Queue is not found
     */
    public boolean resubmitMessageFromDeadLetterQueue(String messageId) throws IOException, BigQueueNotFoundException {
        BigQueueManagementService bigQueueManagementService =
            new BigQueueContextMachineManagementServiceImpl(this.getInboundQueueName(),
                this.inboundQueue, this.getOutboundQueueName(), this.outboundQueue, this.getDeadLetterQueueName(),
                this.deadLetterQueue);

        BigQueueDirectoryManagementService bigQueueDirectoryManagementService
            = new BigQueueDirectoryManagementServiceImpl(bigQueueManagementService, this.queueDir);

        Optional<BigQueueMessage> dlqMessage = bigQueueDirectoryManagementService.getMessages(this.getDeadLetterQueueName())
            .stream().filter(bigQueueMessage -> bigQueueMessage.getMessageId().equals(messageId)).findFirst();

        if(dlqMessage.isPresent()) {
            this.inboundQueue.enqueue(objectMapper.writeValueAsBytes(dlqMessage.get()));
            bigQueueDirectoryManagementService.deleteMessage(this.getDeadLetterQueueName(), messageId);
            logger.info("Successfully resubmitted message[{}] from dead letter queue for job plan instance[{}]!",
                dlqMessage.get().getMessage(), this.contextInstance.getId());
            return true;
        }
        else {
            logger.info("Unable to resubmit message[{}] from dead letter queue for job plan instance[{}]!. Unable to " +
                    " get message associated with that ID from the dead letter queue!",
                messageId, this.contextInstance.getId());
            return false;
        }
    }

    /**
     * This class represents a Runnable implementation for processing inbound queue messages.
     * It runs in a separate thread and processes incoming messages from an inbound queue.
     * The class provides methods to start and stop the message processing.
     *
     * When the run method is executed, it checks if the class is running and proceeds to process the next message in the inbound queue.
     * It deserializes the message, processes the event, and enqueues any necessary outgoing messages to the outbound queue.
     * In case of exceptions during message processing, appropriate error handling is performed.
     *
     * The stop method is used to gracefully stop the message processing by setting the running flag to false.
     * The start method is used to resume the message processing by setting the running flag to true.
     */
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
                        publishJobInitiationEvent(schedulerJobInitiationEvent);
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

                if(bigQueueMessageBlacklist.containsKey(bigQueueMessage.getMessageId())) {
                    logger.info("Successfully processed black listed message[{}] for context instance[{}] with id[{}]." +
                        " Removing the blacklisted message from blacklist map.", bigQueueMessage.getMessageId(),
                        contextInstance.getName(), contextInstance.getId());
                    bigQueueMessageBlacklist.remove(bigQueueMessage.getMessageId());
                }
            }
            catch (Exception e) {
                logger.error(String.format("An error has occurred attempting process scheduled process event [%s]"
                    , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), e);

                // We dequeue messages associated with exceptions and blacklist them. The are then re-enqueued onto the
                // back of the inbound queue associated with the context machine. This repeats until the number of retries
                // exceeds blackListedMessageMaxRetries at which point the offending message is placed onto the associated
                // dead letter queue.
                try {
                    inboundQueue.dequeue();
                    inboundQueue.gc();

                    if(!bigQueueMessageBlacklist.containsKey(bigQueueMessage.getMessageId())) {
                        bigQueueMessageBlacklist.put(bigQueueMessage.getMessageId(), 0);
                        logger.info("Successfully black listed message[{}] for context instance[{}] with id[{}]." +
                                " Adding the blacklisted message to the blacklist map.", bigQueueMessage.getMessageId(),
                            contextInstance.getName(), contextInstance.getId());
                    }

                    if(bigQueueMessageBlacklist.get(bigQueueMessage.getMessageId()) < blackListedMessageMaxRetries) {
                        inboundQueue.enqueue(objectMapper.writeValueAsBytes(bigQueueMessage));
                        Integer retryCount = bigQueueMessageBlacklist.get(bigQueueMessage.getMessageId());
                        bigQueueMessageBlacklist.put(bigQueueMessage.getMessageId(), ++retryCount);
                        logger.info("Re-enqueued black listed message[{}] for context instance[{}] with id[{}]." +
                                " Retry count[{}].", bigQueueMessage.getMessageId()
                            , contextInstance.getName(), contextInstance.getId()
                            , bigQueueMessageBlacklist.get(bigQueueMessage.getMessageId()));
                    }
                    else {
                        // Adding the message to the DLQ
                        deadLetterQueue.enqueue(objectMapper.writeValueAsBytes(bigQueueMessage));
                        bigQueueMessageBlacklist.remove(bigQueueMessage.getMessageId());
                        issueContextInstanceDlqEvent();
                        logger.info("Successfully moved black listed message[{}] for context instance[{}] with id[{}] " +
                                "to the Dead Letter Queue as the max retry count of [{}] has been exceeded!" +
                                " Removed the blacklisted message from the blacklist map.", bigQueueMessage.getMessageId(),
                            contextInstance.getName(), contextInstance.getId(), blackListedMessageMaxRetries);
                    }
                }
                catch (IOException ex) {
                    logger.error(String.format("IOException - An error has occurred attempting to dequeue inbound message [%s]"
                        , bigQueueMessage != null ? bigQueueMessage.getMessage() : "NULL message"), ex);
                }
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

    /**
     * Publishes the SchedulerJobInitiationEvent to the outbound queue after serializing and enqueuing it.
     * This method creates a BigQueueMessage using the provided SchedulerJobInitiationEvent, adds message properties,
     * serializes the message, logs the event, enqueues the serialized message to the outbound queue, and logs the
     * size of the outbound queue.
     *
     * @param schedulerJobInitiationEvent The SchedulerJobInitiationEvent to be published.
     * @throws IOException if an error occurs during serialization or enqueuing of the event.
     */
    public void publishJobInitiationEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) throws IOException {
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
    }


    /**
     * This class represents a Runnable task to process messages from an outbound queue. It implements the Runnable interface
     * and provides methods to start and stop the processing of messages. The run method dequeues messages from the outbound
     * queue, processes them, and triggers an event listener to handle the message. If an error occurs during processing,
     * the message is re-enqueued with exponential back-off retries.
     */
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

                // If the initiation event is a local job we simply broadcast that locally
                if(schedulerJobInitiationEvent.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                    broadcastLocalEvent(schedulerJobInitiationEvent);
                }
                // otherwise we delegate to the event raised listeners
                else if(schedulerJobInitiationEventRaisedListener != null) {
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

    /**
     * Sets up an inbound listener to listen for new messages in the inbound queue.
     * Creates a new InboundQueueMessageRunner and adds it as a listener to the inboundListenableFuture.
     * Uses the contextExecutor provided to execute the listener on a specific context.
     * If an exception occurs, a warning log is generated indicating the failure to add the inbound listener.
     */
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

    /**
     * Sets up an outbound listener by peeking into the outbound queue asynchronously, initializing an
     * OutboundQueueMessageRunner, and adding the runner as a listener to the outboundListenableFuture.
     * This method is protected and intended for internal use within the class or its subclasses.
     */
    protected void addOutboundListener() {
        outboundListenableFuture = outboundQueue.peekAsync();
        this.outboundQueueMessageRunner = new OutboundQueueMessageRunner();
        outboundListenableFuture.addListener(outboundQueueMessageRunner, schedulerInitiatorEventRaisedListenerExecutor);
    }

    /**
     * Retrieves a map of GlobalEventJobInstance objects.
     *
     * @return a Map with String keys representing job instance names and GlobalEventJobInstance values
     */
    protected Map<String, GlobalEventJobInstance> getGlobalEventJobInstanceMap() {
        return globalEventJobInstanceMap;
    }

    /**
     * Retrieves a map containing the internal event-driven job instances.
     *
     * @return A mapping of String keys to InternalEventDrivenJobInstance values representing the internal event-driven job instances.
     */
    public Map<String, InternalEventDrivenJobInstance> getInternalEventDrivenJobInstancesMap() {
        return internalEventDrivenJobInstances;
    }
}
