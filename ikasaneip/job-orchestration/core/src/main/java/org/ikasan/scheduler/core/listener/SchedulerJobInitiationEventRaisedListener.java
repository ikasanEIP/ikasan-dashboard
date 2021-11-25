package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;

public interface SchedulerJobInitiationEventRaisedListener {

    public void onSchedulerJobInitiationEventRaised(SchedulerJobInitiationEvent event);
}
