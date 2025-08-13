package org.ikasan.job.orchestration.service;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheManagementService;

import java.io.IOException;
import java.util.List;

public class JobLockCacheManagementServiceImpl implements JobLockCacheManagementService {
    @Override
    public void releaseLockedJob(String jobIdentifier, String contextName) {
        JobLockCacheImpl.instance().release(jobIdentifier, contextName);
        List<ContextualisedSchedulerJobInitiationEvent> queuedEvents = JobLockCacheImpl.instance()
            .pollSchedulerJobInitiationEventWaitQueue(jobIdentifier, contextName);
        if (queuedEvents != null && !queuedEvents.isEmpty()) {
            for (ContextualisedSchedulerJobInitiationEvent queuedEvent : queuedEvents) {
                if (ContextMachineCache.instance().containsInstanceIdentifier(queuedEvent.getSchedulerJobInitiationEvent().getContextInstanceId())) {
                    try {
                        JobLockCacheImpl.instance().lock(queuedEvent.getSchedulerJobInitiationEvent()
                            .getInternalEventDrivenJob().getIdentifier(), contextName);
                        ContextMachineCache.instance().getByContextInstanceId
                                (queuedEvent.getSchedulerJobInitiationEvent().getContextInstanceId())
                            .publishJobInitiationEvent(queuedEvent.getSchedulerJobInitiationEvent());
                    } catch (IOException e) {
                        throw new RuntimeException(String.format("An error has occurred releasing locked job %s from context %s!"
                            , jobIdentifier, contextName), e);
                    }
                }
            }
        }
    }

    @Override
    public void removeQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) {
        SchedulerJobInstance schedulerJobInstance = schedulerJobInitiationEvent.getInternalEventDrivenJob();
        JobLockCacheImpl.instance().removeQueuedSchedulerJob(schedulerJobInstance);
        if (ContextMachineCache.instance().containsInstanceIdentifier(schedulerJobInstance.getContextInstanceId())) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(schedulerJobInstance.getContextInstanceId());
            contextMachine.resetJob(schedulerJobInstance.getIdentifier(),
                schedulerJobInstance.getChildContextName());
        }
    }
}
