package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.dashboard.DashboardRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class JobProvisionRestServiceImpl extends DashboardRestServiceImpl<String> {

    private Logger logger = LoggerFactory.getLogger(JobProvisionRestServiceImpl.class);

    public JobProvisionRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory,
                                       String path) {
        super(environment, httpComponentsClientHttpRequestFactory, path);
    }

    public void provisionJobs(SchedulerJobWrapper jobs) throws JsonProcessingException {
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
        super.publish(serialised);
    }
}
