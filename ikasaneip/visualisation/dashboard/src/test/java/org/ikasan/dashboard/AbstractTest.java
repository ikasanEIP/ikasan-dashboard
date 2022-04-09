package org.ikasan.dashboard;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AbstractTest
{
    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        return eventInstance;
    }

    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String contextId, String childContextId
        , String jobName, String agentName, boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setContextId(contextId);
        eventInstance.setChildContextIds(List.of(childContextId));
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        return eventInstance;
    }

    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String contextId, List<String> childContextIds
        , String jobName, String agentName, boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setContextId(contextId);
        eventInstance.setChildContextIds(childContextIds);
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        return eventInstance;
    }


    protected ContextParameter getContextParameter(String name, String type) {
        ContextParameterImpl contextParameter = new ContextParameterImpl();
        contextParameter.setName(name);
        contextParameter.setType(type);

        return contextParameter;
    }

}
