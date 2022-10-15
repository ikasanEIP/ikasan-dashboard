package org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration;

public class ScheduleProcessInboundProducerConfiguration {
    private boolean ignoreErrors = false;

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
}
