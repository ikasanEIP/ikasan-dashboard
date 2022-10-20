package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.dashboard.DashboardRestServiceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;

public class JobProvisionRestServiceImpl extends DashboardRestServiceImpl<String> implements JobProvisionService {

    private Logger logger = LoggerFactory.getLogger(JobProvisionRestServiceImpl.class);

    public JobProvisionRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory,
                                       String path) {
        super(environment, httpComponentsClientHttpRequestFactory, path);
    }

    public void provisionJobs(List<SchedulerJob> jobs) {
        try {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.ikasan.spec.scheduled.job.model")
                .allowIfSubType("org.ikasan.job.orchestration.model.job")
                .allowIfSubType("org.ikasan.job.orchestration.model.context")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.HashMap")
                .build();
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

            SchedulerJobWrapper wrapper = new SchedulerJobWrapperImpl();
            wrapper.setJobs(jobs);

            String serialised = objectMapper.writeValueAsString(wrapper);
            super.publish(serialised);
        }
        catch (Exception e) {
            throw new DashboardRestClientException("An exception has occurred provisioning jobs!", e);
        }
    }

    @Override
    public void removeJobs(String contextName) {
        throw new UnsupportedOperationException();
    }
}
