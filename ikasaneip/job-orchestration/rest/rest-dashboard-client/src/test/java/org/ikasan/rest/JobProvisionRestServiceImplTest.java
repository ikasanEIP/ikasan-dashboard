package org.ikasan.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.rest.client.DashboardRestClientException;
import org.ikasan.job.orchestration.rest.client.JobProvisionRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobProvisionRestServiceImplTest extends AbstractTest{

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private String contextBaseUrl;

    private ObjectMapper objectMapper;

    @Mock
    Environment environment;

    @Before
    public void setup() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();

        objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        contextBaseUrl = "http://localhost:" + wireMockRule.port();
    }

    @Test
    public void test_success_provision_jobs() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        JobProvisionRestServiceImpl jobProvisionService = new JobProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/jobs");

        List<SchedulerJob> schedulerJobs = this.createSchedulerJobs();


        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        schedulerJobWrapper.setJobs(schedulerJobs);

        String json = objectMapper.writeValueAsString(schedulerJobWrapper);

        stubFor(put(urlEqualTo("/rest/provision/jobs"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(aResponse()
                .withStatus(200)
            ));

        jobProvisionService.provisionJobs(schedulerJobs);

        verify(putRequestedFor(urlEqualTo("/rest/provision/jobs"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json)));
    }

    @Test(expected = DashboardRestClientException.class)
    public void test_exception_provision_jobs() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        JobProvisionRestServiceImpl jobProvisionService = new JobProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/jobs");

        List<SchedulerJob> schedulerJobs = this.createSchedulerJobs();


        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        schedulerJobWrapper.setJobs(schedulerJobs);

        String json = objectMapper.writeValueAsString(schedulerJobWrapper);

        stubFor(put(urlEqualTo("/rest/provision/jobs"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(aResponse()
                .withStatus(400)
            ));

        jobProvisionService.provisionJobs(schedulerJobs);
    }

    private List<SchedulerJob> createSchedulerJobs() {
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

        return schedulerJobs;
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
            .withContextName(contextId)
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
            .withContextName(contextId)
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
            .withContextName(contextId)
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
}
