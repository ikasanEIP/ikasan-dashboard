package org.ikasan.mongo.persistence.scheduled.notification.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoNotificationSendAuditImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoNotificationSendAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoNotificationSendAuditRepository;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MongoNotificationSendAuditDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoNotificationSendAuditDao dao;
    private MongoNotificationSendAuditRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoNotificationSendAuditRepository.class);

        dao = new MongoNotificationSendAuditDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoNotificationSendAuditRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_find() {
        NotificationSendAuditRecord record = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);

        dao.save(record);

        NotificationSendAuditRecord found = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found);
        assertTrue(found.getNotificationSendAudit().isNotificationSend());
        assertEquals("instance1", found.getNotificationSendAudit().getContextInstanceId());
        assertEquals("context1", found.getNotificationSendAudit().getContextName());
        assertEquals("job1", found.getNotificationSendAudit().getJobName());
        assertEquals("RUNNING", found.getNotificationSendAudit().getMonitorType());
        assertEquals("EMAIL", found.getNotificationSendAudit().getNotifierType());
    }

    @Test
    public void test_find_not_found() {
        NotificationSendAuditRecord record = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);
        dao.save(record);

        NotificationSendAuditRecord found = dao.find("nonexistent", "context1", "job1", "RUNNING", "EMAIL");
        assertNull(found);
    }

    @Test
    public void test_save_generates_id() {
        NotificationSendAuditRecord record = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);

        dao.save(record);

        NotificationSendAuditRecord found = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals("instance1_context1_job1_RUNNING_EMAIL", found.getId());
    }

    @Test
    public void test_save_sets_timestamps() {
        NotificationSendAuditRecord record = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);

        dao.save(record);

        NotificationSendAuditRecord found = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found);
        assertTrue(found.getTimestamp() > 0);
        assertTrue(found.getModifiedTimestamp() > 0);
    }

    @Test
    public void test_save_updates_existing_record() {
        NotificationSendAuditRecord record = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);
        dao.save(record);

        NotificationSendAuditRecord found1 = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found1);
        long timestamp1 = found1.getModifiedTimestamp();
        assertTrue(found1.getNotificationSendAudit().isNotificationSend());

        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NotificationSendAuditRecord updatedRecord = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", false);
        updatedRecord.setModifiedBy("updatedUser");
        dao.save(updatedRecord);

        NotificationSendAuditRecord found2 = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found2);
        assertEquals("updatedUser", found2.getModifiedBy());
        assertFalse(found2.getNotificationSendAudit().isNotificationSend());
        assertTrue(found2.getModifiedTimestamp() > timestamp1);
    }

    @Test
    public void test_save_with_mongo_record() {
        MongoNotificationSendAuditRecordImpl mongoRecord = new MongoNotificationSendAuditRecordImpl();
        NotificationSendAudit audit = createTestAudit("instance1", "context1", "job1", "RUNNING", "EMAIL", true);
        mongoRecord.setNotificationSendAudit(audit);
        mongoRecord.setModifiedBy("testUser");

        dao.save(mongoRecord);

        NotificationSendAuditRecord found = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        assertNotNull(found);
        assertTrue(found.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_save_multiple_records() {
        NotificationSendAuditRecord record1 = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);
        NotificationSendAuditRecord record2 = createTestRecord("instance2", "context2", "job2", "STOPPED", "SMS", false);
        NotificationSendAuditRecord record3 = createTestRecord("instance3", "context1", "job3", "FAILED", "EMAIL", true);

        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        NotificationSendAuditRecord found1 = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        NotificationSendAuditRecord found2 = dao.find("instance2", "context2", "job2", "STOPPED", "SMS");
        NotificationSendAuditRecord found3 = dao.find("instance3", "context1", "job3", "FAILED", "EMAIL");

        assertNotNull(found1);
        assertNotNull(found2);
        assertNotNull(found3);
        assertTrue(found1.getNotificationSendAudit().isNotificationSend());
        assertFalse(found2.getNotificationSendAudit().isNotificationSend());
        assertTrue(found3.getNotificationSendAudit().isNotificationSend());
    }

    @Test
    public void test_insert() {
        List<NotificationSendAuditRecord> records = new ArrayList<>();
        records.add(createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true));
        records.add(createTestRecord("instance2", "context2", "job2", "STOPPED", "SMS", false));

        dao.insert(records);

        NotificationSendAuditRecord found1 = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        NotificationSendAuditRecord found2 = dao.find("instance2", "context2", "job2", "STOPPED", "SMS");

        assertNotNull(found1);
        assertNotNull(found2);
    }

    @Test
    public void test_notification_send_flag() {
        NotificationSendAuditRecord sentRecord = createTestRecord("instance1", "context1", "job1", "RUNNING", "EMAIL", true);
        NotificationSendAuditRecord notSentRecord = createTestRecord("instance2", "context2", "job2", "STOPPED", "SMS", false);

        dao.save(sentRecord);
        dao.save(notSentRecord);

        NotificationSendAuditRecord found1 = dao.find("instance1", "context1", "job1", "RUNNING", "EMAIL");
        NotificationSendAuditRecord found2 = dao.find("instance2", "context2", "job2", "STOPPED", "SMS");

        assertTrue(found1.getNotificationSendAudit().isNotificationSend());
        assertFalse(found2.getNotificationSendAudit().isNotificationSend());
    }

    private NotificationSendAuditRecord createTestRecord(String contextInstanceId, String contextName,
                                                        String jobName, String monitorType,
                                                        String notifierType, boolean isNotificationSend) {
        MongoNotificationSendAuditRecordImpl record = new MongoNotificationSendAuditRecordImpl();
        NotificationSendAudit audit = createTestAudit(contextInstanceId, contextName, jobName,
            monitorType, notifierType, isNotificationSend);
        record.setNotificationSendAudit(audit);
        record.setModifiedBy("testUser");
        return record;
    }

    private NotificationSendAudit createTestAudit(String contextInstanceId, String contextName,
                                                 String jobName, String monitorType,
                                                 String notifierType, boolean isNotificationSend) {
        return new MongoNotificationSendAuditImpl(jobName, contextInstanceId, contextName,
            monitorType, notifierType, isNotificationSend);
    }
}
