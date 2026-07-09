package org.ikasan.job.orchestration.rest.client;

import org.ikasan.dashboard.DashboardRestServiceImpl;
import org.ikasan.job.orchestration.util.JsonMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import static tools.jackson.databind.DefaultTyping.NON_FINAL;


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

            JsonMapper objectMapper = JsonMapperFactory.newInstance();

            objectMapper = objectMapper.rebuild()
                .polymorphicTypeValidator(ptv)
                .activateDefaultTyping(ptv, NON_FINAL)
                .disable(MapperFeature.USE_ANNOTATIONS)
                .build();

            String serialised = objectMapper.writeValueAsString(contextBundle);
            super.publish(serialised);
        }
        catch (Exception e) {
            throw new DashboardRestClientException("An exception has occurred provisioning context!", e);
        }
    }
}
