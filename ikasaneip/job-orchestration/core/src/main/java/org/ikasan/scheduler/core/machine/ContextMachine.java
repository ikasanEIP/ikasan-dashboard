package org.ikasan.scheduler.core.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.leansoft.bigqueue.BigQueueImpl;
import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.scheduler.core.component.converter.ContextInstanceToContextInstanceStatusConverter;
import org.ikasan.scheduler.core.event.ContextInstanceStateChangeEvent;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduler.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.scheduler.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.scheduler.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.scheduler.core.model.instance.ScheduledProcessEventInstance;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.scheduler.core.spec.Context;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private List<SchedulerJobInitiationEventRaisedListener> schedulerJobInitiationEventRaisedListeners;
    private Context context;

    public ContextMachine(Context context, ContextInstance contextInstance, ScheduledContextInstanceService scheduledContextInstanceService) {
        this(contextInstance, scheduledContextInstanceService);
        this.context = context;
    }
    /**
     * Constructor
     *
     * @param contextInstance
     */
    public ContextMachine(ContextInstance contextInstance, ScheduledContextInstanceService scheduledContextInstanceService) {
        this.contextInstance = contextInstance;
        this.jobLogicMachine = new JobLogicMachine();
        this.statusConverter = new ContextInstanceToContextInstanceStatusConverter();
        this.contextInstanceStateChangeEventListeners = new ArrayList<>();
        this.statusListenerExecutor = Executors.newSingleThreadExecutor();
        this.contextExecutor = Executors.newSingleThreadExecutor();
        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newSingleThreadExecutor();
//        this.schedulerInitiatorEventRaisedListenerExecutor = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerJobInitiationEventRaisedListeners = new ArrayList<>();
    }

    public void init() throws IOException {
        String queueDir = "/sandbox/mick/bigquque";
        String inboundQueueName = "inbound-context-queue";
        String outboundQueueName = "outbound-context-queue";
        this.inboundQueue = new BigQueueImpl(queueDir, inboundQueueName);
        this.outboundQueue = new BigQueueImpl(queueDir, outboundQueueName);

        inboundListenableFuture = this.inboundQueue.peekAsync();
        inboundListenableFuture.addListener(new InboundQueueMessageRunner(), this.contextExecutor);

        outboundListenableFuture = this.outboundQueue.peekAsync();
        outboundListenableFuture.addListener(new OutboundQueueMessageRunner(), this.schedulerInitiatorEventRaisedListenerExecutor);
    }

    public void resetContextInstance() throws JsonProcessingException {
        if(this.context != null) {
            ContextService contextService = new ContextService();
            contextInstance = contextService.getContextInstance(contextService.getContextString(this.context));
            contextInstance.setId(UUID.randomUUID().toString());
        }
    }

    public void teardown() throws IOException {
        this.inboundQueue.close();
        this.outboundQueue.close();
    }

    public void addSchedulerJobInitiationEventRaisedListener(SchedulerJobInitiationEventRaisedListener listener) {
        this.schedulerJobInitiationEventRaisedListeners.add(listener);
    }

    /**
     *
     * @param scheduledProcessEvent
     * @return
     */
    protected List<SchedulerJobInitiationEventImpl> eventReceived(ScheduledProcessEvent scheduledProcessEvent) {
        return this.getInitiationEvents(this.contextInstance, scheduledProcessEvent);
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

    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.jobLogicMachine.addSchedulerJobStateChangeEventListener(listener);
    }

    public void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        this.contextInstanceStateChangeEventListeners.add(listener);
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
    private List<SchedulerJobInitiationEventImpl> getInitiationEvents(ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
         if(!contextInstance.getStatus().equals(InstanceStatus.COMPLETE)
             && contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {

             // Delegate to the JobLogicMachine to determine if any any SchedulerJobInitiationEvents are
             // required to be raised.
             List<SchedulerJobInitiationEventImpl> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                 , contextInstance.getScheduledJobsMap(), contextInstance.getJobDependencies());

             // Update the context status after event received and attached
             // to the job instance.
             this.setContextStatus(contextInstance);

             return events;
        }

        List<SchedulerJobInitiationEventImpl> results = new ArrayList<>();

        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(Context instance: contextInstance.getContexts()) {
                // Recursively work our way through all nested contexts to determine if and job initiation events need to be raised.
                results.addAll(this.getInitiationEvents((ContextInstance) instance, scheduledProcessEvent));
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
                this.issueContextInstanceStateChangeEvent(new ContextInstanceStateChangeEvent(contextInstance, previousStatus, newStatus));
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

    private void issueContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event) {
        this.statusListenerExecutor.submit(() -> this.contextInstanceStateChangeEventListeners
            .forEach(listener -> listener.onContextInstanceStateChangeEvent(event)));
    }

    private void addInboundQueueRunner() {
        inboundListenableFuture.addListener(new InboundQueueMessageRunner(), contextExecutor);
    }

    private void saveContext() throws JsonProcessingException {
        ScheduledContextInstanceRecordImpl scheduledContextInstanceRecord
            = new ScheduledContextInstanceRecordImpl(this.contextInstance.getId(), this.contextInstance.getName(),
                this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.contextInstance),
                this.contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(this.contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    private class InboundQueueMessageRunner implements Runnable {

        @Override
        public void run() {
            try {
                byte[] event = inboundQueue.peek();

                if(event == null) {
                    return;
                }

                ScheduledProcessEvent scheduledProcessEvent
                    = objectMapper.readValue(event, ScheduledProcessEventInstance.class);

                List<SchedulerJobInitiationEventImpl> schedulerJobInitiationEvents = eventReceived(scheduledProcessEvent);

                saveContext();

                for(SchedulerJobInitiationEventImpl schedulerJobInitiationEvent: schedulerJobInitiationEvents) {
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

    private class OutboundQueueMessageRunner implements Runnable {

        @Override
        public void run() {
            try {
                byte[] event = outboundQueue.peek();
                if(event == null) {
                    return;
                }

                SchedulerJobInitiationEventImpl schedulerJobInitiationEvent
                    = objectMapper.readValue(event, SchedulerJobInitiationEventImpl.class);

                for (SchedulerJobInitiationEventRaisedListener listener : schedulerJobInitiationEventRaisedListeners) {
                    listener.onSchedulerJobInitiationEventRaised(schedulerJobInitiationEvent);
                }

                outboundQueue.dequeue();
                outboundQueue.gc();
                logger.info("Dequeue job initiation event: " + schedulerJobInitiationEvent);
                logger.info("Outbound queue size: " + outboundQueue.size());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                outboundListenableFuture = outboundQueue.peekAsync();
                outboundListenableFuture.addListener(new OutboundQueueMessageRunner(), schedulerInitiatorEventRaisedListenerExecutor);
            }
        }
    }
}
