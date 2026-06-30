package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Base64;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Scheduler can host configuration properties from a different Spring Cloud Config. 
 * This service allows the dashboard to call the config service to refresh multiple repos that host these
 * configuration. This is based on:
 * https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_pattern_matching_and_multiple_repositories
 */
public class SpringCloudConfigRefreshServiceImpl implements SpringCloudConfigRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudConfigRefreshServiceImpl.class);
    
    /* Based on SpringCloud docs: 
       https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_pattern_matching_and_multiple_repositories 
       Using this API and using the profile [default] to trigger the config-service to update the baseDir */
    public static final String REFRESH_URL = "/{applicationPattern}/default/";
    public static final String ENCRYPTED_URL = "/encrypt";
    public static final String DECRYPTED_URL = "/decrypt";
    public static final String ACTUATOR_REFRESH = "/actuator/refresh";

    private RestTemplate restTemplate;
    private Environment environment;
    
    public SpringCloudConfigRefreshServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
        this.environment = environment;
    }

    @Override
    public void refreshConfigRepo(String contextUrl, String applicationPattern) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + REFRESH_URL;

        Map<String, String> parameters = new HashMap<String, String>() {
            {put("applicationPattern", applicationPattern);}
        };

        try {
            LOGGER.info("Refreshing config services with the applicationPattern [{}]. URL called: [{}]", applicationPattern, url);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class, parameters);
        }
        catch(RestClientException e) {
            LOGGER.warn("Issue refreshing config services with the applicationPattern [{}]. URL called: [{}] with response [{}]",
                applicationPattern, url, e.getLocalizedMessage());
        }
    }
    
    @Override
    public String decrypt(String contextUrl, String encryptedValue) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity request = new HttpEntity(encryptedValue, headers);
        String url = contextUrl + DECRYPTED_URL;

        try {
            String decryptedValue = restTemplate.postForObject(url, request, String.class);
            if (decryptedValue == null) {
                throw new RestClientException("Decrypted value cannot return null");
            }
            return decryptedValue;
        }
        catch(RestClientException e) {
            LOGGER.warn("Issue decrypting the value [{}] from config services with error response [{}]",
                encryptedValue, e.getLocalizedMessage());
            // Return the encrypted value
            return encryptedValue;
        }
    }

    @Override
    public String encrypt(String valueToEncrypt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity request = new HttpEntity(valueToEncrypt, headers);
        String configServiceUrl = environment.getProperty("spring.config.server.url");
        if (configServiceUrl == null || configServiceUrl.isBlank()) {
            LOGGER.warn("Cannot encrypt value: property 'spring.config.server.url' is not configured.");
            throw new RestClientException("spring.config.server.url is not configured — cannot encrypt value");
        }
        String url = configServiceUrl + ENCRYPTED_URL;

        try {
            String encryptedValue = restTemplate.postForObject(url, request, String.class);
            if (encryptedValue == null) {
                throw new RestClientException("Encrypted value cannot return null");
            }
            return encryptedValue;
        }
        catch(RestClientException e) {
            String errorResponse = "Issue encrypting the value using config services with error response [" + e.getLocalizedMessage() + "]";
            LOGGER.warn(errorResponse);
            return errorResponse;
        }
    }
    
    @Override
    public void actuatorRefresh() {
        actuatorRefreshAtUrl(environment.getProperty("ikasan.dashboard.extract.base.url"));
    }

    @Override
    public void actuatorRefreshAtUrl(String baseUrl) {
        HttpEntity entity = new HttpEntity(buildActuatorRefreshHeaders());
        String url = baseUrl + ACTUATOR_REFRESH;
        try {
            LOGGER.info("Actuator Refresh will start now. URL called: [{}]", url);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        }
        catch(Exception e) {
            LOGGER.error("Issue with Actuator Refresh. URL called: [{}] with response [{}]", url, e.getLocalizedMessage(), e);
            throw new RestClientException("Issue with Actuator Refresh");
        }
    }

    private HttpHeaders buildActuatorRefreshHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        String credentials = environment.getProperty("ikasan.dashboard.extract.username") + ":" + environment.getProperty("ikasan.dashboard.extract.password");
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encodeBase64(credentials.getBytes())));
        return headers;
    }
}
