package org.ikasan.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.rest.client.JobProvisionRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
// todo fix test
public class JobProvisionRestServiceImplTest extends AbstractTest{
    @Mock
    Environment environment;

    private ObjectMapper objectMapper = new ObjectMapper();

    public static final String DASHBOARD_BASE_URL_PROPERTY="ikasan.dashboard.base.url";
    public static final String DASHBOARD_USERNAME_PROPERTY="ikasan.dashboard.rest.username";
    public static final String DASHBOARD_PASSWORD_PROPERTY="ikasan.dashboard.rest.password";
    public static final String DASHBOARD_REST_USERAGENT ="ikasan.dashboard.rest.useragent";

    @Test
    public void test_large_number_of_jobs() throws IOException {

        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn("http://localhost:9090");
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");



        JobProvisionRestServiceImpl jobProvisionService = new JobProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/jobs");

        List<SchedulerJob> schedulerJobs = new ArrayList<>();

        IntStream.range(0, 34).forEach(i -> schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "scheduler-agent", "contextId", "description", "jobName"+i, getContextParameters(), List.of("1"))));

        IntStream.range(0, 33).forEach(i -> schedulerJobs.add(this.createInternalEventDrivenJob("commandLine", "workingDirectory"
            , "scheduler-agent-2", "contextId", "description", "jobName"+i, getContextParameters(), List.of("1"))));

        IntStream.range(0, 8).forEach(i -> schedulerJobs.add(this.createQuartzScheduleDrivenJob("scheduler-agent", "contextId", "description"
            , "quartz-jobName"+i, "jobGroup", "* 0/15 * ? * * *", "timezone")));

        IntStream.range(0, 20).forEach(i -> schedulerJobs.add(this.createQuartzScheduleDrivenJob("scheduler-agent-2", "contextId", "description"
            , "quartz-jobName"+i, "jobGroup", "* 0/15 * ? * * *", "timezone")));

        IntStream.range(0, 20).forEach(i -> schedulerJobs.add(this.createFileEventDrivenJob("scheduler-agent", "contextId", "description"
            , "file-jobName"+i, "jobGroup", "* 0/15 * ? * * *", "timezone")));

        IntStream.range(0, 50).forEach(i -> schedulerJobs.add(this.createFileEventDrivenJob("scheduler-agent-2", "contextId", "description"
            , "file-jobName"+i, "jobGroup", "* 0/15 * ? * * *", "timezone")));



        SchedulerJobWrapper wrapper = new SchedulerJobWrapperImpl();
        wrapper.setJobs(schedulerJobs);
        jobProvisionService.provisionJobs(wrapper);
    }

    @Test
    public void test_jobs() throws IOException {

        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn("http://localhost:9090");
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        JobProvisionRestServiceImpl jobProvisionService = new JobProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/jobs");

        List<SchedulerJob> schedulerJobs = new ArrayList<>();

        IntStream.range(0, 1).forEach(i -> schedulerJobs.add(this.createFileEventDrivenJob("scheduler-agent", "test-context", "Detects the arrival of a file every minute."
            , "File Arrive Job", "jobGroup", "0 0/1 * ? * * *", "")));

        IntStream.range(0, 10).forEach(i -> schedulerJobs.add(this.createInternalEventDrivenJob("ls -la", "."
            , "scheduler-agent", "test-context", "Simple job to perform a directory listing.", "job-"+i, getContextParameters(), List.of())));

        IntStream.range(0, 10).forEach(i -> schedulerJobs.add(this.createInternalEventDrivenJob("pwd", "."
            , "scheduler-agent-2", "test-context", "Simple job to discover the present working directory.", "job-"+i, getContextParameters(), List.of())));

        SchedulerJobWrapper wrapper = new SchedulerJobWrapperImpl();
        wrapper.setJobs(schedulerJobs);
        jobProvisionService.provisionJobs(wrapper);
    }

    @Test
    public void sample_provision() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn("http://localhost:9090");
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        JobProvisionRestServiceImpl jobProvisionService = new JobProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/jobs");


        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        List<SchedulerJob> jobs = new ArrayList<>();

        Files.list(Path.of("./src/test/resources/data/SAMPLE_CONTEXT/jobs"))
            .forEach(dir -> {
                try {
                    jobs.add(objectMapper
                        .readValue(super.loadDataFile("/data/SAMPLE_CONTEXT/jobs/"+dir.getFileName()), InternalEventDrivenJobImpl.class));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });

        Files.list(Path.of("./src/test/resources/data/SAMPLE_CONTEXT/quartz"))
            .forEach(dir -> {
                try {
                    jobs.add(objectMapper
                        .readValue(super.loadDataFile("/data/SAMPLE_CONTEXT/quartz/"+dir.getFileName()), QuartzScheduleDrivenJobImpl.class));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });

        SchedulerJobWrapper wrapper = new SchedulerJobWrapperImpl();
        wrapper.setJobs(jobs);
        jobProvisionService.provisionJobs(wrapper);
    }

    private InternalEventDrivenJob createInternalEventDrivenJob(String commandLine, String workingDirectory
        , String agentName, String contextId, String description, String jobName, List<ContextParameter> contextParameters
        , List<String> successfulReturnCodes) {
        InternalEventDrivenJobBuilder fileEventDrivenJobBuilder = new InternalEventDrivenJobBuilder();
        fileEventDrivenJobBuilder
            .withCommandLine(commandLine)
            .withMaxExecutionTime(100000L)
            .withMinExecutionTime(1000L)
            .withWorkingDirectory(workingDirectory)
            .addSuccessfulReturnCode("0")
            .withAgentName(agentName)
            .withContextId(contextId)
            .withDescription(description)
            .withJobName(jobName);

        contextParameters.forEach(contextParameter
            -> fileEventDrivenJobBuilder.addContextParameter(contextParameter));
        successfulReturnCodes.forEach(successfulReturnCode
            -> fileEventDrivenJobBuilder.addSuccessfulReturnCode(successfulReturnCode));

        return fileEventDrivenJobBuilder.build();
    }

    private QuartzScheduleDrivenJob createQuartzScheduleDrivenJob(String agentName, String contextId, String description, String jobName, String jobGroup
        , String cronExpression, String timezone) {
        QuartzScheduleDrivenJobBuilder quartzScheduleDrivenJobBuilder = new QuartzScheduleDrivenJobBuilder();

        Map<String, String> passthrough = new HashMap<>();
        passthrough.put("test", "test");
        quartzScheduleDrivenJobBuilder.withCronExpression(cronExpression)
            .withTimeZone(timezone)
            .withJobGroup(jobGroup)
            .withPassthroughProperties(passthrough)
            .withDescription(description)
            .withJobName(jobName)
            .withContextId(contextId)
            .withAgentName(agentName)
            .withStartupControlType("MANUAL");

        return quartzScheduleDrivenJobBuilder.build();
    }

    private FileEventDrivenJob createFileEventDrivenJob(String agentName, String contextId, String description, String jobName, String jobGroup
        , String cronExpression, String timezone) {
        FileEventDrivenJobBuilder fileEventDrivenJobBuilder = new FileEventDrivenJobBuilder();

        ArrayList files = new ArrayList();
        files.add("/sandbox/mick/test.txt");

        Map<String, String> passthrough = new HashMap<>();
        passthrough.put("test", "test");

        fileEventDrivenJobBuilder
            .withFilePath("/sandbox/mick/test.txt")
            .withFilenames(files)
            .withCronExpression(cronExpression)
            .withTimeZone(timezone)
            .withJobGroup(jobGroup)
            .withPassthroughProperties(passthrough)
            .withDescription(description)
            .withJobName(jobName)
            .withContextId(contextId)
            .withAgentName(agentName);

        return fileEventDrivenJobBuilder.build();
    }

    private List<ContextParameter> getContextParameters() {
        ArrayList<ContextParameter> contextParameters = new ArrayList<>();

        IntStream.range(0, 10).forEach(i -> {
            ContextParameterBuilder contextParameterBuilder = new ContextParameterBuilder();
            contextParameterBuilder.withName("param-name-"+i);
            contextParameterBuilder.withType("java.lang.String");

            contextParameters.add(contextParameterBuilder.build());
        });

        return contextParameters;
    }

    private List<SolrConfigurationParameterMetaData> getConfigurationParameterMetaDataList() {
        List<SolrConfigurationParameterMetaData> solrConfigurationParameterMetaDataList = new ArrayList<>();

        SolrConfigurationParameterMetaData solrConfigurationParameterMetaData = new SolrConfigurationParameterMetaData();
        solrConfigurationParameterMetaData.setName("flowDefinitions");
        solrConfigurationParameterMetaData.setValue(new HashMap<String, String>());
        solrConfigurationParameterMetaData.setImplementingClass(Map.class.getName());
        solrConfigurationParameterMetaData.setDescription("description");

        solrConfigurationParameterMetaDataList.add(solrConfigurationParameterMetaData);

        solrConfigurationParameterMetaData = new SolrConfigurationParameterMetaData();
        solrConfigurationParameterMetaData.setName("flowDefinitionProfiles");
        solrConfigurationParameterMetaData.setValue(new HashMap<String, String>());
        solrConfigurationParameterMetaData.setImplementingClass(Map.class.getName());
        solrConfigurationParameterMetaData.setDescription("description");

        solrConfigurationParameterMetaDataList.add(solrConfigurationParameterMetaData);

        return solrConfigurationParameterMetaDataList;
    }
}
