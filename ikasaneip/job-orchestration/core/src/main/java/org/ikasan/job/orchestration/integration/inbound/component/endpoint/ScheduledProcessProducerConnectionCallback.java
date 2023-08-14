package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import java.io.IOException;

public interface ScheduledProcessProducerConnectionCallback {

    /**
     * The execute method on the callback.
     *
     * @throws IOException
     */
    void execute() throws IOException;

    /**
     * Get the payload associated with the callback.
     * @return
     */
    String getPayload();
}
