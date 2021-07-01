package org.ikasan.scheduled.model;

import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;

import java.util.List;

public class ScheduleProcessConfigurationBucket {

    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration;
    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfiguration;
    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration;

    public ScheduleProcessConfigurationBucket(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration
        , ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfiguration) {
        this.scheduledConsumerConfiguration = scheduledConsumerConfiguration;
        this.blackoutRouterConfiguration = blackoutRouterConfiguration;
        this.processExecutionBrokerConfiguration = processExecutionBrokerConfiguration;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getScheduledConsumerConfiguration() {
        return scheduledConsumerConfiguration;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getBlackoutRouterConfiguration() {
        return blackoutRouterConfiguration;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getProcessExecutionBrokerConfiguration() {
        return processExecutionBrokerConfiguration;
    }
}
