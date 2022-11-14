package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.rest.client.exception.SchedulerAgentRestClientException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.rest.client.SchedulerRestServiceImpl;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;
import java.util.stream.Collectors;

public class JobProvisionModuleRestServiceImpl extends ModuleRestService implements JobProvisionModuleService {

    Logger logger = LoggerFactory.getLogger(SchedulerRestServiceImpl.class);

    public static final String JOB_PROVISION_REST_URL = "/rest/jobProvision";
    public static final String JOB_PROVISION_REMOVE_REST_URL = "/rest/jobProvision/remove";

    public JobProvisionModuleRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void provisionJobs(String contextUrl, SchedulerJobWrapper jobs) {
        try {
            HttpHeaders headers = createHttpHeaders();
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.ikasan.spec.scheduled.job.model")
                .allowIfSubType("org.ikasan.job.orchestration.model.job")
                .allowIfSubType("org.ikasan.job.orchestration.model.context")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.HashMap")
                .build();
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

            String serialised = objectMapper.writeValueAsString(jobs);

            HttpEntity entity = new HttpEntity(serialised, headers);
            String url = contextUrl + JOB_PROVISION_REST_URL;

            logger.info("Context URL[{}] Payload[{}] ", url, serialised);
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        }
        catch (Exception e) {
            logger.error("An error has occurred attempting to provision scheduler jobs!", e);
            throw new SchedulerAgentRestClientException("An error has occurred attempting to provision scheduler jobs!", e);
        }
    }

    @Override
    public void removeJobsForContext(String contextUrl, String contextName) {
        try {
            HttpHeaders headers = createHttpHeaders();

            HttpEntity entity = new HttpEntity(contextName, headers);
            String url = contextUrl + JOB_PROVISION_REMOVE_REST_URL;

            logger.info("Context URL[{}] Payload[{}] ", url, contextName);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        }
        catch (Exception e) {
            logger.error(String.format("An error has occurred attempting to remove jobs for context[%s]!"
                , contextName), e);
            throw new SchedulerAgentRestClientException(String.format("An error has occurred attempting to remove jobs for context[%s]!"
                , contextName), e);
        }
    }
}
