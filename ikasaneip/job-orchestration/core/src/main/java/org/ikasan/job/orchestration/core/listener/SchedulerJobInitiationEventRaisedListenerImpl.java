package org.ikasan.job.orchestration.core.listener;

import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;

/**
 * Default SchedulerJobInitiationEventRaisedListener implementation.
 */
public class SchedulerJobInitiationEventRaisedListenerImpl implements SchedulerJobInitiationEventRaisedListener {
    private SchedulerService schedulerService;

    @Override
    public void onSchedulerJobInitiationEventRaised(SchedulerJobInitiationEvent event) {
        this.schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
    }
}
