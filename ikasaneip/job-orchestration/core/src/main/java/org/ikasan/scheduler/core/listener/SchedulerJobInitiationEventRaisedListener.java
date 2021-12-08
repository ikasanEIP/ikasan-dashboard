package org.ikasan.scheduler.core.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEventImpl;

public interface SchedulerJobInitiationEventRaisedListener {

    void onSchedulerJobInitiationEventRaised(SchedulerJobInitiationEventImpl event) throws JsonProcessingException;
}
