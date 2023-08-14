package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ScheduledProcessProducerConnectionCallbackImpl implements ScheduledProcessProducerConnectionCallback {
    private Logger logger = LoggerFactory.getLogger(ScheduledProcessProducerConnectionCallbackImpl.class);

    private String payload;
    private ContextMachine contextMachine;

    public ScheduledProcessProducerConnectionCallbackImpl(String payload, ContextMachine contextMachine) {
        this.payload = payload;
        this.contextMachine = contextMachine;
    }

    @Override
    public void execute() throws IOException {
        // check to make sure the context machine exist to avoid null pointer exception at the point of commit
        if(this.contextMachine != null && this.contextMachine.getContext() != null && ContextMachineCache.instance()
            .containsInstanceIdentifier(this.contextMachine.getContext().getId())) {
            this.contextMachine = ContextMachineCache.instance().getByContextInstanceId(this.contextMachine.getContext().getId());
            if (this.contextMachine != null) {
                this.contextMachine.eventReceived(payload);
            }
        }
        else {
            logger.info(String.format("Context machine not available in cache. Ignoring payload!. Payload[%s]", payload));
        }
    }

    @Override
    public String getPayload() {
        return payload;
    }
}
