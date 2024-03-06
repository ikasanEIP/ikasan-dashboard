package org.ikasan.dashboard.beans;

import org.ikasan.configurationService.ConfigurationServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@Import(ConfigurationServiceAutoConfiguration.class)
public class PersistenceComponentFactory
{
}
