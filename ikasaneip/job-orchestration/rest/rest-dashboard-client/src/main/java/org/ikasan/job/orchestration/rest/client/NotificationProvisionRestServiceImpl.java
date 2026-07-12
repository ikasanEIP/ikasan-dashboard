package org.ikasan.job.orchestration.rest.client;

import org.ikasan.dashboard.DashboardRestServiceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import static tools.jackson.databind.DefaultTyping.NON_FINAL;

public class NotificationProvisionRestServiceImpl extends DashboardRestServiceImpl<String> {

    private Logger logger = LoggerFactory.getLogger(NotificationProvisionRestServiceImpl.class);

    public NotificationProvisionRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory,
                                                String path) {
        super(environment, httpComponentsClientHttpRequestFactory, path);
    }

    public void provisionNotification(EmailNotificationDetailsWrapper emailNotificationDetailsWrapper) {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.notification")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();
        JsonMapper objectMapper = ObjectMapperFactory.newInstance();
        objectMapper = objectMapper.rebuild()
            .activateDefaultTyping(ptv, NON_FINAL)
            .build();

        String serialised = objectMapper.writeValueAsString(emailNotificationDetailsWrapper);
        super.publish(serialised);
    }
}
