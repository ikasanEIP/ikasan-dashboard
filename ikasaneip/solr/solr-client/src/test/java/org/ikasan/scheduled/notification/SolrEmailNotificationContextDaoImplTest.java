package org.ikasan.scheduled.notification;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationContextDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextRecordImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrEmailNotificationContextDaoImplTest extends SolrTestCaseJ4 {

    private SolrEmailNotificationContextDaoImpl dao;

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
    public void teardown() throws IOException {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrEmailNotificationContextDaoImpl();
        dao.setSolrClient(server);
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }

    protected String loadDataFile(String fileName) throws IOException {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException {
        return getClass().getResourceAsStream(fileName);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            Map<String, String> bodyTemplate = new HashMap<>();
            bodyTemplate.put("ERROR", "someBodyLocation.txt");

            Map<String, String> subjectTemplate = new HashMap<>();
            subjectTemplate.put("ERROR", "someSubjectLocation.txt");

            EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();
            emailNotificationContext.setContextName("parent-context-1");
            emailNotificationContext.setMonitorTypes(List.of("ERROR"));
            emailNotificationContext.setEmailBodyNotificationTemplate(bodyTemplate);
            emailNotificationContext.setEmailSubjectNotificationTemplate(subjectTemplate);
            emailNotificationContext.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationContext.setHtml(false);

            EmailNotificationContextRecord solrEmailNotificationContextRecord = new SolrEmailNotificationContextRecordImpl();
            solrEmailNotificationContextRecord.setEmailNotificationContext(emailNotificationContext);
            solrEmailNotificationContextRecord.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationContextRecord);

            SearchResults<EmailNotificationContextRecord> found = this.dao.findByContextName("parent-context-1", 100, 0);
            EmailNotificationContextRecord foundResults = found.getResultList().get(0);

            Assert.assertEquals("parent-context-1", foundResults.getId());
            Assert.assertEquals("parent-context-1", foundResults.getContextName());
            Assert.assertEquals("ikasan", foundResults.getModifiedBy());

            EmailNotificationContext foundEmailContext = foundResults.getEmailNotificationContext();
            Assert.assertEquals("parent-context-1", foundEmailContext.getContextName());
            Assert.assertTrue(foundEmailContext.getMonitorTypes().contains("ERROR"));
            Assert.assertEquals("someBodyLocation.txt", foundEmailContext.getEmailBodyNotificationTemplate().get("ERROR"));
            Assert.assertEquals("someSubjectLocation.txt", foundEmailContext.getEmailSubjectNotificationTemplate().get("ERROR"));
            Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-1"));
            Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-2"));
            Assert.assertFalse(foundEmailContext.isHtml());
        }
    }

    @Test
    public void test_save_and_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            Map<String, String> bodyTemplate = new HashMap<>();
            bodyTemplate.put("ERROR", "someBodyLocation.txt");

            Map<String, String> subjectTemplate = new HashMap<>();
            subjectTemplate.put("ERROR", "someSubjectLocation.txt");

            EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();
            emailNotificationContext.setContextName("parent-context-1");
            emailNotificationContext.setMonitorTypes(List.of("ERROR"));
            emailNotificationContext.setEmailBodyNotificationTemplate(bodyTemplate);
            emailNotificationContext.setEmailSubjectNotificationTemplate(subjectTemplate);
            emailNotificationContext.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationContext.setHtml(false);

            EmailNotificationContextRecord solrEmailNotificationContextRecord = new SolrEmailNotificationContextRecordImpl();
            solrEmailNotificationContextRecord.setEmailNotificationContext(emailNotificationContext);
            solrEmailNotificationContextRecord.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationContextRecord);

            EmailNotificationContext emailNotificationContext2 = new SolrEmailNotificationContextImpl();
            emailNotificationContext2.setContextName("parent-context-2");
            emailNotificationContext2.setMonitorTypes(List.of("ERROR"));
            emailNotificationContext2.setEmailBodyNotificationTemplate(bodyTemplate);
            emailNotificationContext2.setEmailSubjectNotificationTemplate(subjectTemplate);
            emailNotificationContext2.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationContext2.setHtml(false);

            EmailNotificationContextRecord solrEmailNotificationContextRecord2 = new SolrEmailNotificationContextRecordImpl();
            solrEmailNotificationContextRecord2.setEmailNotificationContext(emailNotificationContext2);
            solrEmailNotificationContextRecord2.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationContextRecord2);

            SearchResults<EmailNotificationContextRecord> found = this.dao.findAll( 100, 0);


            for (EmailNotificationContextRecord foundResults : found.getResultList()) {
                if (foundResults.getId().equals("parent-context-1")) {
                    Assert.assertEquals("parent-context-1", foundResults.getId());
                    Assert.assertEquals("parent-context-1", foundResults.getContextName());
                    Assert.assertEquals("ikasan", foundResults.getModifiedBy());
                    EmailNotificationContext foundEmailContext = foundResults.getEmailNotificationContext();
                    Assert.assertEquals("parent-context-1", foundEmailContext.getContextName());
                    Assert.assertTrue(foundEmailContext.getMonitorTypes().contains("ERROR"));
                    Assert.assertEquals("someBodyLocation.txt", foundEmailContext.getEmailBodyNotificationTemplate().get("ERROR"));
                    Assert.assertEquals("someSubjectLocation.txt", foundEmailContext.getEmailSubjectNotificationTemplate().get("ERROR"));
                    Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-1"));
                    Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-2"));
                    Assert.assertFalse(foundEmailContext.isHtml());
                } else {
                    Assert.assertEquals("parent-context-2", foundResults.getId());
                    Assert.assertEquals("parent-context-2", foundResults.getContextName());
                    Assert.assertEquals("ikasan", foundResults.getModifiedBy());
                    EmailNotificationContext foundEmailContext = foundResults.getEmailNotificationContext();
                    Assert.assertEquals("parent-context-2", foundEmailContext.getContextName());
                    Assert.assertTrue(foundEmailContext.getMonitorTypes().contains("ERROR"));
                    Assert.assertEquals("someBodyLocation.txt", foundEmailContext.getEmailBodyNotificationTemplate().get("ERROR"));
                    Assert.assertEquals("someSubjectLocation.txt", foundEmailContext.getEmailSubjectNotificationTemplate().get("ERROR"));
                    Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-1"));
                    Assert.assertTrue(foundEmailContext.getEmailSendTo().contains("to-2"));
                    Assert.assertFalse(foundEmailContext.isHtml());
                }
            }
        }
    }

    @Test
    public void test_delete_context_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            Map<String, String> bodyTemplate = new HashMap<>();
            bodyTemplate.put("ERROR", "someBodyLocation.txt");

            Map<String, String> subjectTemplate = new HashMap<>();
            subjectTemplate.put("ERROR", "someSubjectLocation.txt");

            EmailNotificationContext emailNotificationContext = new SolrEmailNotificationContextImpl();
            emailNotificationContext.setContextName("parent-context-1");
            emailNotificationContext.setMonitorTypes(List.of("ERROR"));
            emailNotificationContext.setEmailBodyNotificationTemplate(bodyTemplate);
            emailNotificationContext.setEmailSubjectNotificationTemplate(subjectTemplate);
            emailNotificationContext.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationContext.setHtml(false);

            EmailNotificationContextRecord solrEmailNotificationContextRecord = new SolrEmailNotificationContextRecordImpl();
            solrEmailNotificationContextRecord.setEmailNotificationContext(emailNotificationContext);
            solrEmailNotificationContextRecord.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationContextRecord);

            EmailNotificationContext emailNotificationContext2 = new SolrEmailNotificationContextImpl();
            emailNotificationContext2.setContextName("parent-context-2");
            emailNotificationContext2.setMonitorTypes(List.of("ERROR"));
            emailNotificationContext2.setEmailBodyNotificationTemplate(bodyTemplate);
            emailNotificationContext2.setEmailSubjectNotificationTemplate(subjectTemplate);
            emailNotificationContext2.setEmailSendTo(Arrays.asList("to-1", "to-2"));
            emailNotificationContext2.setHtml(false);

            EmailNotificationContextRecord solrEmailNotificationContextRecord2 = new SolrEmailNotificationContextRecordImpl();
            solrEmailNotificationContextRecord2.setEmailNotificationContext(emailNotificationContext2);
            solrEmailNotificationContextRecord2.setModifiedBy("ikasan");

            this.dao.save(solrEmailNotificationContextRecord2);

            SearchResults<EmailNotificationContextRecord> found = this.dao.findAll( 100, 0);

            Assert.assertEquals(2, found.getResultList().size());

            // delete parent context 2
            this.dao.deleteByContextName("parent-context-2");
            found = this.dao.findAll( 100, 0);
            Assert.assertEquals(1, found.getResultList().size());

            // Check that the one we have left is parent context 1
            Assert.assertEquals("parent-context-1", found.getResultList().get(0).getContextName());;

            // Delete parent-context-1
            this.dao.deleteByContextName("parent-context-1");
            found = this.dao.findAll( 100, 0);
            Assert.assertEquals(0, found.getResultList().size());

        }
    }
}
