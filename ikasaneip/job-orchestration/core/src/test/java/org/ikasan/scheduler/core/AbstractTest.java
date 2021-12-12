package org.ikasan.scheduler.core;

import org.apache.commons.io.IOUtils;
import org.ikasan.scheduler.core.model.instance.ContextualisedScheduledProcessEventInstance;

import java.io.IOException;
import java.io.InputStream;

public class AbstractTest
{
    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    protected ContextualisedScheduledProcessEventInstance scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful) {
        ContextualisedScheduledProcessEventInstance eventInstance = new ContextualisedScheduledProcessEventInstance();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        return eventInstance;
    }

}
