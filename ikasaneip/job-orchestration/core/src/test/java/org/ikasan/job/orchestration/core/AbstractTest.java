package org.ikasan.job.orchestration.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * This class provides common utility methods and helper functions for test classes in the project.
 */
public class AbstractTest
{
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
    private final SchedulerContextParametersPropertiesProvider schedulerContextParametersPropertiesProvider = new SchedulerContextParametersPropertiesProvider( jobContextParamsSetupConfiguration, null);
    private final ContextParametersFactory contextParametersFactory = new ContextParametersFactory(schedulerContextParametersPropertiesProvider);
    protected final ContextParametersInstanceService contextParametersInstanceService = new ContextParametersInstanceServiceImpl(contextParametersFactory);

    /**
     * Loads the content from a file and returns it as a string.
     *
     * @param fileName The name of the file to load.
     * @return The content of the file as a string.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    /**
     * Loads a data file as an input stream.
     *
     * @param fileName The name of the data file to load.
     * @return An InputStream representing the data file.
     * @throws IOException If an error occurs while loading the data file.
     */
    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    /**
     * Creates an instance of {@link ContextualisedScheduledProcessEventImpl}.
     *
     * @param jobName      The name of the job associated with the event.
     * @param agentName    The name of the agent associated with the event.
     * @param isSuccessful The success status of the event.
     * @return An instance of {@link ContextualisedScheduledProcessEventImpl} with the specified parameters set.
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);

        eventInstance.setInternalEventDrivenJob(internalEventDrivenJob);

        return eventInstance;
    }

    /**
     * Creates a new instance of ContextualisedScheduledProcessEventImpl with the given parameters.
     *
     * @param jobName       the name of the job
     * @param agentName     the name of the agent
     * @param isSuccessful  true if the event is successful, otherwise false
     * @param childContextId the child context ID
     * @return a new instance of ContextualisedScheduledProcessEventImpl with the provided parameters
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful, String childContextId) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);
        eventInstance.setChildContextNames(List.of(childContextId));

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);

        eventInstance.setInternalEventDrivenJob(internalEventDrivenJob);

        return eventInstance;
    }

    /**
     * Creates a ContextualisedScheduledProcessEventImpl object for a starting event instance.
     *
     * @param jobName     The name of the job.
     * @param agentName   The name of the agent.
     * @param isSuccessful Whether the job is starting successfully or not.
     * @return A ContextualisedScheduledProcessEventImpl object.
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstanceStarting(String jobName, String agentName
        , boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);
        eventInstance.setJobStarting(true);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);

        eventInstance.setInternalEventDrivenJob(internalEventDrivenJob);

        return eventInstance;
    }

    /**
     * Creates an instance of ContextualisedScheduledProcessEventImpl with the given parameters.
     *
     * @param contextId      The ID of the context.
     * @param childContextId The ID of the child context.
     * @param jobName        The name of the job.
     * @param agentName      The name of the agent.
     * @param isSuccessful   Indicates whether the event is successful or not.
     * @return The created instance of ContextualisedScheduledProcessEventImpl.
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String contextId, String childContextId
        , String jobName, String agentName, boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setContextName(contextId);
        eventInstance.setChildContextNames(List.of(childContextId));
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);

        eventInstance.setInternalEventDrivenJob(internalEventDrivenJob);

        return eventInstance;
    }

    /**
     * Creates a new instance of {@link ContextualisedScheduledProcessEventImpl} to represent a scheduled event process.
     *
     * @param contextId The ID of the context.
     * @param childContextIds The list of child context IDs.
     * @param jobName The name of the job.
     * @param agentName The name of the agent.
     * @param isSuccessful Indicates whether the event process was successful or not.
     * @return The newly created {@link ContextualisedScheduledProcessEventImpl} instance.
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String contextId, List<String> childContextIds
        , String jobName, String agentName, boolean isSuccessful) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setContextName(contextId);
        eventInstance.setChildContextNames(childContextIds);
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);

        eventInstance.setInternalEventDrivenJob(internalEventDrivenJob);

        return eventInstance;
    }


    /**
     * Returns a new instance of ContextParameterImpl with the specified name and default value.
     *
     * @param name The name of the context parameter
     * @param value The default value of the context parameter
     * @return A new instance of ContextParameterImpl with the specified name and default value
     */
    protected ContextParameter getContextParameter(String name, String value) {
        ContextParameterImpl contextParameter = new ContextParameterImpl();
        contextParameter.setName(name);
        contextParameter.setDefaultValue(value  );

        return contextParameter;
    }

    /**
     *
     * This method is used to validate that all locks have been cleared in the JobLockCache.
     * It retrieves the instance of the JobLockCacheImpl class and obtains the JobLockCacheData object.
     * It then checks the jobLocksByIdentifier and jobLocksByLockName ConcurrentHashMaps for null values.
     * Next, it verifies that the jobLocksByLockName ConcurrentHashMap contains at least one value.
     * Finally, it iterates through the JobLockHolder objects in the jobLocksByLockName ConcurrentHashMap
     * and checks that the lock holders list is empty for each JobLockHolder.
     *
     * @throws AssertionError if any of the validations fail
     */
    protected void validateAllLocksCleared() {
        JobLockCacheImpl instance = JobLockCacheImpl.instance();
        JobLockCacheData jobLockCacheData = (JobLockCacheData) ReflectionTestUtils.getField(instance, "jobLockCacheData");

        ConcurrentHashMap<String, String> jobLocksByIdentifier
            = jobLockCacheData.getJobLocksByIdentifier();

        ConcurrentHashMap<String, JobLockHolder> jobLocksByLockName
            = jobLockCacheData.getJobLocksByLockName();

        assertNotNull(jobLocksByIdentifier);

        assertNotNull(jobLocksByLockName);
        assertTrue(jobLocksByLockName.values().size() > 0);
        Collection<JobLockHolder> jobLockHolders = jobLocksByLockName.values();
        for (JobLockHolder jlh : jobLockHolders) {
            assertEquals(0, jlh.getLockHolders().size());
        }
    }

    /**
     * Prints the context of a ContextMachine object to the console as JSON.
     *
     * @param contextMachine The ContextMachine object whose context will be printed.
     * @throws JsonProcessingException if an error occurs while processing the JSON.
     */
    protected void printContext(ContextMachine contextMachine) throws JsonProcessingException {
        System.out.println(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachine.getContextInstanceStatus()));
    }

    /**
     * Print the locks in JSON format.
     *
     * @throws JsonProcessingException if an error occurs while processing the JSON
     */
    protected void printLocks() throws JsonProcessingException {
        System.out.println(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(JobLockCacheImpl.instance()));
    }
}
