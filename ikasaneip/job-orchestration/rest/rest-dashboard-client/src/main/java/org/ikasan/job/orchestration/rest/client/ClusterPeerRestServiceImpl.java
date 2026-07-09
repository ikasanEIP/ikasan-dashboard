package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.AbstractRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.dashboard.DashboardRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class ClusterPeerRestServiceImpl extends AbstractRestServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterPeerRestServiceImpl.class);

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public ClusterPeerRestServiceImpl(String baseUrl, Environment environment,
                                      HttpComponentsClientHttpRequestFactory factory) {
        this.baseUrl = baseUrl;
        this.authenticateUrl = baseUrl + "/authenticate";
        this.username = environment.getProperty(DashboardRestService.DASHBOARD_USERNAME_PROPERTY);
        this.password = environment.getProperty(DashboardRestService.DASHBOARD_PASSWORD_PROPERTY);
        this.restTemplate = new RestTemplate(factory);

        JsonMapper mapper = JsonMapper.builder()
            .configure(tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build();

        JacksonJsonHttpMessageConverter jsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(mapper);
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);

        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public <T> void publish(String path, T event) {
        doPublish(path, event, true);
    }

    /**
     * Sends a PUT request to the given path with the supplied body and returns whether it was
     * handled by the remote node (HTTP 2xx). Returns false if the remote node responds with
     * any non-2xx status, or if a network/auth error occurs.
     * <p>
     * Retries once with a fresh authentication token on HTTP 401.
     */
    public <T> boolean invoke(String path, T body) {
        return doInvoke(path, body, true);
    }

    private <T> boolean doInvoke(String path, T body, boolean isFirst) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, createHttpHeaders(baseUrl));
            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.PUT, entity, Void.class);
            LOG.debug("Invoked [{}{}] — status [{}]", baseUrl, path, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) {
                    return doInvoke(path, body, false);
                }
            }
            LOG.debug("Non-success response from [{}{}]: status [{}]", baseUrl, path, e.getStatusCode());
            return false;
        } catch (ResourceAccessException e) {
            LOG.warn("Failed to invoke [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            LOG.warn("Failed to invoke [{}{}]: {}", baseUrl, path, e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.warn("Failed to serialize/invoke [{}{}]", baseUrl, path, e);
            return false;
        }
    }

    /**
     * Sends a PUT request with no body. Returns true on HTTP 2xx, false otherwise.
     */
    public boolean invokeNoBody(String path) {
        return doInvokeNoBody(path, true);
    }

    private boolean doInvokeNoBody(String path, boolean isFirst) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHttpHeaders(baseUrl));
            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.PUT, entity, Void.class);
            LOG.debug("Invoked (no-body) [{}{}] — status [{}]", baseUrl, path, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) return doInvokeNoBody(path, false);
            }
            LOG.debug("Non-success (no-body) from [{}{}]: status [{}]", baseUrl, path, e.getStatusCode());
            return false;
        } catch (ResourceAccessException e) {
            LOG.warn("Failed to invoke (no-body) [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            LOG.warn("Failed to invoke (no-body) [{}{}]: {}", baseUrl, path, e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.warn("Failed to invoke (no-body) [{}{}]", baseUrl, path, e);
            return false;
        }
    }

    /**
     * Sends a GET request and deserialises the response body to the given type.
     * Returns null on 404, network error, or deserialisation failure.
     */
    public <T> T get(String path, Class<T> responseType) {
        return doGet(path, responseType, true);
    }

    private <T> T doGet(String path, Class<T> responseType, boolean isFirst) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHttpHeaders(baseUrl));
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), responseType);
            }
            return null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) return doGet(path, responseType, false);
            }
            LOG.debug("GET [{}{}] returned non-success status [{}]", baseUrl, path, e.getStatusCode());
            return null;
        } catch (ResourceAccessException e) {
            LOG.warn("Failed to GET [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            LOG.warn("Failed to GET [{}{}]: {}", baseUrl, path, e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.warn("Failed to GET/deserialize [{}{}]", baseUrl, path, e);
            return null;
        }
    }

    /**
     * Sends a DELETE request to the given path (no request body). Returns true on HTTP 2xx, false otherwise.
     */
    public boolean invokeDelete(String path) {
        return doInvokeDelete(path, true);
    }

    private boolean doInvokeDelete(String path, boolean isFirst) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHttpHeaders(baseUrl));
            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.DELETE, entity, Void.class);
            LOG.debug("DELETE [{}{}] — status [{}]", baseUrl, path, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) return doInvokeDelete(path, false);
            }
            LOG.debug("DELETE [{}{}] returned non-success status [{}]", baseUrl, path, e.getStatusCode());
            return false;
        } catch (ResourceAccessException e) {
            LOG.warn("Failed to DELETE [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            LOG.warn("Failed to DELETE [{}{}]: {}", baseUrl, path, e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.warn("Failed to DELETE [{}{}]", baseUrl, path, e);
            return false;
        }
    }

    /**
     * Sends a GET request and deserialises the response as a list of the given element type.
     * Returns null on 404, network error, or deserialisation failure.
     */
    public <T> List<T> getList(String path, Class<? extends T> elementType) {
        return doGetList(path, elementType, true);
    }

    private <T> List<T> doGetList(String path, Class<? extends T> elementType, boolean isFirst) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHttpHeaders(baseUrl));
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
                return objectMapper.readValue(response.getBody(), type);
            }
            return null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) return doGetList(path, elementType, false);
            }
            LOG.debug("GET [{}{}] returned non-success status [{}]", baseUrl, path, e.getStatusCode());
            return null;
        } catch (ResourceAccessException e) {
            LOG.warn("Failed to GET list [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            LOG.warn("Failed to GET list [{}{}]: {}", baseUrl, path, e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.warn("Failed to GET/deserialize list [{}{}]", baseUrl, path, e);
            return null;
        }
    }

    private <T> void doPublish(String path, T event, boolean isFirst) {
        try {
            String json = objectMapper.writeValueAsString(event);
            HttpEntity<String> entity = new HttpEntity<>(json, createHttpHeaders(baseUrl));
            restTemplate.exchange(baseUrl + path, HttpMethod.POST, entity, Void.class);
            LOG.debug("Published cluster event to [{}{}]", baseUrl, path);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 && isFirst) {
                this.token = null;
                if (authenticate(baseUrl)) {
                    doPublish(path, event, false);
                    return;
                }
            }
            LOG.warn("Failed to publish cluster event to [{}{}], status [{}]: {}",
                baseUrl, path, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // Network-level failure (connection refused, timeout, etc.) — rethrown so that
            // PeerBroadcastChannel's circuit breaker can track consecutive peer failures.
            LOG.warn("Failed to publish cluster event to [{}{}]: {}", baseUrl, path, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            // HTTP error response (4xx/5xx) — peer is reachable, so do not trip the circuit.
            LOG.warn("Failed to publish cluster event to [{}{}]: {}", baseUrl, path, e.getMessage());
        } catch (Exception e) {
            LOG.warn("Failed to serialize/publish cluster event to [{}{}]", baseUrl, path, e);
        }
    }
}
