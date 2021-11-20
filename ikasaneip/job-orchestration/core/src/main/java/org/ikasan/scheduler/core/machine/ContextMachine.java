package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.InstanceStatus;
import org.ikasan.scheduler.core.spec.Context;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextMachine {
    private ContextInstance contextInstance;
    private JobLogicMachine jobLogicMachine = new JobLogicMachine();

    public ContextMachine(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }

    public List<SchedulerJobInitiationEvent> eventReceived(ScheduledProcessEvent scheduledProcessEvent) {
        return this.getInitiationEvents(this.contextInstance, scheduledProcessEvent);
    }

    public InstanceStatus getContextStatus(String contextName) {
        ContextInstance instance = this.getContextInstanceByName(contextName, this.contextInstance);

        if(instance != null) {
            return instance.getStatus();
        }

        return null;
    }

    public ContextInstance getContext(String contextName) {
        return this.getContextInstanceByName(contextName, this.contextInstance);
    }

    private List<SchedulerJobInitiationEvent> getInitiationEvents(ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
         if(!contextInstance.getStatus().equals(InstanceStatus.COMPLETE)
             && contextInstance.getScheduledJobsMap().containsKey(scheduledProcessEvent.getAgentName()
                + "-" + scheduledProcessEvent.getJobName())) {
             List<SchedulerJobInitiationEvent> events = jobLogicMachine.getJobInitiationEvents(scheduledProcessEvent
                 , contextInstance.getScheduledJobsMap(), contextInstance.getJobDependencies());

             // Update the context status after event received and attached
             // to the job instance.
             this.setContextStatus(contextInstance);

             return events;
        }

        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(Context instance: contextInstance.getContexts()) {
                List<SchedulerJobInitiationEvent> results = this.getInitiationEvents((ContextInstance) instance, scheduledProcessEvent);

                if(results != null) {
                    return results;
                }
            }
        }

        return null;
    }

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
        AtomicBoolean anyErrorJobs = new AtomicBoolean(false);
        contextInstance.getScheduledJobs().forEach(job -> {
            if(!job.getStatus().equals(InstanceStatus.COMPLETE)) {
                allJobsComplete.set(false);
            }
            if(job.getStatus().equals(InstanceStatus.ERROR)) {
                anyErrorJobs.set(true);
            }
        });

        if(anyErrorJobs.get()){
            contextInstance.setStatus(InstanceStatus.ERROR);
        }
        else if(allJobsComplete.get()) {
            contextInstance.setStatus(InstanceStatus.COMPLETE);
        }
        else {
            contextInstance.setStatus(InstanceStatus.RUNNING);
        }
    }
}
