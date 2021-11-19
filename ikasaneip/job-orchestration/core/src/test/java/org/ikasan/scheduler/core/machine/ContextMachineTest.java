package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.ScheduledProcessEventInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ContextMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_context_machine() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        ContextMachine contextMachine  = new ContextMachine(context);

        ScheduledProcessEventInstance eventInstance = new ScheduledProcessEventInstance();
        eventInstance.setAgentName("agentName1");
        eventInstance.setJobName("jobName1blah");

        List<SchedulerJobInitiationEvent> events = contextMachine.eventReceived(eventInstance);
    }

}
