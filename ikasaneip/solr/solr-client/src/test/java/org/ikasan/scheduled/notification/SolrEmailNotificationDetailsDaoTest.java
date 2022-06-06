package org.ikasan.scheduled.notification;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetails;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class SolrEmailNotificationDetailsDaoTest extends SolrTestCaseJ4 {

    private SolrEmailNotificationDetailsDaoImpl dao;

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

        dao = new SolrEmailNotificationDetailsDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
            emailNotificationDetails.setJobName("job-1");
            emailNotificationDetails.setMonitorType("ERROR");
            emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationDetails.setEmailBody("email-body-1");
            emailNotificationDetails.setEmailSubject("email-subject-1");
            emailNotificationDetails.setHtml(false);

            SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
            solrEmailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);
            solrEmailNotificationDetailsRecord.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationDetailsRecord);

            SolrEmailNotificationDetailsRecord found = this.dao.findByJobNameAndMonitorType("job-1", "ERROR");
            EmailNotificationDetails foundEmailNotificationDetails = found.getEmailNotificationDetails();

            Assert.assertEquals("job-1", foundEmailNotificationDetails.getJobName());
            Assert.assertEquals("email-body-1", foundEmailNotificationDetails.getEmailBody());
            Assert.assertEquals("email-subject-1", foundEmailNotificationDetails.getEmailSubject());
            Assert.assertEquals(false, foundEmailNotificationDetails.isHtml());

            Assert.assertNull(this.dao.findByJobNameAndMonitorType("bad-job", "ERROR"));
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
            emailNotificationDetails.setJobName("job-1");
            emailNotificationDetails.setMonitorType("ERROR");
            emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationDetails.setEmailBody("email-body-1");
            emailNotificationDetails.setEmailSubject("email-subject-1");
            emailNotificationDetails.setHtml(false);

            SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
            solrEmailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);
            solrEmailNotificationDetailsRecord.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationDetailsRecord);

            SearchResults<SolrEmailNotificationDetailsRecord> found =  this.dao.findAll(100,0);

            Assert.assertEquals(1, found.getResultList().size());

            Assert.assertEquals("job-1", found.getResultList().get(0).getEmailNotificationDetails().getJobName());
            Assert.assertEquals("email-body-1", found.getResultList().get(0).getEmailNotificationDetails().getEmailBody());
            Assert.assertEquals("email-subject-1", found.getResultList().get(0).getEmailNotificationDetails().getEmailSubject());
            Assert.assertEquals(false, found.getResultList().get(0).getEmailNotificationDetails().isHtml());
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
