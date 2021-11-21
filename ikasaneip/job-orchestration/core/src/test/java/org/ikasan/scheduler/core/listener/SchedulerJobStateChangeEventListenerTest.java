package org.ikasan.scheduler.core.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ScheduledProcessEventInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class SchedulerJobStateChangeEventListenerTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test_context_machine_full_nested_context_success() throws IOException, JSONException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context);
        contextMachine.addSchedulerJobStateChangeEventListener(event -> {
            Assert.assertNotNull(event);

        });
        contextMachine.addSchedulerJobStateChangeEventListener(event -> {
            Assert.assertNotNull(event);
        });

        ScheduledProcessEventInstance eventInstance = scheduledProcessEventInstance("jobName3",
            "agentName3", true);

        contextMachine.eventReceived(eventInstance);
    }
}
