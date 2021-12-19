package org.ikasan.dashboard.notification.scheduler.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationService;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDao;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class SchedulerNotificationServiceTest extends SolrTestCaseJ4 {

    public static final String BUSINESS_STREAM_PAYLOAD = "/data/graph/wriggle3.json";

    private SolrGeneralDaoImpl dao;

    private NodeConfig config;

    @Before
    public void setup()
    {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.WARN);

        config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();


    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrGeneralDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    @DirtiesContext
    public void test_search_scheduled_job_fail() throws Exception {


        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.initialiseOneSucceessOneFailData(server);

            SchedulerNotificationService schedulerNotificationService = this.initialiseService(server);
            Optional<List<IkasanSolrDocument>> failedScheduledJobs
                = schedulerNotificationService.getFailedScheduledJobs("schedulerAgent1", 0L, 1000);

            assertTrue("Failed exclusions found!", failedScheduledJobs.isPresent());
            assertEquals(1, failedScheduledJobs.get().size());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_scheduled_no_fail() throws Exception {


        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.initialiseNoFailData(server);

            SchedulerNotificationService schedulerNotificationService = this.initialiseService(server);
            Optional<List<IkasanSolrDocument>> failedScheduledJobs
                = schedulerNotificationService.getFailedScheduledJobs("schedulerAgent1", 0L, 1000);

            assertFalse("Failed exclusions found!", failedScheduledJobs.isPresent());
        }
    }


    private SchedulerNotificationService initialiseService(EmbeddedSolrServer server) {
        SolrGeneralDaoImpl solrGeneralDao = new SolrGeneralDaoImpl();
        solrGeneralDao.setSolrClient(server);

        SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao = new SolrBusinessStreamMetadataDao();
        solrBusinessStreamMetadataDao.setSolrClient(server);

        SolrErrorReportingServiceDao solrErrorReportingServiceDao = new SolrErrorReportingServiceDao();
        solrErrorReportingServiceDao.setSolrClient(server);

        SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(solrGeneralDao);

        return new SchedulerNotificationService(solrGeneralService);
    }


    private void initialiseOneSucceessOneFailData(EmbeddedSolrServer server) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent1");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent2");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccessFail.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        server.commit();
    }

    private void initialiseNoFailData(EmbeddedSolrServer server) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent1");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent2");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        server.commit();
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
