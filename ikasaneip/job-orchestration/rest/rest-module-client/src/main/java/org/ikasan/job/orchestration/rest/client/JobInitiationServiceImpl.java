package org.ikasan.job.orchestration.rest.client;

import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.rest.client.SchedulerRestServiceImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class JobInitiationServiceImpl extends ModuleRestService implements JobInitiationService {

    private static Logger logger = LoggerFactory.getLogger(SchedulerRestServiceImpl.class);

    public static final String SCHEDULER_JOB_INITIATION_URL = "/rest/schedulerJobInitiation";

    public JobInitiationServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void raiseSchedulerJobInitiationEvent(String contextUrl, SchedulerJobInitiationEvent event) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + SCHEDULER_JOB_INITIATION_URL;

        logger.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }
}
