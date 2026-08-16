package org.ikasan.mongo.persistence.scheduled.notification.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationDetailsRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationDetailsRepository;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MongoEmailNotificationDetailsDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoEmailNotificationDetailsDao dao;
    private MongoEmailNotificationDetailsRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoEmailNotificationDetailsRepository.class);

        dao = new MongoEmailNotificationDetailsDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoEmailNotificationDetailsRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_findAll() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");

        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("job1", results.getResultList().get(0).getJobName());
        assertEquals("context1", results.getResultList().get(0).getContextName());
        assertEquals("RUNNING", results.getResultList().get(0).getMonitorType());
    }

    @Test
    public void test_save_multiple_and_findAll_with_pagination() {
        for (int i = 0; i < 5; i++) {
            EmailNotificationDetailsRecord record = createTestRecord("job" + i, "context" + i, "child" + i, "RUNNING");
            dao.save(record);
        }

        SearchResults<EmailNotificationDetailsRecord> page1 = dao.findAll(2, 0);
        assertEquals(2, page1.getResultList().size());
        assertEquals(5L, page1.getTotalNumberOfResults());

        SearchResults<EmailNotificationDetailsRecord> page2 = dao.findAll(2, 2);
        assertEquals(2, page2.getResultList().size());
        assertEquals(5L, page2.getTotalNumberOfResults());

        SearchResults<EmailNotificationDetailsRecord> page3 = dao.findAll(2, 4);
        assertEquals(1, page3.getResultList().size());
        assertEquals(5L, page3.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName() {
        EmailNotificationDetailsRecord record1 = createTestRecord("job1", "context1", "child1", "RUNNING");
        EmailNotificationDetailsRecord record2 = createTestRecord("job2", "context2", "child2", "STOPPED");
        EmailNotificationDetailsRecord record3 = createTestRecord("job3", "context1", "child3", "FAILED");

        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("context1", 10, 0);
        assertEquals(2, results.getResultList().size());
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName_not_found() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");
        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findByContextName("nonexistent", 10, 0);
        assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_findByJobNameAndMonitorType() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");
        dao.save(record);

        EmailNotificationDetailsRecord found = dao.findByJobNameAndMonitorType("job1", "child1", "RUNNING");
        assertNotNull(found);
        assertEquals("job1", found.getJobName());
        assertEquals("context1", found.getContextName());
        assertEquals("RUNNING", found.getMonitorType());
    }

    @Test
    public void test_findByJobNameAndMonitorType_not_found() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");
        dao.save(record);

        EmailNotificationDetailsRecord found = dao.findByJobNameAndMonitorType("nonexistent", "child1", "RUNNING");
        assertNull(found);
    }

    @Test
    public void test_deleteByContextName() {
        EmailNotificationDetailsRecord record1 = createTestRecord("job1", "context1", "child1", "RUNNING");
        EmailNotificationDetailsRecord record2 = createTestRecord("job2", "context2", "child2", "STOPPED");

        dao.save(record1);
        dao.save(record2);

        SearchResults<EmailNotificationDetailsRecord> beforeDelete = dao.findAll(10, 0);
        assertEquals(2, beforeDelete.getResultList().size());

        dao.deleteByContextName("context1");

        SearchResults<EmailNotificationDetailsRecord> afterDelete = dao.findAll(10, 0);
        assertEquals(1, afterDelete.getResultList().size());
        assertEquals("context2", afterDelete.getResultList().get(0).getContextName());
    }

    @Test
    public void test_deleteByJobNameAndMonitorType() {
        EmailNotificationDetailsRecord record1 = createTestRecord("job1", "context1", "child1", "RUNNING");
        EmailNotificationDetailsRecord record2 = createTestRecord("job2", "context2", "child2", "STOPPED");

        dao.save(record1);
        dao.save(record2);

        dao.deleteByJobNameAndMonitorType("job1", "child1", "RUNNING");

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("job2", results.getResultList().get(0).getJobName());
    }

    @Test
    public void test_save_generates_id() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");

        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertNotNull(results.getResultList().get(0).getId());
        assertEquals("job1_child1_RUNNING", results.getResultList().get(0).getId());
    }

    @Test
    public void test_save_sets_timestamps() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");

        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertTrue(results.getResultList().get(0).getTimestamp() > 0);
        assertTrue(results.getResultList().get(0).getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_updates_existing_record() {
        EmailNotificationDetailsRecord record = createTestRecord("job1", "context1", "child1", "RUNNING");
        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results1 = dao.findAll(10, 0);
        assertEquals(1, results1.getResultList().size());
        long timestamp1 = results1.getResultList().get(0).getModifiedTimestamp();

        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EmailNotificationDetailsRecord updatedRecord = createTestRecord("job1", "context1", "child1", "RUNNING");
        updatedRecord.setModifiedBy("updatedUser");
        dao.save(updatedRecord);

        SearchResults<EmailNotificationDetailsRecord> results2 = dao.findAll(10, 0);
        assertEquals(1, results2.getResultList().size());
        assertEquals("updatedUser", results2.getResultList().get(0).getModifiedBy());
        assertTrue(results2.getResultList().get(0).getModifiedTimestamp() > timestamp1);
    }

    @Test
    public void test_insert() {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            records.add(createTestRecord("job" + i, "context" + i, "child" + i, "RUNNING"));
        }

        dao.insert(records);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_save_with_complex_email_details() {
        EmailNotificationDetails emailDetails = createComplexEmailDetails("job1", "context1", "child1", "RUNNING");

        EmailNotificationDetailsRecord record = new EmailNotificationDetailsRecordImpl();
        record.setEmailNotificationDetails(emailDetails);
        record.setModifiedBy("testUser");

        dao.save(record);

        SearchResults<EmailNotificationDetailsRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());

        EmailNotificationDetails retrieved = results.getResultList().get(0).getEmailNotificationDetails();
        assertEquals("job1", retrieved.getJobName());
        assertEquals("context1", retrieved.getContextName());
        assertEquals("child1", retrieved.getChildContextName());
        assertEquals("RUNNING", retrieved.getMonitorType());
        assertEquals(2, retrieved.getEmailSendTo().size());
        assertTrue(retrieved.getEmailSendTo().contains("test1@example.com"));
        assertTrue(retrieved.getEmailSendTo().contains("test2@example.com"));
        assertTrue(retrieved.isHtml());
    }

    private EmailNotificationDetailsRecord createTestRecord(String jobName, String contextName, String childContextName, String monitorType) {
        EmailNotificationDetailsRecord record = new EmailNotificationDetailsRecordImpl();
        EmailNotificationDetails emailDetails = createTestEmailDetails(jobName, contextName, childContextName, monitorType);
        record.setEmailNotificationDetails(emailDetails);
        record.setModifiedBy("testUser");
        return record;
    }

    private EmailNotificationDetails createTestEmailDetails(String jobName, String contextName, String childContextName, String monitorType) {
        EmailNotificationDetailsImpl emailDetails = new EmailNotificationDetailsImpl();
        emailDetails.setJobName(jobName);
        emailDetails.setContextName(contextName);
        emailDetails.setChildContextName(childContextName);
        emailDetails.setMonitorType(monitorType);

        List<String> emailSendTo = new ArrayList<>();
        emailSendTo.add("test@example.com");
        emailDetails.setEmailSendTo(emailSendTo);

        emailDetails.setEmailSubject("Test Subject");
        emailDetails.setEmailBody("Test Body");
        emailDetails.setHtml(false);

        return emailDetails;
    }

    private EmailNotificationDetails createComplexEmailDetails(String jobName, String contextName, String childContextName, String monitorType) {
        EmailNotificationDetailsImpl emailDetails = new EmailNotificationDetailsImpl();
        emailDetails.setJobName(jobName);
        emailDetails.setContextName(contextName);
        emailDetails.setChildContextName(childContextName);
        emailDetails.setMonitorType(monitorType);

        List<String> emailSendTo = new ArrayList<>();
        emailSendTo.add("test1@example.com");
        emailSendTo.add("test2@example.com");
        emailDetails.setEmailSendTo(emailSendTo);

        List<String> emailSendCc = new ArrayList<>();
        emailSendCc.add("cc@example.com");
        emailDetails.setEmailSendCc(emailSendCc);

        List<String> emailSendBcc = new ArrayList<>();
        emailSendBcc.add("bcc@example.com");
        emailDetails.setEmailSendBcc(emailSendBcc);

        emailDetails.setEmailSubject("Complex Subject");
        emailDetails.setEmailBody("Complex Body");
        emailDetails.setEmailSubjectTemplate("Subject Template");
        emailDetails.setEmailBodyTemplate("Body Template");

        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("param1", "value1");
        templateParams.put("param2", "value2");
        emailDetails.setEmailNotificationTemplateParameters(templateParams);

        emailDetails.setAttachment("attachment.txt");
        emailDetails.setHtml(true);

        return emailDetails;
    }
}
