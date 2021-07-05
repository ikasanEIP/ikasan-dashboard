package org.ikasan.scheduled.converter;

import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduleProcessConfigurationBucket;
import org.ikasan.scheduled.model.ScheduledProcessConfigurationConstants;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.serialiser.Converter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledProcessAggregateConfigurationConverter implements Converter<ScheduleProcessConfigurationBucket, ScheduledProcessAggregateConfiguration> {

    @Override
    public ScheduledProcessAggregateConfiguration convert(ScheduleProcessConfigurationBucket scheduleProcessConfigurationBucket) {
        ScheduledProcessAggregateConfiguration scheduledProcessAggregateConfiguration = new ScheduledProcessAggregateConfiguration();

        scheduledProcessAggregateConfiguration.setJobName((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.JOB_NAME));
        scheduledProcessAggregateConfiguration.setJobGroup((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.JOB_GROUP_NAME));
        scheduledProcessAggregateConfiguration.setJobDescription((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.JOB_DESCRIPTION));
        scheduledProcessAggregateConfiguration.setCronExpression((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.CRON_EXPRESSION));
        scheduledProcessAggregateConfiguration.setTimezone((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.TIMEZONE));
        scheduledProcessAggregateConfiguration.setIgnoreMisfire((Boolean)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.IGNORE_MISFIRE));
        scheduledProcessAggregateConfiguration.setEager((Boolean)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.EAGER));
        scheduledProcessAggregateConfiguration.setMaxEagerCallbacks((Integer) this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.MAX_EAGER_CALLBACKS));
        scheduledProcessAggregateConfiguration.setPassthroughProperties((Map<String, String>) this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getScheduledConsumerConfiguration(), ScheduledProcessConfigurationConstants.PASS_THROUGH_PROPERTIES));

        scheduledProcessAggregateConfiguration.setCommandLine((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.COMMAND_LINE));
        scheduledProcessAggregateConfiguration.setWorkingDirectory((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.WORKING_DIRECTORY));
        scheduledProcessAggregateConfiguration.setSuccessfulReturnCodes((List<String>)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.SUCCESSFUL_RETURN_CODES));
        if(this.getConfigurationParameterMetaDataValue(scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.SECONDS_TO_WAIT_FOR_PROCESS_TO_START) != null) {
            scheduledProcessAggregateConfiguration.setSecondsToWaitForProcessStart(((Integer) this.getConfigurationParameterMetaDataValue
                (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.SECONDS_TO_WAIT_FOR_PROCESS_TO_START)).longValue());
        }
        scheduledProcessAggregateConfiguration.setStdErr((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.STD_ERR));
        scheduledProcessAggregateConfiguration.setStdOut((String)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.STD_OUT));
        scheduledProcessAggregateConfiguration.setRetryOnFail((Boolean)this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getProcessExecutionBrokerConfiguration(), ScheduledProcessConfigurationConstants.RETRY_ON_FAIL));

        scheduledProcessAggregateConfiguration.setBlackoutCronExpressions((List<String>) this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getBlackoutRouterConfiguration(), ScheduledProcessConfigurationConstants.CRON_EXPRESSIONS));
        scheduledProcessAggregateConfiguration.setBlackoutDateTimeRanges((Map<String, String>) this.getConfigurationParameterMetaDataValue
            (scheduleProcessConfigurationBucket.getBlackoutRouterConfiguration(), ScheduledProcessConfigurationConstants.DATE_TIME_RANGES));


        return scheduledProcessAggregateConfiguration;
    }


    private Object getConfigurationParameterMetaDataValue(ConfigurationMetaData<List<ConfigurationParameterMetaData>> params
        , String paramName) {
        AtomicReference<Object> value = new AtomicReference<>();
        params.getParameters().stream()
            .filter(param -> param.getName().equals(paramName))
            .findFirst()
            .ifPresent(conf -> value.set(conf.getValue()));

        return value.get();
    }
}
