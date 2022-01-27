package org.ikasan.job.orchestration.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;


@Configuration
public class JobOrchestrationRestClientAutoConfiguration {

    @Bean
    public JobProvisionModuleRestServiceImpl jobProvisionModuleRestServiceImpl(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory){
        return new JobProvisionModuleRestServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }


}