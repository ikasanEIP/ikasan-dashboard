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
        this.contextMachine.eventReceived(payload);
    }
}
