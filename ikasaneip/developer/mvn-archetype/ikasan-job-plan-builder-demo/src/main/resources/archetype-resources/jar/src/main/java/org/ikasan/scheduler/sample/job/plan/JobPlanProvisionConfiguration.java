package org.ikasan.scheduler.sample.job.plan;

import org.ikasan.job.orchestration.rest.client.ContextProvisionRestServiceImpl;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class JobPlanProvisionConfiguration {

    @Autowired
    Environment environment;

    @Bean
    public ContextProvisionService contextProvisionRestService() {
        ContextProvisionRestServiceImpl contextProvisionRestService = new ContextProvisionRestServiceImpl(environment,
                new HttpComponentsClientHttpRequestFactory(), "/rest/provision/context");

        return contextProvisionRestService;
    }
}
