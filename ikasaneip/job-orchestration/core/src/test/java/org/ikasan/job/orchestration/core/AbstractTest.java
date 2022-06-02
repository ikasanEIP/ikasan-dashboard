package org.ikasan.job.orchestration.core;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerOverrider;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractTest
{
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private final SchedulerOverrider schedulerOverrider = new SchedulerOverrider(false, null, false, null);
    private final ContextParametersFactory contextParametersFactory = new ContextParametersFactory(schedulerOverrider);
    protected final ContextParametersInstanceService contextParametersInstanceService = new ContextParametersInstanceServiceImpl(contextParametersFactory);

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

    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful, String childContextId) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);
        eventInstance.setChildContextIds(List.of(childContextId));

        return eventInstance;
    }

    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstanceStarting(String jobName, String agentName
        , boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);
        eventInstance.setJobStarting(true);

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

    protected void validateAllLocksCleared() {
        JobLockCacheImpl instance = JobLockCacheImpl.instance();
        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByIdentifier
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(instance, "jobLocksByIdentifier");

        ConcurrentHashMap<String, JobLockHolderImpl> jobLocksByLockName
            = (ConcurrentHashMap<String, JobLockHolderImpl>) ReflectionTestUtils.getField(instance, "jobLocksByLockName");

        assertNotNull(jobLocksByIdentifier);
        Collection<JobLockHolderImpl> jobLockHolders = jobLocksByIdentifier.values();
        assertTrue(jobLockHolders.size() > 0);
        for (JobLockHolderImpl jlh : jobLockHolders) {
            assertEquals(0, jlh.getLockHolders().size());
        }

        assertNotNull(jobLocksByLockName);
        assertTrue(jobLocksByLockName.values().size() > 0);
        jobLockHolders = jobLocksByLockName.values();
        for (JobLockHolderImpl jlh : jobLockHolders) {
            assertEquals(0, jlh.getLockHolders().size());
        }
    }

    protected void printContext(ContextMachine contextMachine) throws JsonProcessingException {
        System.out.println(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()));
    }

    protected void printLocks() throws JsonProcessingException {
        System.out.println(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(JobLockCacheImpl.instance()));
    }
}
