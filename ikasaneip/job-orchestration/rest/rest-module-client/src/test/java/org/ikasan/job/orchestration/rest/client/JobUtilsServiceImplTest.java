package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class JobUtilsServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private JobUtilsServiceImpl uut;

    private String contextBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new JobUtilsServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_kill_success() {
        stubFor(get(urlEqualTo("/rest/jobUtils/kill/12345?destroy=false"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(200)));


        uut.killJob(contextBaseUrl, 12345, false);

        verify(getRequestedFor(urlEqualTo("/rest/jobUtils/kill/12345?destroy=false"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test
    public void test_kill_success_destroy() {
        stubFor(get(urlEqualTo("/rest/jobUtils/kill/12345?destroy=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(200)));


        uut.killJob(contextBaseUrl, 12345, true);

        verify(getRequestedFor(urlEqualTo("/rest/jobUtils/kill/12345?destroy=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test(expected = RestClientException.class)
    public void test_exception_kill_success_destroy() {
        stubFor(get(urlEqualTo("/rest/jobUtils/kill/12345?destroy=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(403)));


        uut.killJob(contextBaseUrl, 12345, true);
    }

    @Test(expected = RestClientException.class)
    public void test_exception_kill_success() {
        stubFor(get(urlEqualTo("/rest/jobUtils/kill/12345?destroy=false"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(403)));


        uut.killJob(contextBaseUrl, 12345, false);
    }
}
