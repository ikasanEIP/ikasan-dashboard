package org.ikasan.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.rest.client.ContextProvisionRestServiceImpl;
import org.ikasan.job.orchestration.rest.client.DashboardRestClientException;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.junit.Before;
import org.junit.Ignore;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class ContextProvisionLocalHostImplTest extends AbstractTest{

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

        contextBaseUrl = "http://localhost:9090";
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

        ContextProfileRecord contextProfileRecord = contextBundle.getContextProfiles().get(0);
        ContextProfile contextProfile = contextProfileRecord.getContextProfile();
        contextProfile.setDefaultContext("CONTEXT-1436221681");
        contextProfileRecord.setContextProfile(contextProfile);

        contextProvisionRestService.provisionContext(contextBundle);

    }

    @Test
    public void test_success_provision_context_1025415934() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        InputStream inputStream = new ClassPathResource("data/SAMPLE_CONTEXT/CONTEXT-1025415934.zip").getInputStream();
        ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

        contextProvisionRestService.provisionContext(contextBundle);
    }

    @Test
    public void test_success_provision_CONTEXT_1616314541() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        InputStream inputStream = new ClassPathResource("data/SAMPLE_CONTEXT/CONTEXT--1616314541.zip").getInputStream();
        ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

        contextProvisionRestService.provisionContext(contextBundle);
    }

    @Test
    public void test_success_provision_context_CONTEXT_611007888() throws IOException {
        when(environment.getProperty("ikasan.dashboard.extract.enabled", "false")).thenReturn("true");
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        when(environment.getProperty("module.name")).thenReturn("useragent");
        when(environment.getProperty("ikasan.dashboard.extract.base.url")).thenReturn(contextBaseUrl);
        when(environment.getProperty("ikasan.dashboard.extract.exceptions", "false")).thenReturn("true");

        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
            new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        InputStream inputStream = new ClassPathResource("data/SAMPLE_CONTEXT/CONTEXT--611007888.zip").getInputStream();
        ContextBundle contextBundle = ContextImportZipUtils.extractZipFile(inputStream);

        contextProvisionRestService.provisionContext(contextBundle);
    }

}
