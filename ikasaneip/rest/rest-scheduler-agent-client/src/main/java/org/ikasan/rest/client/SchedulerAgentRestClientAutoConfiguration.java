package org.ikasan.rest.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Module Rest Client configuration required by ikasan dashboard in order to communicate with modules.
 * This autoconfig should be excluded from dashboard.
 */
public class SchedulerAgentRestClientAutoConfiguration
{

    @Bean
    @ConfigurationProperties(prefix = "module.rest.connection")
    public HttpComponentsClientHttpRequestFactory customHttpRequestFactory()
    {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
            = new HttpComponentsClientHttpRequestFactory();

        // all of the properties can be overwritten using spring properties.
        httpComponentsClientHttpRequestFactory.setReadTimeout(5000);
        httpComponentsClientHttpRequestFactory.setConnectTimeout(5000);
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(5000);

        return httpComponentsClientHttpRequestFactory;
    }

}