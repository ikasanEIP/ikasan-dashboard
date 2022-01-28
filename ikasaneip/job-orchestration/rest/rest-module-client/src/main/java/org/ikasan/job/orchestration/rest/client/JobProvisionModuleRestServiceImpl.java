package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.rest.client.SchedulerRestServiceImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class JobProvisionModuleRestServiceImpl extends ModuleRestService {

    Logger logger = LoggerFactory.getLogger(SchedulerRestServiceImpl.class);

    public static final String JOB_PROVISION_REST_URL = "/rest/jobProvision";

    public JobProvisionModuleRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    public void provisionJobs(String contextUrl, SchedulerJobWrapper jobs) throws JsonProcessingException {
        HttpHeaders headers = createHttpHeaders();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("java.util.ArrayList")
            .build();
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        String serialised = objectMapper.writeValueAsString(jobs);

        HttpEntity entity = new HttpEntity(serialised, headers);
        String url = contextUrl + JOB_PROVISION_REST_URL;

        logger.info("Context URL[{}] Payload[{}] ", url, serialised);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }
}
