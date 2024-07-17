package org.ikasan.orchestration.service.context.local;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.LocalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.LocalEventService;

import java.io.IOException;

public class LocalEventServiceImpl implements LocalEventService {
    public static final String LOCAL_EVENT_MANUALLY_RAISED = "LOCAL_EVENT_MANUALLY_RAISED";

    @Override
    public void raiseLocalEventJob(LocalEventJobInstance localEventJobInstance, String contextInstanceId, String username) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(JobConstants.LOCAL_EVENT_JOB);
        schedulerJobInitiationEvent.setJobName(localEventJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstanceId);

        ContextualisedScheduledProcessEvent catalystEvent = new ContextualisedScheduledProcessEventImpl();
        catalystEvent.setJobName("Manually raise by user " + username);
        catalystEvent.setContextName(LOCAL_EVENT_MANUALLY_RAISED);
        catalystEvent.setContextInstanceId("Not Applicable");
        catalystEvent.setFireTime(System.currentTimeMillis());
        catalystEvent.setCompletionTime(System.currentTimeMillis());

        schedulerJobInitiationEvent.setCatalystEvent(catalystEvent);

        ContextualisedScheduledProcessEvent scheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent.setCatalystEvent(catalystEvent);

        localEventJobInstance.setScheduledProcessEvent(scheduledProcessEvent);

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);

        if(contextMachine == null) {
            throw new LocalEventServiceException(String.format("Could not resolve context instance from the" +
                " context instance cache using id[%s] when attempting to submit local event[%s]"
                , contextInstanceId, localEventJobInstance.getJobName()));
        }

        try {
            contextMachine.broadcastLocalEvent(schedulerJobInitiationEvent);
        }
        catch (IOException e) {
            throw new LocalEventServiceException("An exception has occurred broadcast a local event!", e);
        }
    }
}
