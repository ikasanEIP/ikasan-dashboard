package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.scheduler.core.spec.Context;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ContextMachine {
    private ContextInstance contextInstance;

    public ContextMachine(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }

    public List<SchedulerJobInitiationEvent> eventReceived(ScheduledProcessEvent scheduledProcessEvent) {
        return this.getInitiationEvents(this.contextInstance, scheduledProcessEvent);
    }

    private List<SchedulerJobInitiationEvent> getInitiationEvents(ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        List<SchedulerJobInitiationEvent> results = null;

        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {

            AtomicReference<SchedulerJobInstance> schedulerJobInstance = new AtomicReference<>();

            contextInstance.getScheduledJobs().forEach(instance -> {
                if(instance.getAgentName().equals(scheduledProcessEvent.getAgentName())
                    && instance.getJobName().equals(scheduledProcessEvent.getJobName())) {
                    schedulerJobInstance.set(instance);
                }
            });

            if(schedulerJobInstance.get() != null) {
                results = new ArrayList<>();
//                results.add(new SchedulerJobInitiationEvent());

                return results;
            }
        }

        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()){
            for(Context instance: contextInstance.getContexts()) {
                results = this.getInitiationEvents((ContextInstance) instance, scheduledProcessEvent);

                if(results != null) {
                    return results;
                }
            }
        }

        return null;
    }
}
