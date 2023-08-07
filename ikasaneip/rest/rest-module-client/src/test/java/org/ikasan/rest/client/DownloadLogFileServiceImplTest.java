package org.ikasan.rest.client;

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

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class DownloadLogFileServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private DownloadLogFileServiceImpl uut;

    private String contextBaseUrl;

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new DownloadLogFileServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_listLogFiles_200() {
        Map<String, String> expected = new HashMap<>();
        expected.put("application.log", "/opt/somedir/application.log");
        expected.put("h2.log", "/opt/somedir/h2.log");

        stubFor(get(urlEqualTo("/rest/logs/listLogFiles"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withBody("{\"application.log\":\"/opt/somedir/application.log\",\"h2.log\":\"/opt/somedir/h2.log\"}")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        Map<String, String> results = uut.listLogFiles(contextBaseUrl);
        for (Map.Entry<String, String> result : results.entrySet()) {
            assertTrue(expected.containsKey(result.getKey()));
            assertEquals(expected.get(result.getKey()), result.getValue());
        }
        assertEquals(2, results.size());
    }

    @Test
    public void test_listLogFiles_204() {
        stubFor(get(urlEqualTo("/rest/logs/listLogFiles"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(204)
            ));
        Map<String, String> results = uut.listLogFiles(contextBaseUrl);
        assertNull(results);
    }

    @Test(expected = RestClientException.class)
    public void test_listLogFiles_404() {
        stubFor(get(urlEqualTo("/rest/logs/listLogFiles"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        uut.listLogFiles(contextBaseUrl);
    }

    @Test(expected = RestClientException.class)
    public void test_listLogFiles_500() {
        stubFor(get(urlEqualTo("/rest/logs/listLogFiles"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        uut.listLogFiles(contextBaseUrl);
    }

    @Test
    public void test_downloadLogFile_200() {
        stubFor(get(urlEqualTo("/rest/logs/downloadLogFile?fullFilePath=/some/dir/application.log"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .willReturn(aResponse()
                .withBody("Some Data In File".getBytes())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE.toString())
                .withStatus(200)
            ));
        byte[] results = uut.downloadLogFile(contextBaseUrl, "/some/dir/application.log");
        assertEquals(new String(results), "Some Data In File");
    }

    @Test
    public void test_downloadLogFile_204() {
        stubFor(get(urlEqualTo("/rest/logs/downloadLogFile?fullFilePath=/some/dir/application.log"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE.toString())
                .withStatus(204)
            ));
        byte[] results = uut.downloadLogFile(contextBaseUrl, "/some/dir/application.log");
        assertNull(results);
    }

    @Test(expected = RestClientException.class)
    public void test_downloadLogFile_404() {
        stubFor(get(urlEqualTo("/rest/logs/downloadLogFile?fullFilePath=/some/dir/application.log"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE.toString())
                .withStatus(404)
            ));
        uut.downloadLogFile(contextBaseUrl, "/some/dir/application.log");
    }

    @Test(expected = RestClientException.class)
    public void test_downloadLogFile_500() {
        stubFor(get(urlEqualTo("/rest/logs/downloadLogFile?fullFilePath=/some/dir/application.log"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE.toString())
                .withStatus(500)
            ));
        uut.downloadLogFile(contextBaseUrl, "/some/dir/application.log");
    }
}
