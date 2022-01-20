package org.ikasan.scheduler.core.machine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.scheduler.core.component.converter.ContextInstanceToContextInstanceStatusConverter;
import org.ikasan.scheduler.core.model.context.*;
import org.ikasan.scheduler.core.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.scheduler.core.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduler.core.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.instance.ContextParameterInstanceImpl;
import org.ikasan.scheduler.core.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.scheduler.core.model.job.SchedulerJobImpl;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.scheduler.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
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
    private SchedulerJobInitiationEventRaisedListener schedulerJobInitiationEventRaisedListener;
    private ContextTemplate context;
    private int attempts;
    private long maxWait;
    private DryRunParameters dryRunParameters;
    private Map<String, InternalEventDrivenJob> internalEventDrivenJobs;
    private String queueDir;

    // todo clean up the transient queues once a context is complete.
    public ContextMachine(ContextTemplate context, ContextInstance contextInstance, ScheduledContextInstanceService scheduledContextInstanceService,
                          Map<String, InternalEventDrivenJob> internalEventDrivenJobs, String queueDir) {
        this.internalEventDrivenJobs = internalEventDrivenJobs;
        this.context = context;

        this.contextInstance = contextInstance;
        this.internalEventDrivenJobs = internalEventDrivenJobs;
        this.queueDir = queueDir;
        this.jobLogicMachine = new JobLogicMachine();
        this.statusConverter = new ContextInstanceToContextInstanceStatusConverter();
        this.contextInstanceStateChangeEventListeners = new ArrayList<>();
        this.statusListenerExecutor = Executors.newSingleThreadExecutor();
        this.contextExecutor = Executors.newSingleThreadExecutor();
        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newSingleThreadExecutor();
        this.objectMapper = ObjectMapperFactory.newInstance();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.scheduledContextInstanceService = scheduledContextInstanceService;
    }

    /**
     *
     * @throws IOException
     */
    public void init() throws IOException {
        String inboundQueueName = "inbound-"+this.contextInstance.getId()+"-queue";
        String outboundQueueName = "outbound-"+this.contextInstance.getId()+"-queue";
        this.inboundQueue = new BigQueueImpl(this.queueDir, inboundQueueName);
        this.outboundQueue = new BigQueueImpl(this.queueDir, outboundQueueName);

        inboundListenableFuture = this.inboundQueue.peekAsync();
        inboundListenableFuture.addListener(new InboundQueueMessageRunner(), this.contextExecutor);

        this.addOutboundListener();

        this.attempts = 0;
        this.maxWait = 10000L;
    }

    /**
     *
     * @throws JsonProcessingException
     */
    public void resetContextInstance() throws JsonProcessingException {
        if(this.context != null) {
            ContextService contextService = new ContextService();
            contextInstance = contextService.getContextInstance(contextService.getContextString(this.context));
            contextInstance.setId(UUID.randomUUID().toString());
        }
    }

    /**
     *
     * @throws IOException
     */
    public void teardown() throws IOException {
        this.inboundQueue.close();
        this.outboundQueue.close();
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
     * @param scheduledProcessEvent
     * @return
     */
    public void eventReceived(String scheduledProcessEvent) throws IOException {
        this.inboundQueue.enqueue(scheduledProcessEvent.getBytes());
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
    public void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        this.contextInstanceStateChangeEventListeners.add(listener);
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
     * @param jobIdentifier
     */
    public void holdJob(String jobIdentifier) {
        SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, jobIdentifier);
        if(schedulerJobInstance != null) {
            if(!schedulerJobInstance.getStatus().equals(InstanceStatus.WAITING) &&
                !schedulerJobInstance.getStatus().equals(InstanceStatus.RELEASED)) {
                throw new ContextMachineException(String.format("Attempting to hold job[%s], " +
                        "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be put on hold."
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
            }
            schedulerJobInstance.setHeld(true);
            schedulerJobInstance.setStatus(InstanceStatus.ON_HOLD);
            this.saveContext();
            logger.info(String.format("Successfully held job[%s]. Context[%s], Context Instance[%s]."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
        else {
            throw new ContextMachineException(String.format("Attempting to hold job[%s], however this job does not " +
                "appear in context[%s] with instance id[%s], or any of its nested contexts."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
    }

    /**
     *
     * @param jobIdentifier
     * @throws IOException
     */
    public void releaseJob(String jobIdentifier) throws IOException {
        SchedulerJobInitiationEvent event = this.contextInstance.getHeldJobs().get(jobIdentifier);
        if(event != null) {
            this.contextInstance.getHeldJobs().remove(jobIdentifier);
            String serialised = objectMapper.writeValueAsString(event);
            logger.info("Enqueue job initiation event: " + serialised);
            outboundQueue.enqueue(serialised.getBytes());
            logger.info("Outbound queue size: " + outboundQueue.size());
            SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, jobIdentifier);
            schedulerJobInstance.setHeld(false);
            schedulerJobInstance.setStatus(InstanceStatus.RELEASED);
            this.saveContext();
            logger.info(String.format("Successfully released job[%s]. Context[%s], Context Instance[%s]."
                , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
        }
        else {
            SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(this.contextInstance, jobIdentifier);

            if(schedulerJobInstance != null) {
                if(!schedulerJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
                    throw new ContextMachineException(String.format("Attempting to release job[%s], " +
                            "in context[%s] with instance id[%s]. The job currently has a status of [%s] which cannot be released."
                        , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId(), schedulerJobInstance.getStatus()));
                }
                schedulerJobInstance.setHeld(false);
                schedulerJobInstance.setStatus(InstanceStatus.RELEASED);
                this.saveContext();
                logger.info(String.format("Successfully released job[%s]. Context[%s], Context Instance[%s]."
                    , jobIdentifier, this.contextInstance.getName(), this.contextInstance.getId()));
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
     *
     * @param scheduledProcessEvent
     * @return
     */
    protected List<SchedulerJobInitiationEvent> eventReceived(ScheduledProcessEvent scheduledProcessEvent) {
        List<SchedulerJobInitiationEvent> events = this.getInitiationEvents(this.contextInstance, scheduledProcessEvent);

        List<SchedulerJobInitiationEvent> finalEvents = new ArrayList<>();

        events.forEach(event -> {
            SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance, event.getInternalEventDrivenJob().getIdentifier());

            if(schedulerJobInstance != null && schedulerJobInstance.isHeld()) {
                this.contextInstance.getHeldJobs().put(schedulerJobInstance.getIdentifier(), event);
            }
            else {
                finalEvents.add(event);
            }
        });

        return finalEvents;
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
    private List<SchedulerJobInitiationEvent> getInitiationEvents(ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
         if(!contextInstance.getStatus().equals(InstanceStatus.COMPLETE)
             && contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {

             // Delegate to the JobLogicMachine to determine if any any SchedulerJobInitiationEvents are
             // required to be raised.
             List<SchedulerJobInitiationEvent> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                 , contextInstance, this.dryRunParameters, this.internalEventDrivenJobs, this.contextInstance.getContextParameters());

             // Update the context status after event received and attached
             // to the job instance.
             this.setContextStatus(contextInstance);

             return events;
        }

        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(ContextInstance instance: contextInstance.getContexts()) {
                // Recursively work our way through all nested contexts to determine if any job initiation events need to be raised.
                results.addAll(this.getInitiationEvents((ContextInstanceImpl) instance, scheduledProcessEvent));
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
        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
            AtomicBoolean allJobsComplete = new AtomicBoolean(true);
            AtomicBoolean anyErrorJobs = new AtomicBoolean(false);

            contextInstance.getScheduledJobs().forEach(job -> {
                if (!job.getStatus().equals(InstanceStatus.COMPLETE)) {
                    allJobsComplete.set(false);
                }
                if (job.getStatus().equals(InstanceStatus.ERROR)) {
                    anyErrorJobs.set(true);
                }
            });

            InstanceStatus previousStatus = contextInstance.getStatus();

            if (anyErrorJobs.get()) {
                contextInstance.setStatus(InstanceStatus.ERROR);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            } else if (allJobsComplete.get()) {
                contextInstance.setStatus(InstanceStatus.COMPLETE);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            } else {
                contextInstance.setStatus(InstanceStatus.RUNNING);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            }

            InstanceStatus newStatus = contextInstance.getStatus();

            if(!previousStatus.equals(newStatus)) {
                this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEventImpl(contextInstance, previousStatus, newStatus));
            }
        }
        else if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            AtomicBoolean allContextsComplete = new AtomicBoolean(true);
            AtomicBoolean anyErrorContexts = new AtomicBoolean(false);

            contextInstance.getContexts().forEach(context -> {
                if (!context.getStatus().equals(InstanceStatus.COMPLETE)) {
                    allContextsComplete.set(false);
                }
                if (context.getStatus().equals(InstanceStatus.ERROR)) {
                    anyErrorContexts.set(true);
                }
            });

            if (anyErrorContexts.get()) {
                contextInstance.setStatus(InstanceStatus.ERROR);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            } else if (allContextsComplete.get()) {
                contextInstance.setStatus(InstanceStatus.COMPLETE);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            } else {
                contextInstance.setStatus(InstanceStatus.RUNNING);
                contextInstance.setUpdatedDateTime(System.currentTimeMillis());
            }
        }
    }

    private void issueContextInstanceStateChangeEvent(ContextInstanceStateChangeEventImpl event) {
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

    private SchedulerJobInstance getSchedulerJob(ContextInstance contextInstance, String jobIdentifier) {
        if(contextInstance.getScheduledJobsMap() != null && contextInstance.getScheduledJobsMap().containsKey(jobIdentifier)) {
            return contextInstance.getScheduledJobsMap().get(jobIdentifier);
        }
        else if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            for(ContextInstance contextInstance1: contextInstance.getContexts()) {
                 SchedulerJobInstance schedulerJobInstance = this.getSchedulerJob(contextInstance1, jobIdentifier);

                 if(schedulerJobInstance != null) {
                     return schedulerJobInstance;
                 }
            }

            return null;
        }

        return null;
    }

    protected class InboundQueueMessageRunner implements Runnable {

        @Override
        public void run() {
            try {
                byte[] event = inboundQueue.peek();

                if(event == null) {
                    return;
                }

                ScheduledProcessEvent scheduledProcessEvent
                    = objectMapper.readValue(event, ContextualisedScheduledProcessEventImpl.class);

                List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = eventReceived(scheduledProcessEvent);

                saveContext();

                for(SchedulerJobInitiationEvent schedulerJobInitiationEvent: schedulerJobInitiationEvents) {
                    String serialised = objectMapper.writeValueAsString(schedulerJobInitiationEvent);
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
                inboundListenableFuture = inboundQueue.peekAsync();
                inboundListenableFuture.addListener(new InboundQueueMessageRunner(), contextExecutor);
            }
        }
    }

    protected class OutboundQueueMessageRunner implements Runnable {

        @Override
        public void run() {
            boolean exception = false;
            SchedulerJobInitiationEventImpl schedulerJobInitiationEvent = null;
            try {
                byte[] event = outboundQueue.peek();
                if(event == null) {
                    return;
                }

                schedulerJobInitiationEvent
                    = objectMapper.readValue(event, SchedulerJobInitiationEventImpl.class);

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
                        logger.info("Dequeue job initiation event: " + schedulerJobInitiationEvent);
                        logger.info("Outbound queue size: " + outboundQueue.size());
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                addOutboundListener();
            }
        }
    }

    protected void addOutboundListener() {
        outboundListenableFuture = outboundQueue.peekAsync();
        outboundListenableFuture.addListener(new OutboundQueueMessageRunner(), schedulerInitiatorEventRaisedListenerExecutor);
    }
}
