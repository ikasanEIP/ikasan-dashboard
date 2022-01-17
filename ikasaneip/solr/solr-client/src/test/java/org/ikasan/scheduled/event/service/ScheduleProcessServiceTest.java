package org.ikasan.scheduled.event.service;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.scheduled.event.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.event.model.*;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ScheduleProcessServiceTest extends SolrTestCaseJ4 {

    private SolrScheduledProcessServiceImpl solrScheduledProcessService;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup()
    {
        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();

    }

    @After
    public void teardown() throws IOException
    {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        SolrScheduledProcessEventDao scheduledProcessEventDao = new SolrScheduledProcessEventDao();
        scheduledProcessEventDao.setSolrClient(server);

        SolrModuleMetadataDao solrModuleMetadataDao = new SolrModuleMetadataDao();
        solrModuleMetadataDao.setSolrClient(server);

        SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao = new SolrComponentConfigurationMetadataDao();
        solrComponentConfigurationMetadataDao.setSolrClient(server);

        SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao = new SolrBusinessStreamMetadataDao();
        solrBusinessStreamMetadataDao.setSolrClient(server);

        this.solrScheduledProcessService = new SolrScheduledProcessServiceImpl(scheduledProcessEventDao,
            solrModuleMetadataDao, solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_constructor_null_scheduled_proceess_event_dao() throws Exception {
        new SolrScheduledProcessServiceImpl(null,
            new SolrModuleMetadataDao(), new SolrComponentConfigurationMetadataDao(), new SolrBusinessStreamMetadataDao(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_constructor_null_module_metadata_dao() throws Exception {
        new SolrScheduledProcessServiceImpl(new SolrScheduledProcessEventDao(),
            null, new SolrComponentConfigurationMetadataDao(), new SolrBusinessStreamMetadataDao(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_constructor_null_component_configuration_metadata_dao() throws Exception {
        new SolrScheduledProcessServiceImpl(new SolrScheduledProcessEventDao(),
            new SolrModuleMetadataDao(), null, new SolrBusinessStreamMetadataDao(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_constructor_null_business_stream_metadata_dao() throws Exception {
        new SolrScheduledProcessServiceImpl(new SolrScheduledProcessEventDao(),
            new SolrModuleMetadataDao(), new SolrComponentConfigurationMetadataDao(), null, false);
    }

    @Test
    public void test_get_all_agent_names() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            List<String> agentNames = this.solrScheduledProcessService.getAllAgentNames();

            assertEquals(1, agentNames.size());

            agentNames.forEach(i -> Assert.assertTrue(agentNames.contains("scheduler-agent")));
        }
    }


    @Test
    public void test_get_flows_for_agent() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            List<FlowMetaData> flowMetaData = this.solrScheduledProcessService.getFlowsForAgent("scheduler-agent");

            Assert.assertEquals(   1, flowMetaData.size());
        }
    }

    @Test
    public void test_get_configuration_for_agent_flow_component() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ConfigurationMetaData<List<ConfigurationParameterMetaData>> componentConfiguration = this.solrScheduledProcessService.getConfigurationForAgentFlowComponent("scheduler-agent",
                "5 minute job", "Scheduled Consumer");

            Assert.assertNotNull(componentConfiguration);
            Assert.assertEquals(componentConfiguration.getParameters().stream()
                .filter(p -> p.getName().equals("cronExpression")).findFirst().get().getValue(), "0 0/5 * * * ?");
        }
    }

    @Test
    public void test_get_upcoming_scheduled_processes() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService.getUpComingScheduledProcesses("scheduler-agent",
                "5 minute job", System.currentTimeMillis(), System.currentTimeMillis() + 600000L, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test(expected = RuntimeException.class)
    public void test_get_upcoming_scheduled_processes_exception_null_consumer_configuration() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService.getUpComingScheduledProcesses("scheduler-agent",
                "5 minute job", System.currentTimeMillis(), System.currentTimeMillis() + 600000L, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test(expected = RuntimeException.class)
    public void test_get_upcoming_scheduled_processes_exception_null_bloackout_router_configuration() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService.getUpComingScheduledProcesses("scheduler-agent",
                "5 minute job", System.currentTimeMillis(), System.currentTimeMillis() + 600000L, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test(expected = RuntimeException.class)
    public void test_get_upcoming_scheduled_processes_exception_null_process_execution_broker_configuration() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService.getUpComingScheduledProcesses("scheduler-agent",
                "5 minute job", System.currentTimeMillis(), System.currentTimeMillis() + 600000L, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_upcoming_scheduled_processes_with_null_filter() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService
                .getUpComingScheduledProcesses(List.of("scheduler-agent"), System.currentTimeMillis(), System.currentTimeMillis() + 600000L, null, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_upcoming_scheduled_processes_with_filter_some_results() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService
                .getUpComingScheduledProcesses(List.of("scheduler-agent"), System.currentTimeMillis(), System.currentTimeMillis() + 600000L, "sched", 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_upcoming_scheduled_processes_with_filter_no_results() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService
                .getUpComingScheduledProcesses(List.of("scheduler-agent"), System.currentTimeMillis(), System.currentTimeMillis() + 600000L, "bad filter", 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(0, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_upcoming_scheduled_processes_with_null_accessible_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService
                .getUpComingScheduledProcesses(null, System.currentTimeMillis(), System.currentTimeMillis() + 600000L, null, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(2, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_upcoming_scheduled_processes_with_no_accessible_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = this.solrScheduledProcessService
                .getUpComingScheduledProcesses(List.of(), System.currentTimeMillis(), System.currentTimeMillis() + 600000L, null, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(0, upComingScheduledProcesses.getResultList().size());
        }

    }

    @Test
    public void test_get_schedule_process_aggregate_configurations_null_filter() throws Exception{
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> upComingScheduledProcesses = this.solrScheduledProcessService
                .getScheduleProcessAggregateConfigurations("scheduler-agent", null, 0, 100);

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(1, upComingScheduledProcesses.getResultList().size());
        }
    }

    @Test
    public void test_get_schedule_process_aggregate_configurations_filter_results_found() throws Exception{
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> upComingScheduledProcesses = this.solrScheduledProcessService
                .getScheduleProcessAggregateConfigurations("scheduler-agent", "5");

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(1, upComingScheduledProcesses.getResultList().size());
        }
    }

    @Test
    public void test_get_schedule_process_aggregate_configurations_filter_no_results_found() throws Exception{
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent");
            doc.addField("moduleName", "scheduler-agent");
            doc.addField("type", "moduleMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduler-agent-module-metadata.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Scheduled Consumer_-946268323_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/scheduler-consumer-configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Blackout Router_-176915388_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/blackout_router_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            doc = new SolrInputDocument();
            doc.addField("id", "scheduler-agent_5 minute job_Process Execution Broker_1959287546_C");
            doc.addField("type", "componentConfiguration");
            doc.addField("payload", this.loadDataFile("/data/process_execution_broker_configuration.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> upComingScheduledProcesses = this.solrScheduledProcessService
                .getScheduleProcessAggregateConfigurations("scheduler-agent", "bad filter");

            Assert.assertNotNull(upComingScheduledProcesses);
            Assert.assertEquals(0, upComingScheduledProcesses.getResultList().size());
        }
    }

    @Test
    public void test_get_business_streams_for_scheduled_job_found() throws Exception{
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "business stream");
            doc.addField("moduleName", "test business stream");
            doc.addField("type", "businessStreamMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduled-process-business-stream.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();


            List<BusinessStreamMetaData> businessStreams = this.solrScheduledProcessService
                .getBusinessStreams("scheduler-agent", "test-2");

            Assert.assertNotNull(businessStreams);
            Assert.assertEquals(1, businessStreams.size());
        }
    }

    @Test
    public void test_get_business_streams_for_scheduled_job_not_found() throws Exception{
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "business stream");
            doc.addField("moduleName", "test business stream");
            doc.addField("type", "businessStreamMetaData");
            doc.addField("payload", this.loadDataFile("/data/scheduled-process-business-stream.json"));
            doc.addField("timestamp", 100l);

            server.add("ikasan", doc);
            server.commit();


            List<BusinessStreamMetaData> businessStreams = this.solrScheduledProcessService
                .getBusinessStreams("scheduler-agent", "blah");

            Assert.assertNotNull(businessStreams);
            Assert.assertEquals(0, businessStreams.size());
        }
    }

    @Test
    public void test_get_scheduled_process_events() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(10, true);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents("myAgent", 0, System.currentTimeMillis() + 1000000L);

            assertEquals(10, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents("myAgent", 0, 200L);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents("myAgent", System.currentTimeMillis() + 1000000L, System.currentTimeMillis() + 2000000L);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_filtering() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(10, true);
            this.createScheduledEventRecords(5, false);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("bad agent name"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

        }
    }

    @Test
    public void test_get_scheduled_process_events_with_ascending_results() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(15, true);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "asc");

            ScheduledProcessEvent processEvent = scheduledProcessEventSearchResults.getResultList().get(0);

            for (int i=1; i<scheduledProcessEventSearchResults.getResultList().size(); i++) {
                Assert.assertTrue(scheduledProcessEventSearchResults.getResultList().get(i).getFireTime()
                    > processEvent.getFireTime());

                processEvent = scheduledProcessEventSearchResults.getResultList().get(i);
            }
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_desc_results() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(15, true);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "desc");

            ScheduledProcessEvent processEvent = scheduledProcessEventSearchResults.getResultList().get(0);

            for (int i=1; i<scheduledProcessEventSearchResults.getResultList().size(); i++) {
                Assert.assertTrue(scheduledProcessEventSearchResults.getResultList().get(i).getFireTime()
                    < processEvent.getFireTime());

                processEvent = scheduledProcessEventSearchResults.getResultList().get(i);
            }
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_null_order() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(15, true);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L
                , null, false, 0, 100, null);

            ScheduledProcessEvent processEvent = scheduledProcessEventSearchResults.getResultList().get(0);

            for (int i=1; i<scheduledProcessEventSearchResults.getResultList().size(); i++) {
                Assert.assertTrue(scheduledProcessEventSearchResults.getResultList().get(i).getFireTime()
                    < processEvent.getFireTime());

                processEvent = scheduledProcessEventSearchResults.getResultList().get(i);
            }
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_invalid_order_defaulting_to_desc() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.createScheduledEventRecords(15, true);

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = this.solrScheduledProcessService.getScheduledProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L
                , null, false, 0, 100, "invalid");

            ScheduledProcessEvent processEvent = scheduledProcessEventSearchResults.getResultList().get(0);

            for (int i=1; i<scheduledProcessEventSearchResults.getResultList().size(); i++) {
                Assert.assertTrue(scheduledProcessEventSearchResults.getResultList().get(i).getFireTime()
                    < processEvent.getFireTime());

                processEvent = scheduledProcessEventSearchResults.getResultList().get(i);
            }
        }
    }

    private void createScheduledEventRecords(int num, boolean success) {
        List<ScheduledProcessEvent> solrInputDocuments = new ArrayList<>();
        SolrScheduledProcessEventDao dao = new SolrScheduledProcessEventDao();

        IntStream.range(0, num)
            .forEach(i -> {
                SolrScheduledProcessEvent event = new SolrScheduledProcessEvent();
                event.setAgentName("myAgent");
                event.setCommandLine("commandLine"+i);
                event.setCompletionTime(1000L);
                event.setFireTime(System.currentTimeMillis());
                event.setJobDescription("jobDescription"+i);
                event.setJobGroup("jobGroup"+i);
                event.setJobName("jobName"+i);
                event.setNextFireTime(1000L);
                event.setOutcome(Outcome.EXECUTION_INVOKED);
                event.setPid(1234L);
                event.setResultError("error"+i);
                event.setResultOutput("output"+i);
                event.setReturnCode(0);
                event.setSuccessful(success);
                event.setUser("user"+i);

                solrInputDocuments.add(event);
                try {
                    Thread.sleep(2);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        this.solrScheduledProcessService.save(solrInputDocuments);
    }


    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}

