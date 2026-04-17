package org.ikasan.scheduled.notification;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.scheduled.notification.service.SolrEmailNotificationDetailsServiceImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
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
import java.util.stream.IntStream;

public class SolrEmailNotificationDetailsServiceTest extends SolrTestCaseJ4 {

    private SolrEmailNotificationDetailsDaoImpl dao;
    private SolrEmailNotificationDetailsServiceImpl service;

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

        service = new SolrEmailNotificationDetailsServiceImpl(dao);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
            emailNotificationDetails.setJobName("job-1");
            emailNotificationDetails.setContextName("parent-context-1");
            emailNotificationDetails.setChildContextName("context-1");
            emailNotificationDetails.setMonitorType("ERROR");
            emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationDetails.setEmailBody("email-body-1");
            emailNotificationDetails.setEmailSubject("email-subject-1");
            emailNotificationDetails.setHtml(false);

            SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
            solrEmailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);
            solrEmailNotificationDetailsRecord.setModifiedBy("ikasan");

            this.service.save(solrEmailNotificationDetailsRecord);

            EmailNotificationDetailsRecord found = this.service.findByJobNameAndMonitorType("job-1", "context-1", "ERROR");
            EmailNotificationDetails foundEmailNotificationDetails = found.getEmailNotificationDetails();

            Assert.assertEquals("job-1", foundEmailNotificationDetails.getJobName());
            Assert.assertEquals("parent-context-1", foundEmailNotificationDetails.getContextName());
            Assert.assertEquals("context-1", foundEmailNotificationDetails.getChildContextName());
            Assert.assertEquals("email-body-1", foundEmailNotificationDetails.getEmailBody());
            Assert.assertEquals("email-subject-1", foundEmailNotificationDetails.getEmailSubject());
            Assert.assertEquals(false, foundEmailNotificationDetails.isHtml());

            Assert.assertNull(this.service.findByJobNameAndMonitorType("bad-job", "context-1", "ERROR"));
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
            emailNotificationDetails.setJobName("job-1");
            emailNotificationDetails.setContextName("parent-context-1");
            emailNotificationDetails.setChildContextName("context-1");
            emailNotificationDetails.setMonitorType("ERROR");
            emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationDetails.setEmailBody("email-body-1");
            emailNotificationDetails.setEmailSubject("email-subject-1");
            emailNotificationDetails.setHtml(false);

            SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
            solrEmailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);
            solrEmailNotificationDetailsRecord.setModifiedBy("ikasan");

            this.service.save(solrEmailNotificationDetailsRecord);

            SearchResults<EmailNotificationDetailsRecord> found =  this.service.findAll(100,0);

            Assert.assertEquals(1, found.getResultList().size());

            Assert.assertEquals("job-1", found.getResultList().get(0).getEmailNotificationDetails().getJobName());
            Assert.assertEquals("parent-context-1", found.getResultList().get(0).getEmailNotificationDetails().getContextName());
            Assert.assertEquals("context-1", found.getResultList().get(0).getEmailNotificationDetails().getChildContextName());
            Assert.assertEquals("email-body-1", found.getResultList().get(0).getEmailNotificationDetails().getEmailBody());
            Assert.assertEquals("email-subject-1", found.getResultList().get(0).getEmailNotificationDetails().getEmailSubject());
            Assert.assertEquals(false, found.getResultList().get(0).getEmailNotificationDetails().isHtml());
        }
    }

    @Test
    public void test_delete_by_context_id() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.addRecords("Context-One", 10);
            this.addRecords("Context-Two", 15);
            this.addRecords("Context-Three", 5);

            SearchResults<EmailNotificationDetailsRecord> found =  this.service.findAll(100,0);
            Assert.assertEquals(30, found.getResultList().size());

            this.service.deleteByContextName("Context-One");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(20, found.getResultList().size());

            this.service.deleteByContextName("Context-Three");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(15, found.getResultList().size());

            this.service.deleteByContextName("Context-Three"); // Delete Context-Three again, but it will not do anything
            found =  this.service.findAll(100,0);
            Assert.assertEquals(15, found.getResultList().size());

            this.service.deleteByContextName("Context-Two");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(0, found.getResultList().size());
        }
    }

    @Test
    public void test_delete_by_job_id_context_monitor_type() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            this.addRecords("Context-One", 10);
            this.addRecords("Context-Two", 15);
            this.addRecords("Context-Three", 5);

            SearchResults<EmailNotificationDetailsRecord> found =  this.service.findAll(100,0);
            Assert.assertEquals(30, found.getResultList().size());

            // DELETE one
            this.service.deleteByJobNameAndMonitorType("Context-One-job-2", "Context-One-child-2", "ERROR");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(29, found.getResultList().size());

            //DELETE the same one, nothing changed
            this.service.deleteByJobNameAndMonitorType("Context-One-job-2", "Context-One-child-2", "ERROR");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(29, found.getResultList().size());

            // DELETE one that does not exist, nothing changed
            this.service.deleteByJobNameAndMonitorType("Context-One-job-5", "Context-One-child-6", "ERROR");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(29, found.getResultList().size());

            // DELETE one that does exist
            this.service.deleteByJobNameAndMonitorType("Context-One-job-5", "Context-One-child-5", "ERROR");
            found =  this.service.findAll(100,0);
            Assert.assertEquals(28, found.getResultList().size());
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

    private void addRecords(String contextName, int size) {
        IntStream.range(0, size).forEach(i -> {
            EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
            emailNotificationDetails.setJobName(contextName + "-job-" + i);
            emailNotificationDetails.setContextName(contextName);
            emailNotificationDetails.setChildContextName(contextName + "-child-" + i);
            emailNotificationDetails.setMonitorType("ERROR");
            emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationDetails.setEmailBody("email-body-1");
            emailNotificationDetails.setEmailSubject("email-subject-1");
            emailNotificationDetails.setHtml(false);

            SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
            solrEmailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);
            solrEmailNotificationDetailsRecord.setModifiedBy("ikasan");

            this.service.save(solrEmailNotificationDetailsRecord);
        });
    }
}
