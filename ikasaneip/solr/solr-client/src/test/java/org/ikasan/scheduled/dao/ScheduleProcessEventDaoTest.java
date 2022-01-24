package org.ikasan.scheduled.dao;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.scheduled.model.Outcome;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.SolrScheduledProcessEvent;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.solr.SolrDaoBase;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ScheduleProcessEventDaoTest extends SolrTestCaseJ4 {

    private SolrScheduledProcessEventDao dao;

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

        dao = new SolrScheduledProcessEventDao();
        dao.setSolrClient(server);
    }

    @Test
    public void test_convert_entity_to_solr_input_document() {
        SolrScheduledProcessEventDao dao = new SolrScheduledProcessEventDao();

        SolrScheduledProcessEvent event = new SolrScheduledProcessEvent();
        event.setAgentName("agentName");
        event.setCommandLine("commandLine");
        event.setCompletionTime(1000L);
        event.setFireTime(1000L);
        event.setJobDescription("jobDescription");
        event.setJobGroup("jobGroup");
        event.setJobName("jobName");
        event.setNextFireTime(1000L);
        event.setOutcome(Outcome.EXECUTION_INVOKED);
        event.setPid(1234L);
        event.setResultError("error");
        event.setResultOutput("output");
        event.setReturnCode(0);
        event.setSuccessful(true);
        event.setUser("user");

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event);

        Assert.assertEquals("agentName-scheduledProcessEvent--652266522", solrInputDocument.getFieldValue(SolrDaoBase.ID));
        Assert.assertEquals("agentName", solrInputDocument.getFieldValue(SolrDaoBase.MODULE_NAME));
        Assert.assertEquals("scheduledProcessEvent", solrInputDocument.getFieldValue(SolrDaoBase.TYPE));
        Assert.assertEquals("jobName", solrInputDocument.getFieldValue(SolrDaoBase.COMPONENT_NAME));
        Assert.assertEquals("agentName", solrInputDocument.getFieldValue(SolrDaoBase.MODULE_NAME));
        Assert.assertEquals(1000L, solrInputDocument.getFieldValue(SolrDaoBase.CREATED_DATE_TIME));
        Assert.assertEquals("{\"agentName\":\"agentName\"," +
                "\"agentHostname\":null,\"jobName\":\"jobName\",\"jobGroup\":\"jobGroup\",\"jobDescription\":" +
                "\"jobDescription\",\"commandLine\":\"commandLine\",\"resultOutput\":\"output\",\"resultError\":" +
                "\"error\",\"pid\":1234,\"user\":\"user\",\"fireTime\":1000,\"nextFireTime\":1000,\"successful\":true," +
                "\"completionTime\":1000,\"returnCode\":0,\"outcome\":\"EXECUTION_INVOKED\"}",
            solrInputDocument.getFieldValue(SolrDaoBase.PAYLOAD_CONTENT));
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(SolrDaoBase.EXPIRY));
    }


    @Test
    public void test_get_all_agent_names() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            List<String> agentNames = dao.getAllAgentNames();

            assertEquals(10, agentNames.size());

            IntStream.range(0, 10)
                .forEach(i -> Assert.assertTrue(agentNames.contains("agentName"+i)));
        }
    }

    @Test
    public void test_get_scheduled_process_events() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", 0, System.currentTimeMillis() + 1000000L);

            assertEquals(10, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", 0, 200L);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", System.currentTimeMillis() + 1000000L, System.currentTimeMillis() + 2000000L);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_job_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", null, "jobName1"
                    , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(1, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", null, "bad -jobName1"
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_agent_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", null, null
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(10, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("bad", null, null
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_job_group_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(null, "jobGroup1", null
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(1, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(null, "bad", null
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }

    @Test
    public void test_get_scheduled_process_events_with_agent_job_and_job_group_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("myAgent", "jobGroup1", "jobName1"
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(1, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents("bad", "bad", "bad"
                , 0, System.currentTimeMillis() + 1000000L, 0, 1000, null);

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());
        }
    }



    @Test(expected = RuntimeException.class)
    public void test_get_scheduled_process_events_exception() {
            dao = new SolrScheduledProcessEventDao();
            dao.setSolrClient(null);
            dao.getScheduleProcessEvents("myAgent", 0, System.currentTimeMillis() + 1000000L);
    }

    @Test
    public void test_get_scheduled_process_events_with_filtering() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createScheduledEventRecords(5, false));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(15, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(null, 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(new ArrayList<>(), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", true, 0, 100, "desc");

            assertEquals(5, scheduledProcessEventSearchResults.getResultList().size());

            scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("bad agent name"), 0L, System.currentTimeMillis() + 2000000L, "myAgent", false, 0, 100, "desc");

            assertEquals(0, scheduledProcessEventSearchResults.getResultList().size());

        }
    }

    @Test
    public void test_get_scheduled_process_events_with_ascending_results() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createScheduledEventRecords(5, false));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "asc");

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

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createScheduledEventRecords(5, false));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "desc");

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

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createScheduledEventRecords(5, false));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, null);

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

            server.add("ikasan", this.createScheduledEventRecords(10, true));
            server.commit();
            server.add("ikasan", this.createScheduledEventRecords(5, false));
            server.commit();
            server.add("ikasan", this.createAgentRecords(10));
            server.commit();

            ScheduledProcessEventSearchResults<ScheduledProcessEvent> scheduledProcessEventSearchResults
                = dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L, null, false, 0, 100, "invalid");

            ScheduledProcessEvent processEvent = scheduledProcessEventSearchResults.getResultList().get(0);

            for (int i=1; i<scheduledProcessEventSearchResults.getResultList().size(); i++) {
                Assert.assertTrue(scheduledProcessEventSearchResults.getResultList().get(i).getFireTime()
                    < processEvent.getFireTime());

                processEvent = scheduledProcessEventSearchResults.getResultList().get(i);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void test_get_scheduled_process_events_with_filtering_exception() throws Exception {
        dao = new SolrScheduledProcessEventDao();
        dao.setSolrClient(null);
        dao.getScheduleProcessEvents(List.of("myAgent"), 0L, System.currentTimeMillis() + 1000000L
            , null, false, 0, 100, "desc");
    }

    private List<SolrInputDocument> createScheduledEventRecords(int num, boolean success) {
        List<SolrInputDocument> solrInputDocuments = new ArrayList<>();
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

                solrInputDocuments.add(dao.convertEntityToSolrInputDocument(1L, event));

                try {
                    Thread.sleep(2);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

        return solrInputDocuments;
    }

    private List<SolrInputDocument> createAgentRecords(int num) {
        List<SolrInputDocument> solrInputDocuments = new ArrayList<>();

        IntStream.range(0, num)
            .forEach(i -> {
                SolrInputDocument event = new SolrInputDocument();
                event.setField(SolrDaoBase.ID, "agentName"+i);
                event.setField(SolrDaoBase.MODULE_NAME, "agentName"+i);
                event.setField(SolrDaoBase.TYPE, "moduleMetaData");
                event.setField(SolrDaoBase.PAYLOAD_CONTENT, "{\"type\":\"SCHEDULER_AGENT\"}");


                solrInputDocuments.add(event);
            });

        return solrInputDocuments;
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
