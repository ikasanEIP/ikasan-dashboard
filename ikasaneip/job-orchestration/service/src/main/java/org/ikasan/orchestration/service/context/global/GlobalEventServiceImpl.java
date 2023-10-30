package org.ikasan.orchestration.service.context.global;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalEventServiceImpl implements GlobalEventService {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalEventServiceImpl.class);

    public static final String GLOBAL_EVENT_MANUALLY_RAISED = "GLOBAL_EVENT_MANUALLY_RAISED";
    @Override
    public void raiseGlobalEventJob(GlobalEventJobInstance globalEventJobInstance, String contextInstanceId, String username) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(JobConstants.GLOBAL_EVENT);
        schedulerJobInitiationEvent.setJobName(globalEventJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextInstanceId(contextInstanceId);

        ContextualisedScheduledProcessEvent catalystEvent = new ContextualisedScheduledProcessEventImpl();
        catalystEvent.setJobName("Manually raise by user " + username);
        catalystEvent.setContextName(GLOBAL_EVENT_MANUALLY_RAISED);
        catalystEvent.setContextInstanceId("Not Applicable");
        catalystEvent.setFireTime(System.currentTimeMillis());
        catalystEvent.setCompletionTime(System.currentTimeMillis());

        schedulerJobInitiationEvent.setCatalystEvent(catalystEvent);

        ContextualisedScheduledProcessEvent scheduledProcessEvent = new ContextualisedScheduledProcessEventImpl();
        scheduledProcessEvent.setCatalystEvent(catalystEvent);

        globalEventJobInstance.setScheduledProcessEvent(scheduledProcessEvent);

        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);

        if(contextMachine == null) {
            throw new GlobalEventServiceException(String.format("Could not resolve context instance from the" +
                " context instance cache using id[%s]", contextInstanceId));
        }

        try {
            contextMachine.broadcastGlobalEvents(schedulerJobInitiationEvent, false, false);
        }
        catch (IOException e) {
            throw new GlobalEventServiceException("An exception has occurred broadcast global events", e);
        }
    }

    /**
     * Used to raise a global event regardless of what environment it needs to target. It will broadcast it to all active
     * context instances that are currently running
     * @param globalJobName name of the global event job name
     */
    @Override
    public void raiseGlobalEventJob(String globalJobName) {

        ConcurrentHashMap<String, ContextMachine> contextMachineMap = ContextMachineCache.instance().getContextInstanceByContextInstanceIdCache();
        if(contextMachineMap == null || contextMachineMap.size() == 0) {
            LOG.warn("There are no context instances running to send the Global Event [{}]. This will be ignored", globalJobName);
            return;
        }

        // As we are going to send it to all Context Machine regardless of environment group, find any Context Machine
        // in the cache and attempt to send. The Context Machine for that instance will handle
        // the firing of the events to all other Contexts Instance
        Map.Entry<String, ContextMachine> firstContextMachine = contextMachineMap.entrySet().iterator().next();

        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(JobConstants.GLOBAL_EVENT);
        schedulerJobInitiationEvent.setJobName(globalJobName);
        schedulerJobInitiationEvent.setContextInstanceId(firstContextMachine.getValue().getContext().getId());

        try {
            firstContextMachine.getValue().broadcastGlobalEvents(schedulerJobInitiationEvent, true, true);
        } catch (IOException e) {
            throw new GlobalEventServiceException("An exception has occurred broadcast global events", e);
        }
    }
}
