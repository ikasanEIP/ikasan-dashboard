package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.component.converter.ContextInstanceToContextInstanceStatusConverter;
import org.ikasan.scheduler.core.event.ContextInstanceStateChangeEvent;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.scheduler.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.spec.Context;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextMachine {
    private ContextInstance contextInstance;
    private JobLogicMachine jobLogicMachine;
    private ContextInstanceToContextInstanceStatusConverter statusConverter;
    private List<ContextInstanceStateChangeEventListener> contextInstanceStateChangeEventListeners;
    private ExecutorService executor;

    /**
     * Constructor
     *
     * @param contextInstance
     */
    public ContextMachine(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
        jobLogicMachine = new JobLogicMachine();
        statusConverter = new ContextInstanceToContextInstanceStatusConverter();
        contextInstanceStateChangeEventListeners = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     *
     * @param scheduledProcessEvent
     * @return
     */
    public List<SchedulerJobInitiationEvent> eventReceived(ScheduledProcessEvent scheduledProcessEvent) {
        return this.getInitiationEvents(this.contextInstance, scheduledProcessEvent);
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
    private List<SchedulerJobInitiationEvent> getInitiationEvents(ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
         if(!contextInstance.getStatus().equals(InstanceStatus.COMPLETE)
             && contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {

             // Delegate to the JobLogicMachine to determine if any any SchedulerJobInitiationEvents are
             // required to be raised.
             List<SchedulerJobInitiationEvent> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                 , contextInstance.getScheduledJobsMap(), contextInstance.getJobDependencies());

             // Update the context status after event received and attached
             // to the job instance.
             this.setContextStatus(contextInstance);

             return events;
        }

        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

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
        this.executor.submit(() -> this.contextInstanceStateChangeEventListeners
            .forEach(listener -> listener.onContextInstanceStateChangeEvent(event)));
    }
}
