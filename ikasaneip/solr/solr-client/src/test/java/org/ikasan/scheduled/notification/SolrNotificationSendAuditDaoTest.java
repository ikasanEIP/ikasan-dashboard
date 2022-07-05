package org.ikasan.scheduled.notification;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.notification.dao.SolrNotificationSendAuditDaoImpl;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAudit;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolrNotificationSendAuditDaoTest extends SolrTestCaseJ4 {

    private SolrNotificationSendAuditDaoImpl dao;

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

        dao = new SolrNotificationSendAuditDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
            notificationSendAudit.setJobName("job-1");
            notificationSendAudit.setMonitorType("ERROR");
            notificationSendAudit.setNotifierType("Email");
            notificationSendAudit.setContextInstanceId("instance-1");
            notificationSendAudit.setContextName("name-1");
            notificationSendAudit.setNotificationSend(true);

            NotificationSendAuditRecord record = new SolrNotificationSendAuditRecord();
            record.setNotificationSendAudit(notificationSendAudit);
            record.setModifiedBy("ikasan");

            this.dao.save(record);

            NotificationSendAuditRecord found = this.dao.find("instance-1", "name-1","job-1", "ERROR", "Email");
            NotificationSendAudit foundNotificationSendAudit = found.getNotificationSendAudit();

            Assert.assertEquals("job-1", foundNotificationSendAudit.getJobName());
            Assert.assertEquals("instance-1", foundNotificationSendAudit.getContextInstanceId());
            Assert.assertEquals("name-1", foundNotificationSendAudit.getContextName());
            Assert.assertEquals("ERROR", foundNotificationSendAudit.getMonitorType());
            Assert.assertEquals("Email", foundNotificationSendAudit.getNotifierType());
            Assert.assertEquals(true, foundNotificationSendAudit.isNotificationSend());

            Assert.assertNull(this.dao.find("bad-instance-1", "name-1","job-1", "ERROR", "Email"));
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
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


}
