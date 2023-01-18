package org.ikasan.orchestration.service.context.global;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;

import java.io.IOException;

public class GlobalEventServiceImpl implements GlobalEventService {

    @Override
    public void raiseGlobalEventJob(GlobalEventJobInstance globalEventJobInstance, String contextInstanceId) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(JobConstants.GLOBAL_EVENT);
        schedulerJobInitiationEvent.setJobName(globalEventJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstanceId);

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);

        if(contextMachine == null) {
            throw new GlobalEventServiceException(String.format("Could not resolve context instance from the" +
                " context instance cache using id[%s]", contextInstanceId));
        }

        try {
            contextMachine.broadcastGlobalEvents(schedulerJobInitiationEvent);
        }
        catch (IOException e) {
            throw new GlobalEventServiceException("An exception has occurred broadcast global events", e);
        }
    }
}
