package org.ikasan.job.orchestration.rest.client;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

public class SpringCloudConfigRefreshServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private SpringCloudConfigRefreshService uut;

    private String contextBaseUrl;
    
    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        uut = new SpringCloudConfigRefreshServiceImpl(new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_refresh_successful() {
        stubFor(get(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(200)));

        uut.refreshConfigRepo(contextBaseUrl, "scheduler-abc");

        verify(getRequestedFor(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test
    public void test_refresh_successful_empty_response() {
        stubFor(get(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(204)));
        
        uut.refreshConfigRepo(contextBaseUrl, "scheduler-abc");

        verify(getRequestedFor(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * Even though it is a 403, do not throw an error but log a warning that there was an issue 
     */
    @Test
    public void test_403_do_nothing() {
        stubFor(get(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(403)));
        
        uut.refreshConfigRepo(contextBaseUrl, "scheduler-abc");

        verify(getRequestedFor(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * Even though it is a 404, do not throw an error but log a warning that there was an issue 
     */
    @Test
    public void test_404_do_nothing() {
        stubFor(get(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(404)));

        uut.refreshConfigRepo(contextBaseUrl, "scheduler-abc");

        verify(getRequestedFor(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * Even though it is a 500 internal server error, do not throw an error but log a warning that there was an issue 
     */
    @Test
    public void test_500_do_nothing() {
        stubFor(get(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(500)));

        uut.refreshConfigRepo(contextBaseUrl, "scheduler-abc");

        verify(getRequestedFor(urlEqualTo("/scheduler-abc/default/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }
}
