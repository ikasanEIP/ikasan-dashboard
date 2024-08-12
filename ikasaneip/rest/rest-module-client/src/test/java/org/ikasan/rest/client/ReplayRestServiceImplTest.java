package org.ikasan.rest.client;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class ReplayRestServiceImplTest
{
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ReplayRestServiceImpl uut = new ReplayRestServiceImpl(new HttpComponentsClientHttpRequestFactory());

    private String contexBaseUrl;

    @Before
    public void setup()
    {
        contexBaseUrl = "http://localhost:" + wireMockRule.port();
    }

    @Test
    public void replay()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(200)));
        boolean result = uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes());
        assertEquals(true, result);
    }

    @Test(expected = ReplayFailException.class)
    public void replay_returns400()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(400)));
       uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes());
    }

    @Test(expected = ReplayFailException.class)
    public void resubmit_returns404()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(404)));
        uut.replay(contexBaseUrl,  null, null,"test Module Name", "flow Test", "resubmit".getBytes());
    }

    @Test(expected = ReplayFailException.class)
    public void resubmit_returns500()
    {

        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withRequestBody(containing("{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\"}"))
                    .willReturn(aResponse()
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                    .withStatus(500)
                               ));
        uut.replay(contexBaseUrl, null, null,"test Module Name","flow Test","resubmit".getBytes());
    }

    @Test(expected = ReplayFailException.class)
    public void testTimeout() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
            = new HttpComponentsClientHttpRequestFactory();

        httpComponentsClientHttpRequestFactory.setConnectTimeout(1000);
        httpComponentsClientHttpRequestFactory.setReadTimeout(1000);
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(1000);

        uut = new ReplayRestServiceImpl(httpComponentsClientHttpRequestFactory);

        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\"}"))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(200).withFixedDelay(2000)));

        uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes());
    }
}