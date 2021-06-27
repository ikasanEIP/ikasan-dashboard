package org.ikasan.scheduled.service;

import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.model.SolrScheduledProcessEvent;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.ScheduledProcessService;
import org.ikasan.spec.solr.SolrService;
import org.ikasan.spec.solr.SolrServiceBase;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class SolrScheduledProcessServiceImpl extends SolrServiceBase implements ScheduledProcessService,  SolrService<ScheduledProcessEvent>, BatchInsert<ScheduledProcessEvent>
{
    private SolrScheduledProcessEventDao scheduledProcessEventDao;
    private SolrModuleMetadataDao solrModuleMetadataDao;
    private SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao;


    public SolrScheduledProcessServiceImpl(SolrScheduledProcessEventDao solrScheduledProcessEventDao
        , SolrModuleMetadataDao solrModuleMetadataDao, SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao)
    {
        this.scheduledProcessEventDao = solrScheduledProcessEventDao;
        if(this.scheduledProcessEventDao == null)
        {
            throw new IllegalArgumentException("systemEventDao cannot be null!");
        }
        this.solrModuleMetadataDao = solrModuleMetadataDao;
        if(this.solrModuleMetadataDao == null)
        {
            throw new IllegalArgumentException("systemEventDao cannot be null!");
        }
        this.solrComponentConfigurationMetadataDao = solrComponentConfigurationMetadataDao;
        if(this.solrComponentConfigurationMetadataDao == null)
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

    public List<String> getAllAgentNames() {
        return this.scheduledProcessEventDao.getAllAgentNames();
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

    public List<ConfigurationMetaData<List<ConfigurationParameterMetaData>>> getScheduledConfigurationsForAgent(String agent) {
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        List<String> configurationIds = moduleMetaData.getFlows().stream()
            .flatMap(flowMetaData -> flowMetaData.getFlowElements().stream())
            .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals("Scheduled Consumer"))
            .map(flowElementMetaData -> flowElementMetaData.getConfigurationId())
            .collect(Collectors.toList());

        List<ConfigurationMetaData<List<ConfigurationParameterMetaData>>> results = new ArrayList<>();

        configurationIds.forEach(configurationId ->
            results.add(this.solrComponentConfigurationMetadataDao.findById(configurationId)));

        return results;
    }

    public List<FlowMetaData> getFlowsForAgent(String agent) {
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        return moduleMetaData.getFlows().stream()
            .collect(Collectors.toList());
    }

    public ConfigurationMetaData getConfigurationForAgentFlowComponent(String agent, String flow, String component) {
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        String configurationId =  moduleMetaData.getFlows().stream()
            .filter(flowMetaData -> flowMetaData.getName().equals(flow))
            .findFirst().get()
            .getFlowElements().stream()
            .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
            .findFirst().get()
            .getConfigurationId();

        return this.solrComponentConfigurationMetadataDao.findById(configurationId);
    }

    public List<UpcomingScheduledProcess> getUpComingScheduledProcesses(String agent, String flow, long startTime, long endTime) {
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfigurationMetaData
            = this.getConfigurationForAgentFlowComponent(agent, flow, "Scheduled Consumer");

        ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfigurationMetaData
            = this.getConfigurationForAgentFlowComponent(agent, flow, "Process Execution Broker");

        List<UpcomingScheduledProcess> results = new ArrayList<>();

        String cronExpressionString  = (String)scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("cronExpression"))
            .findFirst().get().getValue();


        // todo these need to be made configurable upstream
//        String jobName  = (String)processExecutionBrokerConfigurationMetaData.getParameters().stream()
//            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("jobName"))
//            .findFirst().get().getValue();
//
//        String jobGroup  = (String)processExecutionBrokerConfigurationMetaData.getParameters().stream()
//            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("jobGroup"))
//            .findFirst().get().getValue();
//
//        String jobDescription  = (String)processExecutionBrokerConfigurationMetaData.getParameters().stream()
//            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("jobDescription"))
//            .findFirst().get().getValue();

        String commandLine  = (String)processExecutionBrokerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("commandLine"))
            .findFirst().get().getValue();

        try {
            CronExpression cronExpression = new CronExpression(cronExpressionString);

            Date next = cronExpression.getNextValidTimeAfter(new Date(startTime));

            while (next.before(new Date(endTime))) {
                results.add(new UpcomingScheduledProcess(agent, "jobName",
                    "jobGroup", "jobDescription", commandLine, next.getTime()));

                next = cronExpression.getNextValidTimeAfter(next);
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return results;
    }

    public List<SolrScheduledProcessEvent> getScheduledProcessEvents(String agent, String flow, long startTime, long endTime) {
        return this.scheduledProcessEventDao.getScheduleProcessEvents(agent, startTime, endTime);
    }
}
