package org.ikasan.rest.client;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.security.Principal;
import java.util.List;

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
        SecurityContextHolder
            .setContext(new SecurityContextImpl(
                new IkasanAuthentication(true, () -> "testUser"
                    , List.of(), "credentials"
                    , System.currentTimeMillis())));
    }

    @Test
    public void replay()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\",\"userName\":\"testUser\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(200)));
        boolean result = uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes(), "testUser");
        assertEquals(true, result);
    }

    @Test(expected = ReplayFailException.class)
    public void replay_returns400()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\",\"userName\":\"testUser\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(400)));
       uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes(), "testUser");
    }

    @Test(expected = ReplayFailException.class)
    public void resubmit_returns404()
    {
        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\",\"userName\":\"testUser\"}"))
                    .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(404)));
        uut.replay(contexBaseUrl,  null, null,"test Module Name", "flow Test", "resubmit".getBytes(), "testUser");
    }

    @Test(expected = ReplayFailException.class)
    public void resubmit_returns500()
    {

        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withRequestBody(containing("{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\",\"userName\":\"testUser\"}"))
                    .willReturn(aResponse()
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                    .withStatus(500)
                               ));
        uut.replay(contexBaseUrl, null, null,"test Module Name","flow Test","resubmit".getBytes(), "testUser");
    }

    @Test(expected = ReplayFailException.class)
    public void testTimeout() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
            = new HttpComponentsClientHttpRequestFactory(
            HttpClientBuilder.create().setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create().setDefaultSocketConfig(
                            SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(1000)).build()
                        )
                        .build())
                .build());
        httpComponentsClientHttpRequestFactory.setConnectTimeout(1000);
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(1000);

        uut = new ReplayRestServiceImpl(httpComponentsClientHttpRequestFactory);

        stubFor(put(urlEqualTo(ReplayRestServiceImpl.REPLAY_URL))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString())).withRequestBody(
                containing(
                    "{\"moduleName\":\"test Module Name\",\"flowName\":\"flow Test\",\"event\":\"cmVzdWJtaXQ=\",\"userName\":\"testUser\"}"))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()).withStatus(200).withFixedDelay(2000)));

        uut.replay(contexBaseUrl, null, null, "test Module Name", "flow Test", "resubmit".getBytes(), "testUser");
    }
}