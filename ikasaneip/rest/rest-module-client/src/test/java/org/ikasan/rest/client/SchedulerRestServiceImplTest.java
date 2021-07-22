package org.ikasan.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.rest.client.dto.TriggerDto;
import org.ikasan.spec.trigger.TriggerRelationship;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class SchedulerRestServiceImplTest
{
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private SchedulerRestServiceImpl uut;

    private String contextBaseUrl;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup()
    {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new SchedulerRestServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void getTriggers() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo(SchedulerRestServiceImpl.TRIGGER_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                        .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                        .withStatus(200)));


        Optional<List<Trigger>> result = uut.getTriggers(contextBaseUrl);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());
    }

    @Test
    public void getTriggers_http400() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo(SchedulerRestServiceImpl.TRIGGER_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(400)));


        Optional<List<Trigger>> result = uut.getTriggers(contextBaseUrl);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getTriggers_http404() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo(SchedulerRestServiceImpl.TRIGGER_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(404)));


        Optional<List<Trigger>> result = uut.getTriggers(contextBaseUrl);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getTriggers_http500() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo(SchedulerRestServiceImpl.TRIGGER_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(500)));


        Optional<List<Trigger>> result = uut.getTriggers(contextBaseUrl);
        assertTrue(result.isEmpty());
    }

    @Test
    public void triggerFlowNow() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/moduleName/flowName"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(200)));


        boolean result = uut.triggerFlowNow(contextBaseUrl, "moduleName", "flowName");
        assertTrue(result);
    }

    @Test
    public void triggerFlowNow_http400() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/moduleName/flowName"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(400)));


        boolean result = uut.triggerFlowNow(contextBaseUrl, "moduleName", "flowName");
        assertFalse(result);
    }

    @Test
    public void triggerFlowNow_http404() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/moduleName/flowName"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(404)));


        boolean result = uut.triggerFlowNow(contextBaseUrl, "moduleName", "flowName");
        assertFalse(result);
    }

    @Test
    public void triggerFlowNow_http450() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        stubFor(get(urlEqualTo("/rest/scheduler/moduleName/flowName"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(500)));


        boolean result = uut.triggerFlowNow(contextBaseUrl, "moduleName", "flowName");
        assertFalse(result);
    }


    @Test
    public void testTimeout() throws JsonProcessingException {
        Environment environment = new StandardEnvironment();
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
            = new HttpComponentsClientHttpRequestFactory();

        httpComponentsClientHttpRequestFactory.setConnectTimeout(1000);
        httpComponentsClientHttpRequestFactory.setReadTimeout(1000);
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(1000);

        uut = new SchedulerRestServiceImpl(environment, httpComponentsClientHttpRequestFactory);

        stubFor(get(urlEqualTo(SchedulerRestServiceImpl.TRIGGER_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody(mapper.writeValueAsString(List.of(new MockTrigger(), new MockTrigger(), new MockTrigger())))
                .withStatus(200).withFixedDelay(2000)));

       assertFalse(uut.getTriggers(contextBaseUrl).isPresent());
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

}