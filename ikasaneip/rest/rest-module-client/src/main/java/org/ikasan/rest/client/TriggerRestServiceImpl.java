package org.ikasan.rest.client;

import org.ikasan.rest.client.dto.TriggerDto;
import org.ikasan.spec.module.client.TriggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

public class TriggerRestServiceImpl extends ModuleRestService implements TriggerService<TriggerDto>
{
    Logger logger = LoggerFactory.getLogger(TriggerRestServiceImpl.class);

    protected final static String PUT_TRIGGER_URL = "/rest/wiretap/trigger";
    protected final static String DELETE_TRIGGER_URL = "/rest/wiretap/trigger/{triggerId}";
    protected final static String DELETE_TRIGGER_WITH_USER_URL = "/rest/wiretap/trigger/{triggerId}/{user}";

    /**
     * Constructor for TriggerRestServiceImpl.
     *
     * @param environment the environment object used for configuration
     * @param httpComponentsClientHttpRequestFactory the HTTP components client request factory
     */
    public TriggerRestServiceImpl(Environment environment,
                                  HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    /**
     * Creates a trigger using the provided context URL and trigger DTO.
     *
     * @param contextUrl The base URL context for the trigger creation
     * @param triggerDto The TriggerDto object containing trigger details
     * @return true if the trigger creation is successful, false otherwise
     */
    @Override
    public boolean create(String contextUrl, TriggerDto triggerDto)
    {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(triggerDto, headers);
        String url = contextUrl + PUT_TRIGGER_URL;
        try
        {
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return true;
        }
        catch (RestClientException e)
        {
            logger.warn(
                "Issue creating trigger [" + url + "] with dto [" + triggerDto + "]");
            return false;
        }
    }


    /**
     * Deletes a trigger with the specified triggerId and user from the provided context URL.
     *
     * @param contextUrl The base URL context for trigger deletion
     * @param triggerId The ID of the trigger to be deleted
     * @param user The user associated with the trigger to be deleted
     * @return true if the trigger is successfully deleted, false otherwise
     */
    @Override
    public boolean delete(String contextUrl, String triggerId, String user)
    {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + DELETE_TRIGGER_WITH_USER_URL;
        try
        {
            Map<String, String> parameters = new HashMap<>()
            {{put("triggerId", triggerId);put("user", user);}};

            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class,parameters);

            return true;
        }
        catch (RestClientException e)
        {
            if(this.delete(contextUrl, triggerId) == false) {
                logger.warn("Issue Deleting trigger [" + url + "] with module [" + triggerId + "] and user [" + user + "]");
                return false;
            }
            else {
                return true;
            }
        }
    }

    /**
     * Deletes a trigger with the specified triggerId.
     *
     * @param contextUrl The base URL context for trigger deletion
     * @param triggerId The ID of the trigger to be deleted
     * @return true if the trigger is successfully deleted, false otherwise
     */
    @Override
    public boolean delete(String contextUrl, String triggerId) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + DELETE_TRIGGER_URL;
        try
        {
            Map<String, String> parameters = new HashMap<>()
            {{put("triggerId", triggerId);}};

            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class,parameters);

            return true;
        }
        catch (RestClientException e)
        {
            logger.warn(
                "Issue Deleting trigger [" + url + "] with module [" + triggerId + "]");
            return false;
        }
    }
}
