package org.ikasan.scheduled.model;

import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;

import java.util.List;

public class UpcomingScheduledProcess {
    private String agentName;
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    private String timezone;
    private long fireTime;
    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfigurationMetaData;
    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfigurationMetaData;
    private ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfigurationMetaData;

    public UpcomingScheduledProcess(String agentName, String jobName, String jobGroup, String jobDescription, long fireTime
        , ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfigurationMetaData
        , ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfigurationMetaData
        ,ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfigurationMetaData, String timezone) {
        this.agentName = agentName;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.jobDescription = jobDescription;
        this.fireTime = fireTime;
        this.scheduledConsumerConfigurationMetaData = scheduledConsumerConfigurationMetaData;
        this.processExecutionBrokerConfigurationMetaData = processExecutionBrokerConfigurationMetaData;
        this.blackoutRouterConfigurationMetaData = blackoutRouterConfigurationMetaData;
        this.timezone = timezone;
    }

    public String getAgentName() {
        return agentName;
    }

    
    public String getJobName() {
        return jobName;
    }

    
    public String getJobGroup() {
        return jobGroup;
    }

    
    public String getJobDescription() {
        return this.jobDescription;
    }


    public long getFireTime() {
        return fireTime;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getScheduledConsumerConfigurationMetaData() {
        return scheduledConsumerConfigurationMetaData;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getProcessExecutionBrokerConfigurationMetaData() {
        return processExecutionBrokerConfigurationMetaData;
    }

    public ConfigurationMetaData<List<ConfigurationParameterMetaData>> getBlackoutRouterConfigurationMetaData() {
        return blackoutRouterConfigurationMetaData;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getTimezone() {
        return timezone;
    }

//    @Override
//    public String toString() {
//        final StringBuffer sb = new StringBuffer("UpcomingScheduledProcess{");
//        sb.append("agentName='").append(agentName).append('\'');
//        sb.append(", jobName='").append(jobName).append('\'');
//        sb.append(", jobGroup='").append(jobGroup).append('\'');
//        sb.append(", jobDescription='").append(jobDescription).append('\'');
//        sb.append(", fireTime=").append(fireTime);
//        sb.append(", scheduledConsumerConfigurationMetaData=").append(scheduledConsumerConfigurationMetaData);
//        sb.append(", processExecutionBrokerConfigurationMetaData=").append(processExecutionBrokerConfigurationMetaData);
//        sb.append(", blackoutRouterConfigurationMetaData=").append(blackoutRouterConfigurationMetaData);
//        sb.append('}');
//        return sb.toString();
//    }
}
