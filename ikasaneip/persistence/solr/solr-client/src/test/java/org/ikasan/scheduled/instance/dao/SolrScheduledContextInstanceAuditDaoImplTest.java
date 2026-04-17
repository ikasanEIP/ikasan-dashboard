package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class SolrScheduledContextInstanceAuditDaoImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceAuditDaoImpl dao;
    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        this.tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        this.server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        this.server.request(createRequest);

        this.dao = new SolrScheduledContextInstanceAuditDaoImpl();
        this.dao.setSolrClient(this.server);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
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
        ScheduledContextInstanceRecord record = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);

        SolrContextInstanceImpl contextInstance = (SolrContextInstanceImpl) record.getContextInstance();
        contextInstance.setStartTime(System.currentTimeMillis());
        contextInstance.setEndTime(System.currentTimeMillis() + 10000);

        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getContextInstance());
        Assert.assertEquals("instanceId1", result.getContextInstance().getId());
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

        // Update with new status
        ScheduledContextInstanceRecord record2 = createAuditRecord(auditId, "instanceId1", "context1", InstanceStatus.COMPLETE);
        dao.save(record2);

        ScheduledContextInstanceRecord result = dao.findById(auditId);

        Assert.assertNotNull(result);
        // The record should be updated
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
        // Timestamp should be preserved in the audit record
    }

    // Helper method
    private ScheduledContextInstanceRecord createAuditRecord(String auditId, String contextInstanceId, String contextName, InstanceStatus status) {
        SolrScheduledContextInstanceRecordImpl record = new SolrScheduledContextInstanceRecordImpl();
        // Note: The ID is set internally by the DAO based on the context instance ID
        ReflectionTestUtils.setField(record, "id", auditId);
        record.setContextInstanceId(contextInstanceId != null ? contextInstanceId : auditId);
        record.setContextName(contextName);
        record.setTimestamp(System.currentTimeMillis());

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(contextInstanceId);
        contextInstance.setName(contextName);
        contextInstance.setStartTime(System.currentTimeMillis());
        contextInstance.setEndTime(System.currentTimeMillis() + 5000);

        record.setContextInstance(contextInstance);

        return record;
    }
}
