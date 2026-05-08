package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.ikasan.job.orchestration.rest.client.ClusterNodeRestServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterNodeRestServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ClusterNodeRestServiceImpl uut;

    @Mock
    private Environment environment;

    private static final String TEST_PATH = "/rest/clusterEvents/test-event";

    @Before
    public void setup() {
        String baseUrl = "http://localhost:" + wireMockRule.port();
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        uut = new ClusterNodeRestServiceImpl(baseUrl, environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_publish_success() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withStatus(200)));

        uut.publish(TEST_PATH, "test-event-payload");

        verify(postRequestedFor(urlEqualTo(TEST_PATH))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString())));
    }

    /**
     * A 400 response should be swallowed and logged as a warning — publish does not throw.
     */
    @Test
    public void test_publish_400_does_not_throw() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(400)));

        uut.publish(TEST_PATH, "test-event-payload");

        verify(postRequestedFor(urlEqualTo(TEST_PATH)));
    }

    /**
     * A 500 response should be swallowed and logged as a warning — publish does not throw.
     */
    @Test
    public void test_publish_500_does_not_throw() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(500)));

        uut.publish(TEST_PATH, "test-event-payload");

        verify(postRequestedFor(urlEqualTo(TEST_PATH)));
    }

    /**
     * A 401 response should trigger re-authentication and a single retry of the original request.
     */
    @Test
    public void test_publish_401_triggers_reauthentication_and_retries() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .inScenario("auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(post(urlEqualTo(TEST_PATH))
            .inScenario("auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse().withStatus(200)));

        uut.publish(TEST_PATH, "test-event-payload");

        verify(exactly(2), postRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    /**
     * When re-authentication itself fails (401), the retry should not be attempted and no exception thrown.
     */
    @Test
    public void test_publish_401_reauthentication_fails_does_not_throw() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(401)));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(401)));

        uut.publish(TEST_PATH, "test-event-payload");

        verify(exactly(1), postRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }
}
