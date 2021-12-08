package org.ikasan.scheduled.event.service;

import org.ikasan.scheduled.event.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertListener;

import java.util.List;

public interface ScheduledProcessManagementService {

    /**
     * Method to get all scheduled agent names.
     *
     * @return
     */
    List<String> getAllAgentNames() ;

    /**
     * Get the flows for a specific agent.
     *
     * @param agent
     * @return
     */
    List<FlowMetaData> getFlowsForAgent(String agent);

    /**
     * Get a configuration for a specific agent flow component.
     *
     * @param agent
     * @param flow
     * @param component
     * @return
     */
    ConfigurationMetaData getConfigurationForAgentFlowComponent(String agent, String flow, String component);

    /**
     * Get upcoming scheduled processes for a specific agent, job and time window.
     * @param agent
     * @param flow
     * @param startTime
     * @param endTime
     * @return
     */
    List<UpcomingScheduledProcess> getUpComingScheduledProcesses(String agent, String flow, long startTime, long endTime);

    /**
     * Get filtered upcoming scheduled processes for a specific agent, job and time window for only the agents that the user can access.
     *
     * @param accessibleAgents
     * @param startTime
     * @param endTime
     * @param filter
     * @return
     */
    ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getUpComingScheduledProcesses(List<String> accessibleAgents, long startTime, long endTime, String filter);

    /**
     * Get all scheduled process events for a given agent within a time window. A scheduled process event represents an executed schedule job.
     *
     * @param agent
     * @param startTime
     * @param endTime
     * @return
     */
    ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(String agent, long startTime, long endTime);

    /**
     * Get filtered scheduled process events by agent name, jobGroupName and jobName within a time window. A scheduled process event represents an executed schedule job.
     *
     * @param agentName
     * @param jobGroupName
     * @param jobName
     * @param startTime
     * @param endTime
     * @param start
     * @param limit
     * @param sortOrder
     * @return
     */
    ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(String agentName, String jobGroupName, String jobName, long startTime, long endTime, int start, int limit, String sortOrder);

    /**
     * Get filtered scheduled process events for a given agent within a time window, that are accessible to the user. A scheduled process event represents an executed schedule job.
     *
     * @param accessibleAgents
     * @param startTime
     * @param endTime
     * @param filter
     * @param errorsOnly
     * @param start
     * @param limit
     * @param sortOrder
     * @return
     */
    ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(List<String> accessibleAgents,long startTime, long endTime, String filter, boolean errorsOnly, int start, int limit, String sortOrder);


    /**
     * Get the aggregate configurations for an agent, with filter if necessary.
     *
     * @param agent
     * @param filter
     * @return
     */
    ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getScheduleProcessAggregateConfigurations(String agent, String filter);

    /**
     * Get the aggregate configurations for an agent, with filter if necessary.
     *
     * @param agent
     * @param filter
     * @param offset
     * @param limit
     * @return
     */
    ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getScheduleProcessAggregateConfigurations(String agent, String filter, int offset, int limit);

    /**
     *
     * @param agentName
     * @param flowMetaData
     * @return
     */
    ScheduledProcessAggregateConfiguration getScheduleProcessAggregateConfiguration(String agentName, FlowMetaData flowMetaData);

    /**
     * Get a specific aggregate configuration for an agent and flow.
     * @param agent
     * @param flow
     * @return
     */
    ScheduledProcessAggregateConfiguration getScheduleProcessAggregateConfiguration(String agent, String flow);

    /**
     * Get all business stream that an agent flow is a member of.
     *
     * @param agent
     * @param flow
     * @return
     */
    List<BusinessStreamMetaData> getBusinessStreams(String agent, String flow);

    /**
     * Helper method to save a configuration.
     *
     * @param configurationMetaData
     */
    void saveConfiguration(ConfigurationMetaData configurationMetaData);

    /**
     * Add a listener that is notified when a batch insert occurs.
     *
     * @param batchInsertListener
     */
    void addBatchInsertListener(BatchInsertListener<ScheduledProcessEvent> batchInsertListener);

    /**
     * Remove a batch insert listener.
     *
     * @param batchInsertListener
     */
    void removeBatchInsertListener(BatchInsertListener<ScheduledProcessEvent> batchInsertListener);
}
