package org.ikasan.job.orchestration.rest.client;

import java.util.HashMap;
import java.util.Map;

import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

public class ContextInstancePublicationRestServiceImpl extends ModuleRestService implements ContextInstancePublicationService<ContextInstance> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextInstancePublicationRestServiceImpl.class);
    private static final String REST_URL = "/rest/contextInstance";
    private static final String REST_URL_SAVE = REST_URL + "/save";
    private static final String REST_URL_REMOVE = REST_URL + "/remove";
    private static final String REST_URL_REMOVE_ALL = REST_URL + "/removeAll";

    public ContextInstancePublicationRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public synchronized void publish(String contextUrl, ContextInstance instance) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(instance, headers);
        String url = contextUrl + REST_URL_SAVE;

        LOGGER.info(String.format("Publishing context instance[%s] with id[%s] to agent url[%s]"
            , instance.getName(), instance.getId(), url));
        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            LOGGER.info(String.format("Successfully published context instance[%s] with id[%s] to agent url[%s]"
                , instance.getName(), instance.getId(), url));
            //TODO figure out if more serious problem i.e. agent is down vs some more serious problem
            // 503/504 is timeout? 408? depends on server setup
        } catch (RestClientResponseException e) {
            String message = String.format("Could not update context parameters for for agent url %s, instance %s, responseCode: %d, error: %s",
                url, instance.getId(), e.getRawStatusCode(), e.getMessage());
            LOGGER.warn(message, e);
        } catch (Exception e) {
            String message = String.format("Could not update context parameters for for agent url %s, instance %s, error: %s",
                url, instance.getId(), e.getMessage());
            LOGGER.warn(message, e);
        }
    }

    @Override
    public synchronized void remove(String contextUrl, ContextInstance contextInstance) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + REST_URL_REMOVE;
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("correlationId", "{correlationId}")
                .encode()
                .toUriString();
            Map<String, String> parameters = new HashMap<>() {{
                put("correlationId", contextInstance.getId());
            }};

            restTemplate.exchange(urlTemplate, HttpMethod.DELETE, entity, String.class, parameters);
            //TODO figure out if more serious problem i.e. agent is down vs some more serious problem
            // 503/504 is timeout? 408? depends on server setup
        } catch (RestClientResponseException e) {
            e.printStackTrace();
            String message = String.format("Could not remove instance from agent for agent url %s, name %s, responseCode: %d, error: %s",
                url, contextInstance.getName(), e.getRawStatusCode(), e.getMessage());
            LOGGER.warn(message);
        } catch (Exception e) {
            e.printStackTrace();
            String message = String.format("Could not remove instance from agent for agent url %s, name %s, error: %s",
                url, contextInstance.getName(), e.getMessage());
            LOGGER.warn(message);
        }
    }

    @Override
    public void removeAll(String contextUrl) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + REST_URL_REMOVE_ALL;
        try {
            String urlTemplate = UriComponentsBuilder.fromHttpUrl(url)
                .encode()
                .toUriString();
            Map<String, String> parameters = new HashMap<>();

            restTemplate.exchange(urlTemplate, HttpMethod.DELETE, entity, String.class, parameters);
            //TODO figure out if more serious problem i.e. agent is down vs some more serious problem
            // 503/504 is timeout? 408? depends on server setup
        } catch (RestClientResponseException e) {
            e.printStackTrace();
            String message = String.format("Could not remove all context instances from agent for agent url %s, responseCode: %d, error: %s",
                url, e.getRawStatusCode(), e.getMessage());
            LOGGER.warn(message);
        } catch (Exception e) {
            e.printStackTrace();
            String message = String.format("Could not remove all context instances from agent for agent url %s, error: %s",
                url, e.getMessage());
            LOGGER.warn(message);
        }
    }
}
