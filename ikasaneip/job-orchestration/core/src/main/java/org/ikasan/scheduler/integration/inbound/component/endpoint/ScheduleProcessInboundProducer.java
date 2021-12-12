package org.ikasan.scheduler.integration.inbound.component.endpoint;

import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.component.endpoint.Producer;

import java.io.IOException;

public class ScheduleProcessInboundProducer implements Producer<String> {

    @Override
    public void invoke(String payload) throws EndpointException {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName("test");

        if(contextMachine != null) {
            try {
                contextMachine.eventReceived(payload);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
