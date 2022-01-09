package org.ikasan.scheduled.service;

import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.converter.ScheduledProcessAggregateConfigurationConverter;
import org.ikasan.scheduled.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.model.ScheduleProcessConfigurationBucket;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.ScheduledProcessService;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
import org.ikasan.spec.solr.SolrService;
import org.ikasan.spec.solr.SolrServiceBase;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by Ikasan Development Team.
 */
public class SolrScheduledProcessServiceImpl extends SolrServiceBase implements ScheduledProcessManagementService, ScheduledProcessService, SolrService<ScheduledProcessEvent>, BatchInsert<ScheduledProcessEvent>
{
    private SolrScheduledProcessEventDao scheduledProcessEventDao;
    private SolrModuleMetadataDao solrModuleMetadataDao;
    private SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao;
    private ScheduledProcessAggregateConfigurationConverter scheduledProcessAggregateConfigurationConverter;
    private SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao;
    private List<BatchInsertListener<ScheduledProcessEvent>> batchInsertListeners;


    public SolrScheduledProcessServiceImpl(SolrScheduledProcessEventDao solrScheduledProcessEventDao
        , SolrModuleMetadataDao solrModuleMetadataDao, SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao
        , SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao)
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
        this.solrBusinessStreamMetadataDao = solrBusinessStreamMetadataDao;
        if(this.solrBusinessStreamMetadataDao == null)
        {
            throw new IllegalArgumentException("solrBusinessStreamMetadataDao cannot be null!");
        }

        this.scheduledProcessAggregateConfigurationConverter = new ScheduledProcessAggregateConfigurationConverter();
        this.batchInsertListeners = new ArrayList<>();
    }

    @Override
    public void insert(List<ScheduledProcessEvent> scheduledProcessEvents) {
        this.save(scheduledProcessEvents);

        this.batchInsertListeners.forEach(batchInsertListeners
            -> batchInsertListeners.onBatchInsert(new BatchInsertEvent<>(scheduledProcessEvents)));
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

    @Override
    public List<String> getAllAgentNames() {
        return this.scheduledProcessEventDao.getAllAgentNames();
    }

    @Override
    public List<FlowMetaData> getFlowsForAgent(String agent) {
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        return moduleMetaData.getFlows().stream()
            .collect(Collectors.toList());
    }

    @Override
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

    @Override
    public ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getUpComingScheduledProcesses(String agent, String flow, long startTime, long endTime, int offset, int limit) {
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfigurationMetaData
            = this.getConfigurationForAgentFlowComponent(agent, flow, "Scheduled Consumer");

        if(scheduledConsumerConfigurationMetaData == null) {
            throw new RuntimeException(String.format("Could not load scheduled consumer configuration for agent[%s], job[%s]", agent, flow));
        }

        ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfigurationMetaData
            = this.getConfigurationForAgentFlowComponent(agent, flow, "Process Execution Broker");

        if(processExecutionBrokerConfigurationMetaData == null) {
            throw new RuntimeException(String.format("Could not load process execution broker configuration for agent[%s], job[%s]", agent, flow));
        }

        ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfigurationMetaData
            = this.getConfigurationForAgentFlowComponent(agent, flow, "Blackout Router");

        if(blackoutRouterConfigurationMetaData == null) {
            throw new RuntimeException(String.format("Could not load blackout router configuration for agent[%s], job[%s]", agent, flow));
        }

        List<UpcomingScheduledProcess> results = new ArrayList<>();

        AtomicReference<String> cronExpressionString = new AtomicReference<>();

        scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("cronExpression"))
            .findFirst().ifPresent(value -> cronExpressionString.set((String)value.getValue()));

        Optional<ConfigurationParameterMetaData> timezone  = scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("timezone"))
            .findFirst();

        AtomicReference<String> jobName = new AtomicReference<>();

        scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("jobName"))
            .findFirst().ifPresent(value -> jobName.set((String)value.getValue()));

        AtomicReference<String> jobGroup = new AtomicReference<>();

        scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("jobGroupName"))
            .findFirst().ifPresent(value -> jobGroup.set((String)value.getValue()));

        AtomicReference<String> jobDescription = new AtomicReference<>();

        scheduledConsumerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("description"))
            .findFirst().ifPresent(value -> jobDescription.set((String)value.getValue()));

        AtomicReference<String> commandLine = new AtomicReference<>();

        processExecutionBrokerConfigurationMetaData.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("commandLine"))
            .findFirst().ifPresent(value -> commandLine.set((String)value.getValue()));

        if(startTime < System.currentTimeMillis()) {
            startTime = System.currentTimeMillis();
        }

        int offsetCounter = 0;

        try {
            CronExpression cronExpression = new CronExpression(cronExpressionString.get());

            timezone.ifPresent(configurationParameterMetaData -> {
                if(configurationParameterMetaData.getValue() != null) {
                    cronExpression.setTimeZone(TimeZone.getTimeZone((String)configurationParameterMetaData.getValue()));
                }
            });

            Date next = cronExpression.getNextValidTimeAfter(new Date(startTime));

            // project the upcoming jobs forward
            while (next.before(new Date(endTime))) {
                if(offsetCounter >= offset) {
                    results.add(new UpcomingScheduledProcess(agent, jobName.get(),
                        jobGroup.get(), jobDescription.get(), next.getTime(), scheduledConsumerConfigurationMetaData,
                        processExecutionBrokerConfigurationMetaData, blackoutRouterConfigurationMetaData, cronExpression.getTimeZone().getID()));
                }

                next = cronExpression.getNextValidTimeAfter(next);
                offsetCounter++;
            }
        }
        catch (ParseException e) {
            throw new RuntimeException(String.format("Could not parse cron expression[%s] when determining upcoming jobs for agent[%s], job[%s]"
                , cronExpressionString.get(), agent, flow), e);
        }

        return new ScheduledProcessEventSearchResults(results, offsetCounter, 1L);
    }

    @Override
    public ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getUpComingScheduledProcesses(List<String> accessibleModules, long startTime, long endTime, String filter, int offset, int limit) {

        List<UpcomingScheduledProcess> results = new ArrayList<>();

        long start = System.currentTimeMillis();

        AtomicLong totalResults = new AtomicLong();

        this.scheduledProcessEventDao.getAllAgentNames().forEach(agentName -> {
            if(accessibleModules == null || accessibleModules.contains(agentName)) {
                ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agentName);

                moduleMetaData.getFlows().forEach(flowMetaData -> {
                    ScheduledProcessEventSearchResults<UpcomingScheduledProcess> scheduledProcessEventSearchResults;

                    if(filter != null && (agentName.toLowerCase().contains(filter.toLowerCase()) || (flowMetaData.getName().toLowerCase().contains(filter.toLowerCase())))){
                        scheduledProcessEventSearchResults = this.getUpComingScheduledProcesses(agentName, flowMetaData.getName(), startTime, endTime, offset, limit);
                        results.addAll(scheduledProcessEventSearchResults.getResultList());
                        totalResults.addAndGet(scheduledProcessEventSearchResults.getTotalNumberOfResults());
                    }
                    else if(filter == null) {
                        scheduledProcessEventSearchResults = this.getUpComingScheduledProcesses(agentName, flowMetaData.getName(), startTime, endTime, offset, limit);
                        results.addAll(scheduledProcessEventSearchResults.getResultList());
                        totalResults.addAndGet(scheduledProcessEventSearchResults.getTotalNumberOfResults());
                    }
                });
            }
        });

        results.sort((o1, o2) -> {
            if(o1.getFireTime() > o2.getFireTime()) return 1;
            else if(o1.getFireTime() < o2.getFireTime()) return -1;
            else return 0;
        });

        List<UpcomingScheduledProcess> finalResults = new ArrayList<>();

        if(limit == 0) {
            finalResults = results.subList(0, limit);
        }
        else if(results.size() > limit) {
            finalResults = results.subList(0, limit);
        }
        else {
            finalResults = results;
        }

        return new ScheduledProcessEventSearchResults(finalResults, totalResults.get(), System.currentTimeMillis() - start);
    }

    @Override
    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(String agent, long startTime, long endTime) {
        return this.scheduledProcessEventDao.getScheduleProcessEvents(agent, startTime, endTime);
    }

    @Override
    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(List<String> accessibleModules, long startTime, long endTime, String filter, boolean errorsOnly, int start, int limit, String sortOrder) {
        return this.scheduledProcessEventDao.getScheduleProcessEvents(accessibleModules, startTime, endTime, filter, errorsOnly, start, limit, sortOrder);
    }

    @Override
    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledProcessEvents(String agentName, String jobGroupName, String jobName, long startTime, long endTime, int start, int limit, String sortOrder) {
        return this.scheduledProcessEventDao.getScheduleProcessEvents(agentName, jobGroupName, jobName, startTime, endTime, start, limit, sortOrder);
    }

    @Override
    public ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getScheduleProcessAggregateConfigurations(String agent, String filter) {
        long start = System.currentTimeMillis();
        List<FlowMetaData> flows = this.getFlowsForAgent(agent);
        List<ScheduledProcessAggregateConfiguration> results = new ArrayList<>();

        flows.forEach(flowMetaData -> results.add(this.getScheduleProcessAggregateConfiguration(agent, flowMetaData.getName())));

        List<ScheduledProcessAggregateConfiguration> filteredResults = results.stream().filter(scheduledProcessAggregateConfiguration -> {
                if(filter == null || filter.isEmpty()) {
                    return true;
                }
                else {
                    return (scheduledProcessAggregateConfiguration.getJobName().toLowerCase().contains(filter.toLowerCase()) ||
                        scheduledProcessAggregateConfiguration.getJobGroup().toLowerCase().contains(filter.toLowerCase()) ||
                        scheduledProcessAggregateConfiguration.getJobDescription().toLowerCase().contains(filter.toLowerCase()));
                }
            })
            .collect(Collectors.toList());

        ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> searchResults
            = new ScheduledProcessEventSearchResults<>(filteredResults, filteredResults.size(), System.currentTimeMillis() - start);

        return searchResults;
    }

    @Override
    public ScheduledProcessAggregateConfiguration getScheduleProcessAggregateConfiguration(String agent, String flow) {
        AtomicReference<ScheduledProcessAggregateConfiguration> scheduledProcessAggregateConfiguration = new AtomicReference<>(new ScheduledProcessAggregateConfiguration());
        ModuleMetaData moduleMetaData = this.solrModuleMetadataDao.findById(agent);

        moduleMetaData.getFlows().stream()
            .filter(flowMetaData -> flowMetaData.getName().equals(flow))
            .findFirst()
            .ifPresent(flowMetaData -> {
                AtomicReference<String> scheduledConsumerConfigurationId = new AtomicReference<>();
                flowMetaData.getFlowElements().stream()
                    .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals("Scheduled Consumer"))
                    .findFirst()
                    .map(flowElementMetaData -> flowElementMetaData.getConfigurationId())
                    .ifPresent(id -> scheduledConsumerConfigurationId.set(id));

                AtomicReference<String> blackoutRouterConfigurationId = new AtomicReference<>();

                flowMetaData.getFlowElements().stream()
                    .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals("Blackout Router"))
                    .findFirst()
                    .map(flowElementMetaData -> flowElementMetaData.getConfigurationId())
                    .ifPresent(id -> blackoutRouterConfigurationId.set(id));

                AtomicReference<String> processExecutionBrokerConfigurationId = new AtomicReference<>();

                flowMetaData.getFlowElements().stream()
                    .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals("Process Execution Broker"))
                    .findFirst()
                    .map(flowElementMetaData -> flowElementMetaData.getConfigurationId())
                    .ifPresent(id -> processExecutionBrokerConfigurationId.set(id));

                ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration =
                    this.solrComponentConfigurationMetadataDao.findById(scheduledConsumerConfigurationId.get());
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfiguration =
                    this.solrComponentConfigurationMetadataDao.findById(blackoutRouterConfigurationId.get());
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration =
                    this.solrComponentConfigurationMetadataDao.findById(processExecutionBrokerConfigurationId.get());

                ScheduleProcessConfigurationBucket bucket = new ScheduleProcessConfigurationBucket(scheduledConsumerConfiguration,
                    processExecutionBrokerConfiguration, blackoutRouterConfiguration);

                scheduledProcessAggregateConfiguration.set(this.scheduledProcessAggregateConfigurationConverter.convert(bucket));
                scheduledProcessAggregateConfiguration.get().setAgentName(agent);
                scheduledProcessAggregateConfiguration.get().setJobName(flow);
                scheduledProcessAggregateConfiguration.get().setStartAutomatically(flowMetaData.getFlowStartupType().equals("AUTOMATIC"));
                scheduledProcessAggregateConfiguration.get().setBusinessStreamMetaData(this.solrBusinessStreamMetadataDao
                    .findBusinessStreamsContainingFlow(moduleMetaData.getName(), flowMetaData.getName(), 0, 1000));
            });


        return scheduledProcessAggregateConfiguration.get();
    }

    @Override
    public List<BusinessStreamMetaData> getBusinessStreams(String agent, String flow) {
        return this.solrBusinessStreamMetadataDao.findBusinessStreamsContainingFlow(agent, flow, 0, 1000);
    }

    public void saveConfiguration(ConfigurationMetaData configurationMetaData) {
        this.solrComponentConfigurationMetadataDao.save(configurationMetaData);
    }

    public void addBatchInsertListener(BatchInsertListener<ScheduledProcessEvent> batchInsertListener) {
        this.batchInsertListeners.add(batchInsertListener);
    }

    public void removeBatchInsertListener(BatchInsertListener<ScheduledProcessEvent> batchInsertListener) {
        this.batchInsertListeners.remove(batchInsertListener);
    }
}
