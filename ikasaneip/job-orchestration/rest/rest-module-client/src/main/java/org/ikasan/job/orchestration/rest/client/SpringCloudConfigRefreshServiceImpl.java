package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private RestTemplate restTemplate;
    
    public SpringCloudConfigRefreshServiceImpl(HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
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
    
}
