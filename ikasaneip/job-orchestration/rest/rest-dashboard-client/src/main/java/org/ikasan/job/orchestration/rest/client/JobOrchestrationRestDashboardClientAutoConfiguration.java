package org.ikasan.job.orchestration.rest.client;

import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class JobOrchestrationRestDashboardClientAutoConfiguration {

    @Bean
    public SpringCloudConfigRefreshService springCloudConfigRefreshService(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        return new SpringCloudConfigRefreshServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }
    
}
