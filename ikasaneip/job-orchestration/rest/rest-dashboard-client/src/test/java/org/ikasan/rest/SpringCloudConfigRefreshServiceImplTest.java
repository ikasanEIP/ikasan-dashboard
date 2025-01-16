package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.job.orchestration.rest.client.SpringCloudConfigRefreshServiceImpl;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
public class SpringCloudConfigRefreshServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private SpringCloudConfigRefreshService uut;

    private String contextBaseUrl;

    @MockBean
    private Environment environment;
    
    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        uut = new SpringCloudConfigRefreshServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
        Mockito.doReturn("test").when(environment).getProperty("ikasan.dashboard.extract.username");
        Mockito.doReturn("test").when(environment).getProperty("ikasan.dashboard.extract.password");
        Mockito.doReturn(contextBaseUrl).when(environment).getProperty("ikasan.dashboard.extract.base.url");
        Mockito.doReturn(contextBaseUrl).when(environment).getProperty("spring.config.server.url");
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

    @Test
    public void test_actuator_successful() {
        stubFor(post(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(200)));

        uut.actuatorRefresh();

        verify(postRequestedFor(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test
    public void test_actuator_successful_empty_response() {
        stubFor(post(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(204)));

        uut.actuatorRefresh();

        verify(postRequestedFor(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * 403 Forbidden 
     */
    @Test(expected = RestClientException.class)
    public void test_actuator_403_do_nothing() {
        stubFor(post(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(403)));

        uut.actuatorRefresh();

        verify(postRequestedFor(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * 404 Not Found 
     */
    @Test(expected = RestClientException.class)
    public void test_actuator_404_do_nothing() {
        stubFor(post(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(404)));

        uut.actuatorRefresh();

        verify(postRequestedFor(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * 500 internal server error
     */
    @Test(expected = RestClientException.class)
    public void test_actuator_500_do_nothing() {
        stubFor(post(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(500)));

        uut.actuatorRefresh();

        verify(postRequestedFor(urlEqualTo("/actuator/refresh"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    @Test
    public void test_decrypt_successful() {
        stubFor(post(urlEqualTo("/decrypt"))
            .withRequestBody(containing("Pa5sW0rD"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("password")));

        String result = uut.decrypt(contextBaseUrl, "Pa5sW0rD");

        Assert.assertEquals("password", result);

        verify(postRequestedFor(urlEqualTo("/decrypt")));
    }

    @Test
    public void test_decrypt_empty_response() {
        stubFor(post(urlEqualTo("/decrypt"))
            .withRequestBody(containing("Pa5sW0rD"))
            .willReturn(aResponse()
                .withStatus(204)));

        String result = uut.decrypt(contextBaseUrl, "Pa5sW0rD");

        // As response is null, return the original value
        Assert.assertEquals("Pa5sW0rD", result);

        verify(postRequestedFor(urlEqualTo("/decrypt")));
    }

    /**
     * Even though it is a 403, do not throw an error and return the original input value
     */
    @Test
    public void test_decrypt_403_do_nothing() {
        stubFor(post(urlEqualTo("/decrypt"))
            .withRequestBody(containing("Pa5sW0rD"))
            .willReturn(aResponse()
                .withStatus(403)));

        String result = uut.decrypt(contextBaseUrl, "Pa5sW0rD");

        // As there was an error, return the original value
        Assert.assertEquals("Pa5sW0rD", result);

        verify(postRequestedFor(urlEqualTo("/decrypt")));
    }

    /**
     * Even though it is a 404, do not throw an error and return the original input value
     */
    @Test
    public void test_decrypt_404_do_nothing() {
        stubFor(post(urlEqualTo("/decrypt"))
            .withRequestBody(containing("Pa5sW0rD"))
            .willReturn(aResponse()
                .withStatus(404)));

        String result = uut.decrypt(contextBaseUrl, "Pa5sW0rD");

        // As there was an error, return the original value
        Assert.assertEquals("Pa5sW0rD", result);

        verify(postRequestedFor(urlEqualTo("/decrypt")));
    }

    /**
     * Even though it is a 500 internal server error, do not throw an error and return the original input value
     */
    @Test
    public void test_decrypt_500_do_nothing() {
        stubFor(post(urlEqualTo("/decrypt"))
            .withRequestBody(containing("Pa5sW0rD"))
            .willReturn(aResponse()
                .withStatus(500)));

        String result = uut.decrypt(contextBaseUrl, "Pa5sW0rD");

        // As there was an error, return the original value
        Assert.assertEquals("Pa5sW0rD", result);

        verify(postRequestedFor(urlEqualTo("/decrypt")));
    }

    @Test
    public void test_encrypt_successful() {
        stubFor(post(urlEqualTo("/encrypt"))
            .withRequestBody(containing("password"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("AQAqrD0tjxuYvid2QphgaQnaF9yne15o/4xSMKpYsvVTpqhCIwy")));

        String result = uut.encrypt("password");

        Assert.assertEquals("AQAqrD0tjxuYvid2QphgaQnaF9yne15o/4xSMKpYsvVTpqhCIwy", result);

        verify(postRequestedFor(urlEqualTo("/encrypt")));
    }

    @Test
    public void test_encrypt_empty_response() {
        stubFor(post(urlEqualTo("/encrypt"))
            .withRequestBody(containing("password"))
            .willReturn(aResponse()
                .withStatus(204)));

        String result = uut.encrypt("password");

        // As response is null, return the original value
        Assert.assertEquals("Issue encrypting the value using config services with error response [Encrypted value cannot return null]", result);

        verify(postRequestedFor(urlEqualTo("/encrypt")));
    }

    /**
     * Even though it is a 403, do not throw an error and return the original input value
     */
    @Test
    public void test_encrypt_403_do_nothing() {
        stubFor(post(urlEqualTo("/encrypt"))
            .withRequestBody(containing("password"))
            .willReturn(aResponse()
                .withStatus(403)));

        String result = uut.encrypt("password");

        // As there was an error, return the original value
        Assert.assertTrue(result.contains("Issue encrypting the value using config services with error response [403 Forbidden"));
        verify(postRequestedFor(urlEqualTo("/encrypt")));
    }

    /**
     * Even though it is a 404, do not throw an error and return the original input value
     */
    @Test
    public void test_encrypt_404_do_nothing() {
        stubFor(post(urlEqualTo("/encrypt"))
            .withRequestBody(containing("password"))
            .willReturn(aResponse()
                .withStatus(404)));

        String result = uut.encrypt("password");

        // As there was an error, return the original value
        Assert.assertTrue(result.contains("Issue encrypting the value using config services with error response [404 Not Found"));
        verify(postRequestedFor(urlEqualTo("/encrypt")));
    }

    /**
     * Even though it is a 500 internal server error, do not throw an error and return the original input value
     */
    @Test
    public void test_encrypt_500_do_nothing() {
        stubFor(post(urlEqualTo("/encrypt"))
            .withRequestBody(containing("password"))
            .willReturn(aResponse()
                .withStatus(500)));

        String result = uut.encrypt("password");

        // As there was an error, return the original value
        Assert.assertTrue(result.contains("Issue encrypting the value using config services with error response [500 Server Error"));

        verify(postRequestedFor(urlEqualTo("/encrypt")));
    }

}
