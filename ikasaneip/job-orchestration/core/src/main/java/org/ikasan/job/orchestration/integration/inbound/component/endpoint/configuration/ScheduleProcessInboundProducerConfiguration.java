package org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration;

public class ScheduleProcessInboundProducerConfiguration {
    private boolean ignoreErrors = false;
    private boolean logDetails = false;

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean isLogDetails() {
        return logDetails;
    }

    public void setLogDetails(boolean logDetails) {
        this.logDetails = logDetails;
    }
}
