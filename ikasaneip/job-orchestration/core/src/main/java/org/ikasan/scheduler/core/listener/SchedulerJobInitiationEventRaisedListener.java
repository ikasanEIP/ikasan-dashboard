package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEventImpl;

public interface SchedulerJobInitiationEventRaisedListener {

    void onSchedulerJobInitiationEventRaised(SchedulerJobInitiationEventImpl event);
}
