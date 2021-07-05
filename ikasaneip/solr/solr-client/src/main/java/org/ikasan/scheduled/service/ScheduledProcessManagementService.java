package org.ikasan.scheduled.service;

import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.List;

public interface ScheduledProcessManagementService {

    public List<String> getAllAgentNames() ;

    public List<String> getJobGroupsForAgent(String agent);

    public List<String> getJobsForAgentAndJobGroup(String agent, String jobGroup);

    public List<String> getScheduledProcessConfigurationsForAgent(String agent);

    public List<ConfigurationMetaData<List<ConfigurationParameterMetaData>>> getScheduledConfigurationsForAgent(String agent);

    public List<FlowMetaData> getFlowsForAgent(String agent);

    public ConfigurationMetaData getConfigurationForAgentFlowComponent(String agent, String flow, String component);

    public List<UpcomingScheduledProcess> getUpComingScheduledProcesses(String agent, String flow, long startTime, long endTime);

    public ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getUpComingScheduledProcesses(long startTime, long endTime, String filter);

    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(String agent, long startTime, long endTime);

    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(long startTime, long endTime, String filter, boolean errorsOnly);

    public ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getScheduleProcessAggregateConfigurations(String agent, String filter);

    public ScheduledProcessAggregateConfiguration getScheduleProcessAggregateConfiguration(String agent, String flow);

    public void saveConfiguration(ConfigurationMetaData configurationMetaData);
}
