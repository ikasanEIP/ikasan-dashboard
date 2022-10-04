package org.ikasan.rest.client;

import org.ikasan.rest.client.dto.BigQueueMessageDto;
import org.ikasan.spec.module.client.BigQueueModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.*;

public class BigQueueModuleRestServiceImpl extends ModuleRestService implements BigQueueModuleService {

    private static final Logger LOG = LoggerFactory.getLogger(BigQueueModuleRestServiceImpl.class);

    private final static String GET_QUEUES_URL = "/rest/big/queue/";
    private final static String DELETE_MESSAGE_ID_URL = "/rest/big/queue/delete/{queueName}/{messageId}";
    private final static String GET_MESSAGES_URL = "/rest/big/queue/messages/{queueName}";
    private final static String PEEK_QUEUES_URL = "/rest/big/queue/peek/{queueName}";
    private final static String GET_QUEUES_SIZE_URL = "/rest/big/queue/size?includeZeros={includeZeros}";
    private final static String GET_SIZE_FOR_QUEUE_URL = "/rest/big/queue/size/{queueName}";

    public BigQueueModuleRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    /**
     * Calls the module to get the size of a given queue
     * @param contextUrl url of the module
     * @param queueName name of the queue we want to check the size for
     * @return size of the queue for the module being checked
     */
    @Override
    public long size(String contextUrl, String queueName) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>() {{ put("queueName", queueName); }};
        String url = contextUrl + GET_SIZE_FOR_QUEUE_URL;
        try {
            ResponseEntity<Long> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Long.class, parameters);
            if (responseEntity.getBody() == null) {
                throw new RestClientException("Empty response from the Module when getting the size of the queue");
            }
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            LOG.warn("Issue getting the size of the queue [{}] on the url [{}] with error [{}]", queueName, url, e.getLocalizedMessage());
            return 0L;
        }
    }

    /**
     * Calls the module to get the first message on the provided queue
     * @param contextUrl url of the module
     * @param queueName name of the queue to take a peek for
     * @return BigQueueMessage
     */
    @Override
    public BigQueueMessageDto peek(String contextUrl, String queueName) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>() {{ put("queueName", queueName); }};
        String url = contextUrl + PEEK_QUEUES_URL;
        try {
            ResponseEntity<BigQueueMessageDto> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, BigQueueMessageDto.class, parameters);
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            LOG.warn("Issue getting the first message from the queue [{}] on the url [{}] with error [{}]", queueName, url, e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Calls the module to get the all the messages on the provided queue
     * @param contextUrl url of the module
     * @param queueName name of the queue to get all messages for
     * @return list of BigQueueMessage
     */
    @Override
    public List<BigQueueMessageDto> getMessages(String contextUrl, String queueName) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>() {{ put("queueName", queueName); }};
        String url = contextUrl + GET_MESSAGES_URL;
        try {
            ResponseEntity<List<BigQueueMessageDto>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}, parameters);
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            LOG.warn("Issue getting the list of messages from the queue [{}] on the url [{}] with error [{}]", queueName, url, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calls the module to get all the names of the Big Queue that exist
     * @param contextUrl url of the module
     * @return a list of all queues for the module being checked.
     */
    @Override
    public List<String> listQueues(String contextUrl) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + GET_QUEUES_URL;
        try {
            ResponseEntity<List<String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            LOG.warn("Issue getting all queues on the url [{}] with error [{}]", url, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Calls the module to get all queue with its current queue depth
     * @param contextUrl url of the module
     * @param includeZeros boolean value to ask for queues with a 0 depth to be return (set to true). Set to false if only queue depth greater than 0 should be returned.
     * @return Map of queue and its size for the module
     */
    @Override
    public Map<String, Long> size(String contextUrl, boolean includeZeros) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, Boolean> parameters = new HashMap<>() {{ put("includeZeros", includeZeros); }};
        String url = contextUrl + GET_QUEUES_SIZE_URL;
        try {
            ResponseEntity<Map<String, Long>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}, parameters);
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            LOG.warn("Issue getting the size of all queues on the url [{}] [includeZeros={}]. Likely module is not compatible with BigQueue or module is not responsive.", url, includeZeros);
            LOG.debug(e.getLocalizedMessage());
            return new HashMap<>();
        }
    }

    /**
     * Calls the module to remove a message from a queue
     * @param contextUrl url of the module
     * @param queueName name of the queue to delete a message from
     * @param messageId id of the message to delete
     * @return true if successfully removed, false if something went wrong
     */
    @Override
    public boolean deleteMessage(String contextUrl, String queueName, String messageId) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>() {{ put("queueName", queueName); put("messageId", messageId);}};
        String url = contextUrl + DELETE_MESSAGE_ID_URL;
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, parameters);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return true;
            } else {
                if (responseEntity.getBody() != null) {
                    throw new RestClientException(responseEntity.getBody());
                } else {
                    throw new RestClientException("An unknown internal error has occurred when trying to delete the message from the queue");
                }
            }
        }
        catch(RestClientException e){
            LOG.warn("Issue removing the messageId [{}] from the queue [{}] on the url [{}] with error [{}]", messageId, queueName, url, e.getLocalizedMessage());
            return false;
        }
    }
}
