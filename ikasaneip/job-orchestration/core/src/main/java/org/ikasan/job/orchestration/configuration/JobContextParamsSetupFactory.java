package org.ikasan.job.orchestration.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Setup parameters that may be required for the Context.
 *
 * The below is the property entry needed.
 *
 *   job.context.mapping.configuration.location[ContextName1]=/location/file/test-context-1.properties
 *   job.context.mapping.configuration.location[ContextName2]=/location/file/test-context-2.properties
 *
 * The property contains the path to the location of where the file that contains the parameters are stored.
 * The file is a key value pair property file, example of its contents:
 *
 * param1=value1
 * param2=value2
 */
@Configuration
public class JobContextParamsSetupFactory {

    /**
     * Reads from the properties file a List of location where the individual context configuration are stored.
     * Expecting to see the below in the property file.
     *
     *   job.context.mapping.configuration.location[ContextName1]=/location/file/test-context-1.properties
     *   job.context.mapping.configuration.location[ContextName2]=/location/file/test-context-2.properties
     *
     * Once object is returned, you can use getParamsToReplace to get the required values, for example for ContextName1
     * {
     *   ContextName1 : {
     *     param1=value2,
     *     param2=value2
     *   }
     * }
     *
     * @return JobContextParamsSetupConfiguration
     */
    @Bean
    @ConfigurationProperties(prefix = "job.context.mapping.configuration", ignoreUnknownFields = true)
    public JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration() {
        return new JobContextParamsSetupConfiguration();
    }
}
