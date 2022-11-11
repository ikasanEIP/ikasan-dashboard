package org.ikasan.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailNotificationParamsFactory {

    @Bean
    @ConfigurationProperties(prefix = "scheduler.email.notification.configuration", ignoreUnknownFields = true)
    public EmailNotificationParamsConfiguration emailNotificationParamsConfiguration() {
        return new EmailNotificationParamsConfiguration();
    }
}
