package org.ikasan.rest;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.ikasan.job.orchestration.rest.client.ClusterPeerRestServiceImpl;
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
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterPeerRestServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private ClusterPeerRestServiceImpl uut;

    @Mock
    private Environment environment;

    private static final String TEST_PATH = "/rest/clusterEvents/test-event";
    private String baseUrl;

    @Before
    public void setup() {
        baseUrl = "http://localhost:" + wireMockRule.port();
        when(environment.getProperty("ikasan.dashboard.extract.username")).thenReturn("admin");
        when(environment.getProperty("ikasan.dashboard.extract.password")).thenReturn("admin");
        uut = new ClusterPeerRestServiceImpl(baseUrl, environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_getBaseUrl_returns_constructor_value() {
        assertEquals(baseUrl, uut.getBaseUrl());
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

    /**
     * A network-level failure on publish must be rethrown so callers can trip circuit breakers.
     */
    @Test(expected = ResourceAccessException.class)
    public void test_publish_resource_access_exception_is_rethrown() {
        stubFor(post(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.publish(TEST_PATH, "test-event-payload");
    }

    // ── invoke (PUT with body) ───────────────────────────────────────────────────────────────────

    @Test
    public void test_invoke_success_returns_true() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invoke(TEST_PATH, "test-payload"));
        verify(putRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    public void test_invoke_non_2xx_returns_false() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(404)));

        assertFalse(uut.invoke(TEST_PATH, "test-payload"));
    }

    @Test
    public void test_invoke_401_triggers_reauthentication_and_retries() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .inScenario("invoke-auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(put(urlEqualTo(TEST_PATH))
            .inScenario("invoke-auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invoke(TEST_PATH, "test-payload"));

        verify(exactly(2), putRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    /**
     * Network failure on invoke must be rethrown — ContextMachineRestImpl relies on this to
     * trigger its retry loop.
     */
    @Test(expected = ResourceAccessException.class)
    public void test_invoke_resource_access_exception_is_rethrown() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.invoke(TEST_PATH, "test-payload");
    }

    // ── invokeNoBody (PUT, no request body) ─────────────────────────────────────────────────────

    @Test
    public void test_invokeNoBody_success_returns_true() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invokeNoBody(TEST_PATH));
        verify(putRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    public void test_invokeNoBody_non_2xx_returns_false() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(404)));

        assertFalse(uut.invokeNoBody(TEST_PATH));
    }

    @Test
    public void test_invokeNoBody_401_triggers_reauthentication_and_retries() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .inScenario("invoke-no-body-auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(put(urlEqualTo(TEST_PATH))
            .inScenario("invoke-no-body-auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invokeNoBody(TEST_PATH));

        verify(exactly(2), putRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    @Test(expected = ResourceAccessException.class)
    public void test_invokeNoBody_resource_access_exception_is_rethrown() {
        stubFor(put(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.invokeNoBody(TEST_PATH);
    }

    // ── get (GET, deserialise to T) ──────────────────────────────────────────────────────────────

    @Test
    public void test_get_success_deserialises_response_body() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("\"hello\"")));

        assertEquals("hello", uut.get(TEST_PATH, String.class));
    }

    @Test
    public void test_get_non_2xx_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(404)));

        assertNull(uut.get(TEST_PATH, String.class));
    }

    @Test
    public void test_get_401_triggers_reauthentication_and_retries() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .inScenario("get-auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(get(urlEqualTo(TEST_PATH))
            .inScenario("get-auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("\"hello\"")));

        assertEquals("hello", uut.get(TEST_PATH, String.class));

        verify(exactly(2), getRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    @Test
    public void test_get_success_with_null_body_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)));

        assertNull(uut.get(TEST_PATH, String.class));
    }

    @Test
    public void test_get_invalid_json_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{invalid-json")));

        assertNull(uut.get(TEST_PATH, String.class));
    }

    @Test(expected = ResourceAccessException.class)
    public void test_get_resource_access_exception_is_rethrown() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.get(TEST_PATH, String.class);
    }

    // ── invokeDelete (DELETE) ────────────────────────────────────────────────────────────────────

    @Test
    public void test_invokeDelete_success_returns_true() {
        stubFor(delete(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invokeDelete(TEST_PATH));
        verify(deleteRequestedFor(urlEqualTo(TEST_PATH)));
    }

    @Test
    public void test_invokeDelete_non_2xx_returns_false() {
        stubFor(delete(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(404)));

        assertFalse(uut.invokeDelete(TEST_PATH));
    }

    @Test
    public void test_invokeDelete_401_triggers_reauthentication_and_retries() {
        stubFor(delete(urlEqualTo(TEST_PATH))
            .inScenario("delete-auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(delete(urlEqualTo(TEST_PATH))
            .inScenario("delete-auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse().withStatus(200)));

        assertTrue(uut.invokeDelete(TEST_PATH));

        verify(exactly(2), deleteRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    @Test(expected = ResourceAccessException.class)
    public void test_invokeDelete_resource_access_exception_is_rethrown() {
        stubFor(delete(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.invokeDelete(TEST_PATH);
    }

    // ── getList (GET, deserialise to List<T>) ────────────────────────────────────────────────────

    @Test
    public void test_getList_success_deserialises_list() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("[\"item1\",\"item2\"]")));

        List<String> result = uut.getList(TEST_PATH, String.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("item1", result.get(0));
        assertEquals("item2", result.get(1));
    }

    @Test
    public void test_getList_non_2xx_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withStatus(404)));

        assertNull(uut.getList(TEST_PATH, String.class));
    }

    @Test
    public void test_getList_401_triggers_reauthentication_and_retries() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .inScenario("get-list-auth-retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(401))
            .willSetStateTo("authenticated"));

        stubFor(post(urlEqualTo("/authenticate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"token\":\"test-jwt-token\"}")));

        stubFor(get(urlEqualTo(TEST_PATH))
            .inScenario("get-list-auth-retry")
            .whenScenarioStateIs("authenticated")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("[\"item1\",\"item2\"]")));

        List<String> result = uut.getList(TEST_PATH, String.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(exactly(2), getRequestedFor(urlEqualTo(TEST_PATH)));
        verify(exactly(1), postRequestedFor(urlEqualTo("/authenticate")));
    }

    @Test
    public void test_getList_success_with_null_body_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)));

        assertNull(uut.getList(TEST_PATH, String.class));
    }

    @Test
    public void test_getList_invalid_json_returns_null() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{invalid-json")));

        assertNull(uut.getList(TEST_PATH, String.class));
    }

    @Test(expected = ResourceAccessException.class)
    public void test_getList_resource_access_exception_is_rethrown() {
        stubFor(get(urlEqualTo(TEST_PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        uut.getList(TEST_PATH, String.class);
    }
}
