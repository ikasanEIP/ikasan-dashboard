package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ikasan.dashboard.AbstractRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.dashboard.DashboardRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class ClusterNodeRestServiceImpl extends AbstractRestServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterNodeRestServiceImpl.class);

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public ClusterNodeRestServiceImpl(String baseUrl, Environment environment,
                                      HttpComponentsClientHttpRequestFactory factory) {
        this.baseUrl = baseUrl;
        this.authenticateUrl = baseUrl + "/authenticate";
        this.username = environment.getProperty(DashboardRestService.DASHBOARD_USERNAME_PROPERTY);
        this.password = environment.getProperty(DashboardRestService.DASHBOARD_PASSWORD_PROPERTY);
        this.restTemplate = new RestTemplate(factory);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.restTemplate.getMessageConverters().add(converter);
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    public <T> void publish(String path, T event) {
        doPublish(path, event, true);
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
        } catch (RestClientException e) {
            LOG.warn("Failed to publish cluster event to [{}{}]: {}", baseUrl, path, e.getMessage());
        } catch (Exception e) {
            LOG.warn("Failed to serialize/publish cluster event to [{}{}]", baseUrl, path, e);
        }
    }
}
