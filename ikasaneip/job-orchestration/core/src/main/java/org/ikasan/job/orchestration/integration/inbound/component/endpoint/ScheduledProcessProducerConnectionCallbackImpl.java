package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import org.ikasan.job.orchestration.core.machine.ContextMachine;

import java.io.IOException;

public class ScheduledProcessProducerConnectionCallbackImpl implements ScheduledProcessProducerConnectionCallback {
    private String payload;
    private ContextMachine contextMachine;

    public ScheduledProcessProducerConnectionCallbackImpl(String payload, ContextMachine contextMachine) {
        this.payload = payload;
        this.contextMachine = contextMachine;
    }

    @Override
    public void execute() throws IOException {
        // check to make sure the context machine exist to avoid null pointer exception at the point of commit
        if (this.contextMachine != null) {
            this.contextMachine.eventReceived(payload);
        }
    }
}
