package org.ikasan.scheduled.service;

import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.dao.SolrScheduledProcessEventDao;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

@Ignore
public class ScheduleProcessServiceTest {

    private SolrScheduledProcessEventDao dao;
    private SolrModuleMetadataDao solrModuleMetadataDao;
    private SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao;

    private  SolrScheduledProcessServiceImpl solrScheduledProcessService;

    @Before
    public void setup() {
        dao = new SolrScheduledProcessEventDao();
        dao.initStandalone("http://localhost:8983/solr", 30);
        dao.setSolrUsername("ikasan");
        dao.setSolrPassword("1ka5an");

        solrModuleMetadataDao = new SolrModuleMetadataDao();
        solrModuleMetadataDao.initStandalone("http://localhost:8983/solr", 30);
        solrModuleMetadataDao.setSolrUsername("ikasan");
        solrModuleMetadataDao.setSolrPassword("1ka5an");

        solrComponentConfigurationMetadataDao = new SolrComponentConfigurationMetadataDao();
        solrComponentConfigurationMetadataDao.initStandalone("http://localhost:8983/solr", 30);
        solrComponentConfigurationMetadataDao.setSolrUsername("ikasan");
        solrComponentConfigurationMetadataDao.setSolrPassword("1ka5an");
    }

    @Test
    public void test_get_scheduled_configurations_for_agent() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent ->
            this.solrScheduledProcessService.getScheduledConfigurationsForAgent(agent)
                .forEach(configurationMetaData -> System.out.println(configurationMetaData)));
    }

    @Test
    public void test_get_flows_for_agent() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent ->
            this.solrScheduledProcessService.getFlowsForAgent(agent)
                .forEach(flowMetaData -> System.out.println(flowMetaData)));
    }

    @Test
    public void getConfigurationForAgentFlowComponent() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent ->
            this.solrScheduledProcessService.getFlowsForAgent(agent)
                .forEach(flowMetaData
                    -> System.out.println(this.solrScheduledProcessService.getConfigurationForAgentFlowComponent(agent, flowMetaData.getName(), "Blackout Router"))
                ));

        agents.forEach(agent ->
            this.solrScheduledProcessService.getFlowsForAgent(agent)
                .forEach(flowMetaData
                    -> System.out.println(this.solrScheduledProcessService.getConfigurationForAgentFlowComponent(agent, flowMetaData.getName(), "Scheduled Consumer"))
                ));

        agents.forEach(agent ->
            this.solrScheduledProcessService.getFlowsForAgent(agent)
                .forEach(flowMetaData
                    -> System.out.println(this.solrScheduledProcessService.getConfigurationForAgentFlowComponent(agent, flowMetaData.getName(), "Process Execution Broker"))
                ));
    }

    @Test
    public void get_upcoming_jobs_for_agent() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent ->
            this.solrScheduledProcessService.getUpComingScheduledProcesses(agent, "flow 1" ,System.currentTimeMillis(),
                System.currentTimeMillis() + 100000000L).forEach(upcomingScheduledProcess -> System.out.println(upcomingScheduledProcess)));
    }

    @Test
    public void get_all_upcoming_jobs() {
            this.solrScheduledProcessService.getUpComingScheduledProcesses(System.currentTimeMillis(),
                System.currentTimeMillis() + 100000L, "").getResultList().forEach(upcomingScheduledProcess -> System.out.println(upcomingScheduledProcess));
    }

    @Test
    public void get_processed_jobs_for_agent() {
        List<String> agents = this.dao.getAllAgentNames();
        System.out.println(agents);

        agents.forEach(agent ->
            this.solrScheduledProcessService.getScheduledProcessEvents(agent, System.currentTimeMillis() - 100000000L,
                System.currentTimeMillis()).getResultList().forEach(upcomingScheduledProcess -> System.out.println(upcomingScheduledProcess)));
    }

    @Test
    public void get_processed_jobs() {
        this.solrScheduledProcessService.getScheduledProcessEvents(System.currentTimeMillis() - 100000L,
                System.currentTimeMillis(), "filter", false, 0, 1000).getResultList().forEach(upcomingScheduledProcess -> System.out.println(upcomingScheduledProcess));
    }
}
