package org.ikasan.scheduled.service;

import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.dao.SolrScheduledProcessEventDao;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.ScheduledProcessEventService;
import org.ikasan.spec.solr.SolrService;
import org.ikasan.spec.solr.SolrServiceBase;
import org.ikasan.spec.systemevent.SystemEvent;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class SolrScheduledProcessEventServiceImpl extends SolrServiceBase implements ScheduledProcessEventService,  SolrService<ScheduledProcessEvent>, BatchInsert<ScheduledProcessEvent>
{
    private SolrScheduledProcessEventDao scheduledProcessEventDao;
    private SolrModuleMetadataDao solrModuleMetadataDao;
    private SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao;


    public SolrScheduledProcessEventServiceImpl(SolrScheduledProcessEventDao systemEventDao)
    {
        this.scheduledProcessEventDao = systemEventDao;
        if(this.scheduledProcessEventDao == null)
        {
            throw new IllegalArgumentException("systemEventDao cannot be null!");
        }
    }

    @Override
    public void insert(List<ScheduledProcessEvent> scheduledProcessEvents)
    {
        this.save(scheduledProcessEvents);
    }

    @Override
    public void save(ScheduledProcessEvent scheduledProcessEvent)
    {
        this.scheduledProcessEventDao.setSolrUsername(this.solrUsername);
        this.scheduledProcessEventDao.setSolrPassword(this.solrPassword);
        scheduledProcessEventDao.save(scheduledProcessEvent);
    }

    @Override
    public void save(List<ScheduledProcessEvent> scheduledProcessEvents)
    {
        this.scheduledProcessEventDao.setSolrUsername(this.solrUsername);
        this.scheduledProcessEventDao.setSolrPassword(this.solrPassword);
        scheduledProcessEventDao.save(scheduledProcessEvents);

    }

    public List<String> getAllAgents() {
        return this.scheduledProcessEventDao.getAllAgents();
    }

    public List<String> getJobGroupsForAgent(String agent) {
        return this.scheduledProcessEventDao.getJobGroupsForAgent(agent);
    }

    public List<String> getJobsForAgentAndJobGroup(String agent, String jobGroup) {
        return this.scheduledProcessEventDao.getJobsForAgentAndJobGroup(agent, jobGroup);
    }

    public List<String> getScheduledProcessConfigurationsForAgent(String agent) {
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        String configurationId = moduleMetaData.getFlows().stream()
            .filter(flow -> flow.getName().equals("sheduler-flow-name"))
            .findFirst().get()
            .getFlowElements().stream()
            .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals("scheduler-configurable-component"))
            .findFirst().get()
            .getConfigurationId();

        ConfigurationMetaData configurationMetaData = this.solrComponentConfigurationMetadataDao.findById(configurationId);

        return null;
    }
}
