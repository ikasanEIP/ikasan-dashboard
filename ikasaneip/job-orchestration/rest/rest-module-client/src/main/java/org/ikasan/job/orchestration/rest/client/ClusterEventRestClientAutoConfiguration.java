package org.ikasan.job.orchestration.rest.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class ClusterEventRestClientAutoConfiguration {

    @Bean
    public ClusterEventRestServiceImpl clusterEventRestServiceImpl(Environment environment
        , HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        return new ClusterEventRestServiceImpl(environment, httpComponentsClientHttpRequestFactory);
    }
}
