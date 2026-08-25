package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

/**
 * MongoDB DAO Test for ScheduledContextInstanceAudit operations.
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoScheduledContextInstanceAuditDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoScheduledContextInstanceRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoScheduledContextInstanceAuditDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_findById() {
        String auditId = UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals("context1", result.getContextName());
        Assert.assertEquals("instanceId1", result.getContextInstanceId());
    }

    @Test
    public void test_findById_returns_null_when_not_found() {
        ScheduledContextInstanceRecord result = dao.findById("nonexistent-id");

        Assert.assertNull(result);
    }

    @Test
    public void test_save_audit_record_with_complete_status() {
        String auditId = UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals("context1", result.getContextName());
    }

    @Test
    public void test_save_audit_record_with_error_status() {
        String auditId = UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.ERROR);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals("instanceId1", result.getContextInstanceId());
    }

    @Test
    public void test_save_audit_record_with_running_status() {
        String auditId = UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.RUNNING);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getContextInstance());
    }

    @Test
    public void test_save_multiple_audit_records() {
        String auditId1 = UUID.randomUUID().toString();
        String auditId2 = UUID.randomUUID().toString();
        String auditId3 = UUID.randomUUID().toString();

        dao.save(createAuditRecord(auditId1, "instanceId1", "context1", InstanceStatus.COMPLETE));
        dao.save(createAuditRecord(auditId2, "instanceId2", "context2", InstanceStatus.ERROR));
        dao.save(createAuditRecord(auditId3, "instanceId3", "context3", InstanceStatus.RUNNING));

        ScheduledContextInstanceRecord result1 = dao.findById(auditId1);
        ScheduledContextInstanceRecord result2 = dao.findById(auditId2);
        ScheduledContextInstanceRecord result3 = dao.findById(auditId3);

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertNotNull(result3);
        Assert.assertEquals("context1", result1.getContextName());
        Assert.assertEquals("context2", result2.getContextName());
        Assert.assertEquals("context3", result3.getContextName());
    }

    @Test
    public void test_save_audit_record_with_same_context_instance_different_audit_ids() {
        String auditId1 = UUID.randomUUID().toString();
        String auditId2 = UUID.randomUUID().toString();

        dao.save(createAuditRecord(auditId1, "instanceId1", "context1", InstanceStatus.RUNNING));
        dao.save(createAuditRecord(auditId2, "instanceId1", "context1", InstanceStatus.COMPLETE));

        ScheduledContextInstanceRecord result1 = dao.findById(auditId1);
        ScheduledContextInstanceRecord result2 = dao.findById(auditId2);

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertEquals("instanceId1", result1.getContextInstanceId());
        Assert.assertEquals("instanceId1", result2.getContextInstanceId());
    }

    @Test
    public void test_save_audit_record_preserves_context_instance_data() {
        String auditId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 10000;

        MongoScheduledContextInstanceRecordImpl record = (MongoScheduledContextInstanceRecordImpl)
                createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);

        // Get the context instance, modify it, and set it back to trigger field updates
        ContextInstanceImpl contextInstance = (ContextInstanceImpl) record.getContextInstance();
        contextInstance.setStartTime(startTime);
        contextInstance.setEndTime(endTime);
        record.setContextInstance(contextInstance);

        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getContextInstance());
        Assert.assertEquals("instanceId1", result.getContextInstance().getId());
        Assert.assertEquals(startTime, result.getContextInstance().getStartTime());
        Assert.assertEquals(endTime, result.getContextInstance().getEndTime());
    }

    @Test
    public void test_save_audit_record_with_all_status_types() {
        InstanceStatus[] statuses = {
            InstanceStatus.WAITING,
            InstanceStatus.RUNNING,
            InstanceStatus.COMPLETE,
            InstanceStatus.ERROR,
            InstanceStatus.ON_HOLD,
            InstanceStatus.SKIPPED
        };

        for (InstanceStatus status : statuses) {
            String auditId = UUID.randomUUID().toString();
            ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId_" + status, "context_" + status, status);
            dao.save(record);

            ScheduledContextInstanceRecord result = dao.findById(auditId);
            Assert.assertNotNull("Record with status " + status + " should not be null", result);
            Assert.assertEquals("contextId should match for " + status, "instanceId_" + status, result.getContextInstanceId());
        }
    }

    @Test
    public void test_save_audit_record_with_special_characters_in_context_name() {
        String auditId = UUID.randomUUID().toString();
        String contextName = "context-with-special_chars.123";
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", contextName, InstanceStatus.COMPLETE);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals(contextName, result.getContextName());
    }

    @Test
    public void test_save_audit_record_with_long_context_instance_id() {
        String auditId = UUID.randomUUID().toString();
        String longInstanceId = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, longInstanceId, "context1", InstanceStatus.COMPLETE);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals(longInstanceId, result.getContextInstanceId());
    }

    @Test
    public void test_save_audit_record_updates_existing_record() {
        String auditId = UUID.randomUUID().toString();

        // Save initial record
        ScheduledContextInstanceRecord record1 = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.RUNNING);
        dao.save(record1);

        // Update with new status (in audit context, this would typically be a new audit ID,
        // but testing that same ID can be overwritten)
        ScheduledContextInstanceRecord record2 = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        dao.save(record2);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals("instanceId1", result.getContextInstanceId());
    }

    @Test
    public void test_save_audit_record_with_null_context_instance_id() {
        String auditId = UUID.randomUUID().toString();
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, null, "context1", InstanceStatus.COMPLETE);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals("context1", result.getContextName());
    }

    @Test
    public void test_save_preserves_timestamp() {
        String auditId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        record.setTimestamp(timestamp);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals(timestamp, result.getTimestamp());
    }

    @Test
    public void test_save_preserves_modified_timestamp() {
        String auditId = UUID.randomUUID().toString();
        long modifiedTimestamp = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        record.setModifiedTimestamp(modifiedTimestamp);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals(modifiedTimestamp, result.getModifiedTimestamp());
    }

    @Test
    public void test_save_preserves_modified_by() {
        String auditId = UUID.randomUUID().toString();
        String modifiedBy = "testUser";

        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        record.setModifiedBy(modifiedBy);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertEquals(modifiedBy, result.getModifiedBy());
    }

    @Test
    public void test_save_preserves_contains_repeating_jobs() {
        String auditId = UUID.randomUUID().toString();

        MongoScheduledContextInstanceRecordImpl record = (MongoScheduledContextInstanceRecordImpl)
                createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);

        // Get the context instance, modify it, and set it back to trigger field updates
        ContextInstanceImpl contextInstance = (ContextInstanceImpl) record.getContextInstance();
        contextInstance.setContainsRepeatingJobs(true);
        record.setContextInstance(contextInstance); // Trigger the setter to copy fields
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isContainsRepeatingJobs());
        Assert.assertTrue(result.getContextInstance().isContainsRepeatingJobs());
    }

    @Test
    public void test_audit_records_are_independent() {
        String auditId1 = UUID.randomUUID().toString();
        String auditId2 = UUID.randomUUID().toString();

        // Save first audit record
        ScheduledContextInstanceRecord record1 = createAuditRecord(auditId1, "instanceId1", "context1", InstanceStatus.RUNNING);
        dao.save(record1);

        // Save second audit record with different data but same context instance ID
        ScheduledContextInstanceRecord record2 = createAuditRecord(auditId2, "instanceId1", "context1", InstanceStatus.COMPLETE);
        dao.save(record2);

        // Both records should exist independently
        ScheduledContextInstanceRecord result1 = dao.findById(auditId1);
        ScheduledContextInstanceRecord result2 = dao.findById(auditId2);

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertNotEquals(result1.getId(), result2.getId());
    }

    // Helper method
    private ScheduledContextInstanceRecord createAuditRecord(String auditId, String contextInstanceId,
                                                             String contextName, InstanceStatus status) {
        MongoScheduledContextInstanceRecordImpl record = new MongoScheduledContextInstanceRecordImpl();
        record.setId(auditId);
        record.setContextInstanceId(contextInstanceId != null ? contextInstanceId : auditId);
        record.setContextName(contextName);
        record.setStatus(status.name());
        record.setTimestamp(System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(contextInstanceId);
        contextInstance.setName(contextName);
        contextInstance.setStartTime(System.currentTimeMillis());
        contextInstance.setEndTime(System.currentTimeMillis() + 5000);

        record.setContextInstance(contextInstance);

        return record;
    }
}
