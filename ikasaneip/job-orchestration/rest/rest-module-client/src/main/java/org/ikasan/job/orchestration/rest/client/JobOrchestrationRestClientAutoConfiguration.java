package org.ikasan.job.orchestration.rest.client;

import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;


@Configuration
public class JobOrchestrationRestClientAutoConfiguration {

    @Value("${file.job.submission.wait.time.seconds:3}")
    private int fileJobSubmissionWaitTimeSeconds;

    @Bean
    public JobProvisionModuleService jobProvisionModuleRestServiceImpl(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory){
        return new JobProvisionModuleRestServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }

    @Bean
    public ContextInstancePublicationService contextParametersRestService(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        return new ContextInstancePublicationRestServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }

    @Bean
    public JobInitiationService jobInitiationService(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        return new JobInitiationServiceImpl(environment, httpComponentsClientHttpRequestFactory, this.fileJobSubmissionWaitTimeSeconds);
    }

    @Bean
    public JobUtilsService jobUtilsService(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        return new JobUtilsServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }
}