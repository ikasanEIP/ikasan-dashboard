package org.ikasan.job.orchestration.rest.client;

import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.rest.agent.client.ContextInstancePublicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;

public class ContextInstancePublicationRestServiceImpl extends ModuleRestService implements ContextInstancePublicationService<ContextInstance> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextInstancePublicationRestServiceImpl.class);
    private static final String REST_URL = "/rest/contextInstance/save";

    public ContextInstancePublicationRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void publish(String contextUrl, ContextInstance instance) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(instance, headers);
        String url = contextUrl + REST_URL;

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (RestClientResponseException e) {
            String message = String.format("Could not update context parameters for for agent url %s, params %s, responseCode: %d, error: %s",
                url, instance, e.getRawStatusCode(), e.getMessage());
            LOGGER.warn(message);

            //TODO figure out if more serious problem i.e. agent is down vs some more serious problem
            // 503/504 is timeout? 408? depends on server setup

        }

    }
}
