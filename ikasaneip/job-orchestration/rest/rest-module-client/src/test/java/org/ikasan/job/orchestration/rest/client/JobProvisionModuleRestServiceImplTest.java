package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.rest.client.dto.SchedulerJobInitiationEventDto;
import org.ikasan.job.orchestration.rest.client.exception.SchedulerAgentRestClientException;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class JobProvisionModuleRestServiceImplTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private JobProvisionModuleService uut;

    private String contextBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new JobProvisionModuleRestServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_job_provision_success() throws IOException {
        stubFor(put(urlEqualTo("/rest/jobProvision"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        uut.provisionJobs(contextBaseUrl, schedulerJobWrapper);

        verify(putRequestedFor(urlEqualTo("/rest/jobProvision"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test(expected = SchedulerAgentRestClientException.class)
    public void test_job_provision_exception() throws IOException {
        stubFor(put(urlEqualTo("/rest/jobProvision"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(400)));

        SchedulerJobWrapperImpl schedulerJobWrapper = new SchedulerJobWrapperImpl();
        uut.provisionJobs(contextBaseUrl, schedulerJobWrapper);
    }

    @Test
    public void test_job_provision_remove_success() throws IOException {
        stubFor(delete(urlEqualTo("/rest/jobProvision/remove"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing("contextName"))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        uut.removeJobsForContext(contextBaseUrl, "contextName");

        verify(deleteRequestedFor(urlEqualTo("/rest/jobProvision/remove"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing("contextName")));
    }

    @Test(expected = SchedulerAgentRestClientException.class)
    public void test_job_provision_remove_exception() throws IOException {
        stubFor(delete(urlEqualTo("/rest/jobProvision/remove"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing("contextName"))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(400)));

        uut.removeJobsForContext(contextBaseUrl, "contextName");
    }
}
