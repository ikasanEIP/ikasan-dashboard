package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import java.io.IOException;

public interface ScheduledProcessProducerConnectionCallback {

    /**
     * The execute method on the callback.
     *
     * @throws IOException
     */
    public void execute() throws IOException;
}
