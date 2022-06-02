package org.ikasan.job.orchestration.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.IOException;
import java.util.List;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class ContextParametersRestUpdateServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ContextParametersRestUpdateServiceImpl uut;

    private String contextBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new ContextParametersRestUpdateServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void update_returns_200() throws IOException {
        ContextInstance instanceWithParams = createInstanceWithParams("CONTEXT-1");
        String json = objectMapper.writeValueAsString(instanceWithParams);
        stubFor(put(urlEqualTo("/rest/contextInstance/save"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(200)));

        uut.update(contextBaseUrl, instanceWithParams);

        verify(putRequestedFor(urlEqualTo("/rest/contextInstance/save"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json)));
    }

    @Test
    public void update_returns_503_catches_error() throws IOException {
        ContextInstance instanceWithParams = createInstanceWithParams("CONTEXT-1");
        String json = objectMapper.writeValueAsString(instanceWithParams);
        stubFor(put(urlEqualTo("/rest/contextInstance/save"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withStatus(503)));

        uut.update(contextBaseUrl, instanceWithParams);

        verify(putRequestedFor(urlEqualTo("/rest/contextInstance/save"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withRequestBody(containing(json)));
    }

    private ContextInstance createInstanceWithParams(String contextName) {
        ContextInstance instance = new ContextInstanceImpl();
        instance.setName(contextName);
        instance.setContextParameters(createParams());
        return instance;
    }

    private List<ContextParameterInstance> createParams() {
        return List.of(
            createParam("BusinessDate", "20220428"),
            createParam("ErrorSearch", "someValue"),
            createParam("UseBusinessDate", "1")
        );
    }

    private ContextParameterInstanceImpl createParam(String name, String value) {
        ContextParameterInstanceImpl param = new ContextParameterInstanceImpl();
        param.setName(name);
        param.setType("java.lang.String");
        param.setValue(value);
        return param;
    }

}