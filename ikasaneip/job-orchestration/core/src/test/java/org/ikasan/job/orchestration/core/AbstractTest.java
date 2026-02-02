package org.ikasan.job.orchestration.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextStartJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextTerminalJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.LocalEventJobInstanceImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.instance.model.SolrBridgingJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrLocalEventJobInstanceImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.junit.Assert;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return this.scheduledProcessEventInstance(jobName, agentName, isSuccessful, false);
    }


    /**
     * Creates a contextualized scheduled process event instance with the provided parameters.
     *
     * @param jobName the name of the job
     * @param agentName the name of the agent
     * @param isSuccessful true if the job was executed successfully, false otherwise
     * @param isRepeating true if the job is repeating, false otherwise
     * @return a ContextualizedScheduledProcessEventImpl instance representing the scheduled process event
     */
    protected ContextualisedScheduledProcessEventImpl scheduledProcessEventInstance(String jobName, String agentName
        , boolean isSuccessful, boolean isRepeating) {
        ContextualisedScheduledProcessEventImpl eventInstance = new ContextualisedScheduledProcessEventImpl();
        eventInstance.setJobName(jobName);
        eventInstance.setAgentName(agentName);
        eventInstance.setSuccessful(isSuccessful);

        InternalEventDrivenJobInstanceImpl internalEventDrivenJob = new InternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setIdentifier(agentName + "-" + jobName);
        internalEventDrivenJob.setJobRepeatable(isRepeating);

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
        contextParameter.setDefaultValue(value);

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
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(instance, "jobLockCacheDataMap");
        JobLockCacheData jobLockCacheData = jobLockCacheDataMap.get("environment");

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
     * Retrieves a specific JobLockCacheData object from the jobLockCacheDataMap based on the key "environment".
     * This method accesses the underlying ConcurrentHashMap containing JobLockCacheData objects via reflection.
     *
     * Note that this method is protected and should be used according to the access control restrictions of the class.
     */
    protected JobLockCacheData getJobLockCacheData() {
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(JobLockCacheImpl.instance(), "jobLockCacheDataMap");
        return jobLockCacheDataMap.get("environment");
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

    /**
     * Asserts that the status of a given context in a ContextMachine object matches the expected status.
     *
     * @param contextMachine The ContextMachine object that contains the context.
     * @param context The name of the context to check the status for.
     * @param expected The expected status of the context.
     */
    protected void assertContextStatus(ContextMachine contextMachine, String context, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getContextStatus(context);
        Assert.assertEquals(expected, status);
    }

    /**
     * Asserts that the status of a job in a given context in a ContextMachine object matches the expected status.
     *
     * @param contextMachine The ContextMachine object that contains the job.
     * @param context The name of the context where the job resides.
     * @param jobName The name of the job to check the status for.
     * @param expected The expected status of the job.
     */
    protected void assertJobStatus(ContextMachine contextMachine, String context, String jobName, InstanceStatus expected) {
        InstanceStatus status = contextMachine.getJobStatus(context, jobName);
        Assert.assertEquals(expected, status);
    }


    /**
     * Loads a map of ContextTerminalJobInstance objects based on the given context template and instance.
     *
     * @param  contextTemplate    The context template.
     * @param  contextInstance    The context instance.
     * @return A map of ContextTerminalJobInstance objects, where the key is the identifier of the job and child context name (if applicable), and the value is
     * the corresponding ContextTerminalJobInstance.
     */
    public Map<String, ContextTerminalJobInstance> loadContextTerminalJobInstanceMap(ContextTemplate contextTemplate, ContextInstance contextInstance) {
        Map<String, ContextTerminalJobInstance> contextStartJobInstanceMap = ContextHelper.getContextTerminalJobsFromContext(contextTemplate).stream()
            .map(localEventJob -> {
                ContextTerminalJobInstance contextStartJobInstance = new ContextTerminalJobInstanceImpl();
                contextStartJobInstance.setJobName(localEventJob.getJobName());
                contextStartJobInstance.setContextName(contextTemplate.getName());
                contextStartJobInstance.setContextInstanceId(contextInstance.getId());
                contextStartJobInstance.setOrdinal(localEventJob.getOrdinal());
                return contextStartJobInstance;
            }).collect(Collectors.toMap(SchedulerJobInstance::getIdentifier, Function.identity(), (key1, key2)-> key2));

        List<ContextTerminalJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            ContextTerminalJobInstance instance = contextStartJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                ContextTerminalJobInstance contextualisedInstance = (ContextTerminalJobInstance) SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Loads a map of ContextStartJobInstance objects based on the given context template and instance.
     *
     * @param  contextTemplate   The context template.
     * @param  contextInstance   The context instance.
     * @return A map of ContextStartJobInstance objects, where the key is the identifier of the job and child context name (if applicable),
     *         and the value is the corresponding ContextStartJobInstance.
     */
    public Map<String, ContextStartJobInstance> loadContextStartJobInstanceMap(ContextTemplate contextTemplate, ContextInstance contextInstance) {
        Map<String, ContextStartJobInstance> contextStartJobInstanceMap = ContextHelper.getContextStartJobsFromContext(contextTemplate).stream()
            .map(localEventJob -> {
                ContextStartJobInstance contextStartJobInstance = new ContextStartJobInstanceImpl();
                contextStartJobInstance.setJobName(localEventJob.getJobName());
                contextStartJobInstance.setContextName(contextTemplate.getName());
                contextStartJobInstance.setContextInstanceId(contextInstance.getId());
                contextStartJobInstance.setOrdinal(localEventJob.getOrdinal());
                return contextStartJobInstance;
            }).collect(Collectors.toMap(SchedulerJobInstance::getIdentifier, Function.identity(), (key1, key2)-> key2));

        List<ContextStartJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            ContextStartJobInstance instance = contextStartJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                ContextStartJobInstance contextualisedInstance = (ContextStartJobInstance) SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Loads and returns a map of LocalEventJobInstance objects.
     * The map is populated based on the given ContextTemplate and ContextInstance.
     *
     * @param contextTemplate The context template used to populate the map.
     * @param contextInstance The context instance used to populate the map.
     * @return A map of LocalEventJobInstance objects, where the key is the identifier concatenated with the child context name,
     *         and the value is the LocalEventJobInstance object.
     */
    public Map<String, LocalEventJobInstance> loadLocalEventJobInstanceMap(ContextTemplate contextTemplate, ContextInstance contextInstance) {
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = ContextHelper.getLocalEventJobsFromContext(contextTemplate).stream()
            .map(localEventJob -> {
                LocalEventJobInstance contextStartJobInstance = new LocalEventJobInstanceImpl();
                contextStartJobInstance.setJobName(localEventJob.getJobName());
                contextStartJobInstance.setContextName(contextTemplate.getName());
                contextStartJobInstance.setContextInstanceId(contextInstance.getId());
                contextStartJobInstance.setOrdinal(localEventJob.getOrdinal());
                return contextStartJobInstance;
            }).collect(Collectors.toMap(SchedulerJobInstance::getIdentifier, Function.identity(), (key1, key2)-> key2));

        List<LocalEventJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            LocalEventJobInstance instance = localEventJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                LocalEventJobInstance contextualisedInstance = (LocalEventJobInstance) SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    /**
     * Loads a map of InternalEventDrivenJobInstance objects based on the given context instance, directory, and jobsBase.
     *
     * @param contextInstance The context instance.
     * @param directory The directory where the job instances are stored.
     * @param jobsBase The base directory for the jobs.
     * @return A map of InternalEventDrivenJobInstance objects, where the key is the identifier of the job and child context name (if applicable),
     *         and the value is the corresponding InternalEventDrivenJobInstance.
     * @throws IOException If an I/O error occurs while loading the job instances.
     */
    public Map<String, InternalEventDrivenJobInstance> loadInternalEventDrivenJobInstanceMap(ContextInstance contextInstance, String directory, String jobsBase) throws IOException {
        List<InternalEventDrivenJobInstance> files = Files.list(Path.of(directory)).map(path -> {
            InternalEventDrivenJobInstance internalEventDrivenJob = null;
            try {
                String jobJson = loadDataFile(jobsBase + FileSystems.getDefault().getSeparator() + path.toFile().getName());
                internalEventDrivenJob = objectMapper.readValue(jobJson, InternalEventDrivenJobInstanceImpl.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return internalEventDrivenJob;
        }).collect(Collectors.toList());

        Map<String, InternalEventDrivenJobInstance> schedulerJobInstanceMap =  files.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (job1, job2) -> job1));

        List<InternalEventDrivenJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            InternalEventDrivenJobInstance instance = schedulerJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                InternalEventDrivenJobInstance contextualisedInstance = (InternalEventDrivenJobInstance) SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }

    public Map<String, BridgingJobInstance> loadBridgingJobInstanceMap(ContextTemplate contextTemplate, ContextInstance contextInstance) {
        Map<String, BridgingJobInstance> contextStartJobInstanceMap = ContextHelper.getBridgingJobsFromContext(contextTemplate).stream()
            .map(localEventJob -> {
                BridgingJobInstance bridgingJobInstance = new SolrBridgingJobInstanceImpl();
                bridgingJobInstance.setJobName(localEventJob.getJobName());
                bridgingJobInstance.setContextName(contextTemplate.getName());
                bridgingJobInstance.setContextInstanceId(contextInstance.getId());
                bridgingJobInstance.setOrdinal(localEventJob.getOrdinal());
                return bridgingJobInstance;
            }).collect(Collectors.toMap(SchedulerJobInstance::getIdentifier, Function.identity(), (key1, key2)-> key2));

        List<BridgingJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

        contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
            BridgingJobInstance instance = contextStartJobInstanceMap.get(schedulerJobInstance.getIdentifier());

            if(instance != null) {
                BridgingJobInstance contextualisedInstance = (BridgingJobInstance) SerializationUtils.clone(instance);
                contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                contextualisedInstance.setContextInstanceId(contextInstance.getId());

                contextualisedSchedulerJobInstances.add(contextualisedInstance);
            }
        });

        return contextualisedSchedulerJobInstances.stream()
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (job1, job2) -> job1));
    }
}
