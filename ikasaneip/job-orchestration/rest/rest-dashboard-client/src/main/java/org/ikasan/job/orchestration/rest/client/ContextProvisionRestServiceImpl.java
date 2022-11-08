package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.dashboard.DashboardRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class ContextProvisionRestServiceImpl extends DashboardRestServiceImpl<String> implements ContextProvisionService {

    public ContextProvisionRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory,
                                       String path) {
        super(environment, httpComponentsClientHttpRequestFactory, path);
    }

    @Override
    public void provisionContext(ContextBundle contextBundle) {
        try {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.ikasan.spec.scheduled.job.model")
                .allowIfSubType("org.ikasan.job.orchestration.model.job")
                .allowIfSubType("org.ikasan.job.orchestration.model.context")
                .allowIfSubType("org.ikasan.job.orchestration.model.profile")
                .allowIfSubType("org.ikasan.job.orchestration.model.notification")
                .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.HashMap")
                .build();
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

            String serialised = objectMapper.writeValueAsString(contextBundle);
            super.publish(serialised);
        }
        catch (Exception e) {
            throw new DashboardRestClientException("An exception has occurred provisioning context!", e);
        }
    }
}
