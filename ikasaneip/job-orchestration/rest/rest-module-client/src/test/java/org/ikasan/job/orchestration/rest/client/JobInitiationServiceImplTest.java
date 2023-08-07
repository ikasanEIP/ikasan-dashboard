package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.rest.client.dto.JobDryRunModeDto;
import org.ikasan.job.orchestration.rest.client.dto.SchedulerJobInitiationEventDto;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class JobInitiationServiceImplTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private JobInitiationServiceImpl uut;

    private String contextBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new JobInitiationServiceImpl(environment, new HttpComponentsClientHttpRequestFactory(), 3);
    }

    @Test
    public void test_scheduler_job_initiation_event_initiation_success() throws IOException {
        SchedulerJobInitiationEventDto schedulerJobInitiationEvent = new SchedulerJobInitiationEventDto();

        String json = objectMapper.writeValueAsString(schedulerJobInitiationEvent);
        stubFor(put(urlEqualTo("/rest/schedulerJobInitiation"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        uut.raiseSchedulerJobInitiationEvent(contextBaseUrl, schedulerJobInitiationEvent);

        verify(putRequestedFor(urlEqualTo("/rest/schedulerJobInitiation"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json)));
    }

    @Test(expected = RestClientException.class)
    public void test_scheduler_job_initiation_event_initiation_exception_thrown() throws IOException {
        SchedulerJobInitiationEventDto schedulerJobInitiationEvent = new SchedulerJobInitiationEventDto();

        String json = objectMapper.writeValueAsString(schedulerJobInitiationEvent);
        stubFor(put(urlEqualTo("/rest/schedulerJobInitiation"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(403)));

        uut.raiseSchedulerJobInitiationEvent(contextBaseUrl, schedulerJobInitiationEvent);
    }

    @Test
    public void test_submit_quartz_job_success() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(200)));


        uut.raiseQuartzSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");

        verify(getRequestedFor(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test(expected = RestClientException.class)
    public void test_exception_raise_quartz_job() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(400)));


        uut.raiseQuartzSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");
    }

    @Test
    public void test_submit_file_job_success() throws JsonProcessingException, InterruptedException {
        JobDryRunModeDto jobDryRunTrue = new JobDryRunModeDto();
        jobDryRunTrue.setJobName("jobName_contextName");
        jobDryRunTrue.setIsDryRun(true);

        JobDryRunModeDto jobDryRunFalse = new JobDryRunModeDto();
        jobDryRunFalse.setJobName("jobName_contextName");
        jobDryRunFalse.setIsDryRun(false);

        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(200)));

        String jobDryRunTrueJson = objectMapper.writeValueAsString(jobDryRunTrue);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunTrueJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        String jobDryRunFalseJson = objectMapper.writeValueAsString(jobDryRunFalse);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunFalseJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        uut.raiseFileEventSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");

        Thread.sleep(5000);
        verify(getRequestedFor(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));

        verify(putRequestedFor(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunTrueJson)));

        verify(putRequestedFor(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunFalseJson)));
    }

    @Test(expected = RestClientException.class)
    public void test_submit_file_job_exception_set_job_dry_run_true() throws JsonProcessingException
    {
        JobDryRunModeDto jobDryRunTrue = new JobDryRunModeDto();
        jobDryRunTrue.setJobName("jobName_contextName");
        jobDryRunTrue.setIsDryRun(true);

        JobDryRunModeDto jobDryRunFalse = new JobDryRunModeDto();
        jobDryRunFalse.setJobName("jobName_contextName");
        jobDryRunFalse.setIsDryRun(false);

        String jobDryRunTrueJson = objectMapper.writeValueAsString(jobDryRunTrue);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunTrueJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(403)));

        uut.raiseFileEventSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");
    }

    @Test(expected = RestClientException.class)
    public void test_submit_file_job_exception_trigger_job() throws JsonProcessingException
    {
        JobDryRunModeDto jobDryRunTrue = new JobDryRunModeDto();
        jobDryRunTrue.setJobName("jobName_contextName");
        jobDryRunTrue.setIsDryRun(true);

        JobDryRunModeDto jobDryRunFalse = new JobDryRunModeDto();
        jobDryRunFalse.setJobName("jobName_contextName");
        jobDryRunFalse.setIsDryRun(false);

        String jobDryRunTrueJson = objectMapper.writeValueAsString(jobDryRunTrue);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunTrueJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(403)));

        uut.raiseFileEventSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");
    }

    @Test
    public void test_submit_file_job_exception_set_job_dry_run_false() throws JsonProcessingException, InterruptedException {
        JobDryRunModeDto jobDryRunTrue = new JobDryRunModeDto();
        jobDryRunTrue.setJobName("jobName_contextName");
        jobDryRunTrue.setIsDryRun(true);

        JobDryRunModeDto jobDryRunFalse = new JobDryRunModeDto();
        jobDryRunFalse.setJobName("jobName_contextName");
        jobDryRunFalse.setIsDryRun(false);

        String jobDryRunTrueJson = objectMapper.writeValueAsString(jobDryRunTrue);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunTrueJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/agentName/jobName_contextName/correlationId"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(200)));

        String jobDryRunFalseJson = objectMapper.writeValueAsString(jobDryRunFalse);
        stubFor(put(urlEqualTo("/rest/dryRun/jobmode"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(jobDryRunFalseJson))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(403)));

        uut.raiseFileEventSchedulerJob(contextBaseUrl, "agentName", this.getJob(), "correlationId");

        Thread.sleep(5000);
    }

    private class MockTrigger implements Trigger {

        @Override
        public TriggerKey getKey() {
            return null;
        }

        @Override
        public JobKey getJobKey() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getCalendarName() {
            return null;
        }

        @Override
        public JobDataMap getJobDataMap() {
            return null;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public boolean mayFireAgain() {
            return false;
        }

        @Override
        public Date getStartTime() {
            return null;
        }

        @Override
        public Date getEndTime() {
            return null;
        }

        @Override
        public Date getNextFireTime() {
            return null;
        }

        @Override
        public Date getPreviousFireTime() {
            return null;
        }

        @Override
        public Date getFireTimeAfter(Date date) {
            return null;
        }

        @Override
        public Date getFinalFireTime() {
            return null;
        }

        @Override
        public int getMisfireInstruction() {
            return 0;
        }

        @Override
        public TriggerBuilder<? extends Trigger> getTriggerBuilder() {
            return null;
        }

        @Override
        public ScheduleBuilder<? extends Trigger> getScheduleBuilder() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int compareTo(Trigger trigger) {
            return 0;
        }
    }

    private SchedulerJob getJob() {
        return new SchedulerJob() {
            @Override
            public String getContextName() {
                return "contextName";
            }

            @Override
            public void setContextName(String contextName) {

            }

            @Override
            public List<String> getChildContextNames() {
                return null;
            }

            @Override
            public void setChildContextNames(List<String> contextIds) {

            }

            @Override
            public String getIdentifier() {
                return null;
            }

            @Override
            public void setIdentifier(String jobIdentifier) {

            }

            @Override
            public String getAgentName() {
                return null;
            }

            @Override
            public void setAgentName(String agentName) {

            }

            @Override
            public String getJobName() {
                return "jobName";
            }

            @Override
            public void setJobName(String jobName) {

            }

            @Override
            public String getJobDescription() {
                return null;
            }

            @Override
            public void setJobDescription(String jobDescription) {

            }

            @Override
            public String getStartupControlType() {
                return null;
            }

            @Override
            public void setStartupControlType(String startupControlType) {

            }

            @Override
            public void setSkippedContexts(Map<String, Boolean> skippedContexts) {

            }

            @Override
            public Map<String, Boolean> getSkippedContexts() {
                return null;
            }

            @Override
            public void setHeldContexts(Map<String, Boolean> heldContexts) {

            }

            @Override
            public Map<String, Boolean> getHeldContexts() {
                return null;
            }
        };
    }
}
