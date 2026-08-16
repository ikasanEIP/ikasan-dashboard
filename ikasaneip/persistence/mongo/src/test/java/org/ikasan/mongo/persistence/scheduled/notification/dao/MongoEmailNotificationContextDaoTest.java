package org.ikasan.mongo.persistence.scheduled.notification.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationContextRepository;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
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

public class MongoEmailNotificationContextDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoEmailNotificationContextDao dao;
    private MongoEmailNotificationContextRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoEmailNotificationContextRepository.class);

        dao = new MongoEmailNotificationContextDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoEmailNotificationContextRecordImpl.class);
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
        EmailNotificationContextRecord record = createTestRecord("context1", "user1");

        dao.save(record);

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("context1", results.getResultList().get(0).getContextName());
        assertEquals("user1", results.getResultList().get(0).getModifiedBy());
    }

    @Test
    public void test_save_multiple_and_findAll_with_pagination() {
        for (int i = 0; i < 5; i++) {
            EmailNotificationContextRecord record = createTestRecord("context" + i, "user" + i);
            dao.save(record);
        }

        SearchResults<EmailNotificationContextRecord> page1 = dao.findAll(2, 0);
        assertEquals(2, page1.getResultList().size());
        assertEquals(5L, page1.getTotalNumberOfResults());

        SearchResults<EmailNotificationContextRecord> page2 = dao.findAll(2, 2);
        assertEquals(2, page2.getResultList().size());
        assertEquals(5L, page2.getTotalNumberOfResults());

        SearchResults<EmailNotificationContextRecord> page3 = dao.findAll(2, 4);
        assertEquals(1, page3.getResultList().size());
        assertEquals(5L, page3.getTotalNumberOfResults());
    }

    @Test
    public void test_findByContextName() {
        EmailNotificationContextRecord record1 = createTestRecord("context1", "user1");
        EmailNotificationContextRecord record2 = createTestRecord("context2", "user2");
        EmailNotificationContextRecord record3 = createTestRecord("context1", "user3");

        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("context1", 10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("context1", results.getResultList().get(0).getContextName());
    }

    @Test
    public void test_findByContextName_not_found() {
        EmailNotificationContextRecord record = createTestRecord("context1", "user1");
        dao.save(record);

        SearchResults<EmailNotificationContextRecord> results = dao.findByContextName("nonexistent", 10, 0);
        assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_findByContextName_with_pagination() {
        for (int i = 0; i < 5; i++) {
            EmailNotificationContextRecord record = createTestRecord("context1", "user" + i);
            dao.save(record);
        }

        SearchResults<EmailNotificationContextRecord> page1 = dao.findByContextName("context1", 2, 0);
        assertEquals(1, page1.getResultList().size());
        assertEquals(1L, page1.getTotalNumberOfResults());
    }

    @Test
    public void test_deleteByContextName() {
        EmailNotificationContextRecord record1 = createTestRecord("context1", "user1");
        EmailNotificationContextRecord record2 = createTestRecord("context2", "user2");

        dao.save(record1);
        dao.save(record2);

        SearchResults<EmailNotificationContextRecord> beforeDelete = dao.findAll(10, 0);
        assertEquals(2, beforeDelete.getResultList().size());

        dao.deleteByContextName("context1");

        SearchResults<EmailNotificationContextRecord> afterDelete = dao.findAll(10, 0);
        assertEquals(1, afterDelete.getResultList().size());
        assertEquals("context2", afterDelete.getResultList().get(0).getContextName());
    }

    @Test
    public void test_deleteByContextName_not_found() {
        EmailNotificationContextRecord record = createTestRecord("context1", "user1");
        dao.save(record);

        dao.deleteByContextName("nonexistent");

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_save_with_mongo_record() {
        MongoEmailNotificationContextRecordImpl mongoRecord = new MongoEmailNotificationContextRecordImpl();
        mongoRecord.setId("context1");
        mongoRecord.setContextName("context1");

        EmailNotificationContext emailContext = createTestEmailContext("context1");
        mongoRecord.setEmailNotificationContext(emailContext);
        mongoRecord.setModifiedBy("testUser");

        dao.save(mongoRecord);

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertEquals("context1", results.getResultList().get(0).getContextName());
    }

    @Test
    public void test_save_updates_existing_record() {
        EmailNotificationContextRecord record = createTestRecord("context1", "user1");
        dao.save(record);

        SearchResults<EmailNotificationContextRecord> results1 = dao.findAll(10, 0);
        assertEquals(1, results1.getResultList().size());
        long timestamp1 = results1.getResultList().get(0).getModifiedTimestamp();

        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EmailNotificationContextRecord updatedRecord = createTestRecord("context1", "user2");
        dao.save(updatedRecord);

        SearchResults<EmailNotificationContextRecord> results2 = dao.findAll(10, 0);
        assertEquals(1, results2.getResultList().size());
        assertEquals("user2", results2.getResultList().get(0).getModifiedBy());
        assertTrue(results2.getResultList().get(0).getModifiedTimestamp() > timestamp1);
    }

    @Test
    public void test_save_sets_timestamps() {
        EmailNotificationContextRecord record = createTestRecord("context1", "user1");

        dao.save(record);

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());
        assertTrue(results.getResultList().get(0).getTimestamp() > 0);
        assertTrue(results.getResultList().get(0).getModifiedTimestamp() > 0);
    }

    @Test
    public void test_insert() {
        List<EmailNotificationContextRecord> records = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            records.add(createTestRecord("context" + i, "user" + i));
        }

        dao.insert(records);

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_save_with_complex_email_context() {
        EmailNotificationContext emailContext = createComplexEmailContext("complexContext");

        EmailNotificationContextRecord record = new EmailNotificationContextRecordImpl();
        record.setEmailNotificationContext(emailContext);
        record.setModifiedBy("testUser");

        dao.save(record);

        SearchResults<EmailNotificationContextRecord> results = dao.findAll(10, 0);
        assertEquals(1, results.getResultList().size());

        EmailNotificationContext retrievedContext = results.getResultList().get(0).getEmailNotificationContext();
        assertEquals("complexContext", retrievedContext.getContextName());
        assertEquals(2, retrievedContext.getMonitorTypes().size());
        assertTrue(retrievedContext.getMonitorTypes().contains("type1"));
        assertTrue(retrievedContext.getMonitorTypes().contains("type2"));
        assertEquals(2, retrievedContext.getEmailSendTo().size());
        assertTrue(retrievedContext.getEmailSendTo().contains("test1@example.com"));
        assertTrue(retrievedContext.getEmailSendTo().contains("test2@example.com"));
        assertTrue(retrievedContext.isHtml());
    }

    private EmailNotificationContextRecord createTestRecord(String contextName, String modifiedBy) {
        EmailNotificationContextRecord record = new EmailNotificationContextRecordImpl();
        EmailNotificationContext emailContext = createTestEmailContext(contextName);
        record.setEmailNotificationContext(emailContext);
        record.setModifiedBy(modifiedBy);
        return record;
    }

    private EmailNotificationContext createTestEmailContext(String contextName) {
        EmailNotificationContextImpl emailContext = new EmailNotificationContextImpl();
        emailContext.setContextName(contextName);

        List<String> monitorTypes = new ArrayList<>();
        monitorTypes.add("type1");
        emailContext.setMonitorTypes(monitorTypes);

        List<String> emailSendTo = new ArrayList<>();
        emailSendTo.add("test@example.com");
        emailContext.setEmailSendTo(emailSendTo);

        emailContext.setHtml(false);

        return emailContext;
    }

    private EmailNotificationContext createComplexEmailContext(String contextName) {
        EmailNotificationContextImpl emailContext = new EmailNotificationContextImpl();
        emailContext.setContextName(contextName);

        List<String> monitorTypes = new ArrayList<>();
        monitorTypes.add("type1");
        monitorTypes.add("type2");
        emailContext.setMonitorTypes(monitorTypes);

        List<String> emailSendTo = new ArrayList<>();
        emailSendTo.add("test1@example.com");
        emailSendTo.add("test2@example.com");
        emailContext.setEmailSendTo(emailSendTo);

        List<String> emailSendCc = new ArrayList<>();
        emailSendCc.add("cc@example.com");
        emailContext.setEmailSendCc(emailSendCc);

        List<String> emailSendBcc = new ArrayList<>();
        emailSendBcc.add("bcc@example.com");
        emailContext.setEmailSendBcc(emailSendBcc);

        Map<String, List<String>> emailSendToByMonitorType = new HashMap<>();
        List<String> type1Emails = new ArrayList<>();
        type1Emails.add("type1@example.com");
        emailSendToByMonitorType.put("type1", type1Emails);
        emailContext.setEmailSendToByMonitorType(emailSendToByMonitorType);

        Map<String, String> emailSubjectTemplate = new HashMap<>();
        emailSubjectTemplate.put("type1", "Subject for type1");
        emailContext.setEmailSubjectNotificationTemplate(emailSubjectTemplate);

        Map<String, String> emailBodyTemplate = new HashMap<>();
        emailBodyTemplate.put("type1", "Body for type1");
        emailContext.setEmailBodyNotificationTemplate(emailBodyTemplate);

        emailContext.setAttachment("attachment.txt");
        emailContext.setHtml(true);

        return emailContext;
    }
}
