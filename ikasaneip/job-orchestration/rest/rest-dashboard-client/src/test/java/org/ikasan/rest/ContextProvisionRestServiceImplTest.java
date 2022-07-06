package org.ikasan.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.builder.job.FileEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.InternalEventDrivenJobBuilder;
import org.ikasan.job.orchestration.builder.job.QuartzScheduleDrivenJobBuilder;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.rest.client.ContextProvisionRestServiceImpl;
import org.ikasan.job.orchestration.rest.client.DashboardRestClientException;
import org.ikasan.job.orchestration.rest.client.JobProvisionRestServiceImpl;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContextProvisionRestServiceImplTest extends AbstractTest{

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
            .allowIfSubType("org.ikasan.job.orchestration.model.profile")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();

        objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        contextBaseUrl = "http://localhost:" + wireMockRule.port();
    }

    @Test
    public void test_success_provision_context () throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        InputStream inputStream = new ClassPathResource("data/SAMPLE_CONTEXT/CONTEXT-1793100514_WITH-PROFILES.zip").getInputStream();
        ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

        String json = objectMapper.writeValueAsString(contextBundle);

        stubFor(put(urlEqualTo("/rest/provision/context"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(aResponse()
                .withStatus(200)
            ));

        contextProvisionRestService.provisionContext(contextBundle);

        verify(putRequestedFor(urlEqualTo("/rest/provision/context"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json)));
    }

    @Test(expected = DashboardRestClientException.class)
    public void test_exception_provision_context() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        InputStream inputStream = new ClassPathResource("data/SAMPLE_CONTEXT/CONTEXT-1793100514_WITH-PROFILES.zip").getInputStream();
        ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

        String json = objectMapper.writeValueAsString(contextBundle);

        stubFor(put(urlEqualTo("/rest/provision/context"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("useragent"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(aResponse()
                .withStatus(400)
            ));

        contextProvisionRestService.provisionContext(contextBundle);
    }

}
