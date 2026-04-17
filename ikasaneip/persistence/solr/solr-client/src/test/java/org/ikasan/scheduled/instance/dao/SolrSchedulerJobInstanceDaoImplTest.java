package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SolrSchedulerJobInstanceDaoImplTest extends SolrTestCaseJ4 {

    private SolrSchedulerJobInstanceDaoImpl dao;
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

        this.dao = new SolrSchedulerJobInstanceDaoImpl();
        this.dao.setSolrClient(this.server);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_save_and_findById_internal_event_driven_job() {
        SchedulerJobInstanceRecord record = createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "childContext1");
        dao.save(record);

        String id = "job1_instanceId1_childContext1_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job1", result.getJobName());
        Assert.assertEquals("context1", result.getContextName());
    }

    @Test
    public void test_save_and_findById_file_event_driven_job() {
        SchedulerJobInstanceRecord record = createFileEventDrivenJobRecord("job2", "context2", "instanceId2", "childContext2");
        dao.save(record);

        String id = "job2_instanceId2_childContext2_" + JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job2", result.getJobName());
        Assert.assertEquals("context2", result.getContextName());
    }

    @Test
    public void test_save_and_findById_quartz_schedule_driven_job() {
        SchedulerJobInstanceRecord record = createQuartzScheduleDrivenJobRecord("job3", "context3", "instanceId3", "childContext3");
        dao.save(record);

        String id = "job3_instanceId3_childContext3_" + JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job3", result.getJobName());
    }

    @Test
    public void test_save_and_findById_global_event_job() {
        SchedulerJobInstanceRecord record = createGlobalEventJobRecord("job4", "context4", "instanceId4", "childContext4");
        dao.save(record);

        String id = "job4_instanceId4_childContext4_" + JobConstants.GLOBAL_EVENT_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job4", result.getJobName());
    }

    @Test
    public void test_save_and_findById_context_start_job() {
        SchedulerJobInstanceRecord record = createContextStartJobRecord("job5", "context5", "instanceId5", "childContext5");
        dao.save(record);

        String id = "job5_instanceId5_childContext5_" + JobConstants.CONTEXT_START_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job5", result.getJobName());
    }

    @Test
    public void test_save_and_findById_context_terminal_job() {
        SchedulerJobInstanceRecord record = createContextTerminalJobRecord("job6", "context6", "instanceId6", "childContext6");
        dao.save(record);

        String id = "job6_instanceId6_childContext6_" + JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job6", result.getJobName());
    }

    @Test
    public void test_save_and_findById_local_event_job() {
        SchedulerJobInstanceRecord record = createLocalEventJobRecord("job7", "context7", "instanceId7", "childContext7");
        dao.save(record);

        String id = "job7_instanceId7_childContext7_" + JobConstants.LOCAL_EVENT_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job7", result.getJobName());
    }

    @Test
    public void test_save_and_findById_bridging_job() {
        SchedulerJobInstanceRecord record = createBridgingJobRecord("job8", "context8", "instanceId8", "childContext8");
        dao.save(record);

        String id = "job8_instanceId8_childContext8_" + JobConstants.BRIDGING_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("job8", result.getJobName());
    }

    @Test
    public void test_findById_returns_null_when_not_found() {
        SchedulerJobInstanceRecord result = dao.findById("nonexistent_id_123");
        Assert.assertNull(result);
    }

    @Test
    public void test_getSchedulerJobInstancesByContextInstanceId() {
        dao.save(createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1"));
        dao.save(createInternalEventDrivenJobRecord("job2", "context1", "instanceId1", "child2"));
        dao.save(createInternalEventDrivenJobRecord("job3", "context2", "instanceId2", "child1"));

        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextInstanceId("instanceId1", 10, 0, null, null);

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getSchedulerJobInstancesByContextName() {
        dao.save(createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1"));
        dao.save(createInternalEventDrivenJobRecord("job2", "context1", "instanceId2", "child2"));
        dao.save(createInternalEventDrivenJobRecord("job3", "context2", "instanceId3", "child1"));

        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextName("context1", 10, 0, null, null);

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_status() {
        dao.save(createInternalEventDrivenJobRecordWithStatus("job1", "context1", "instanceId1", "child1", InstanceStatus.COMPLETE));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job2", "context1", "instanceId2", "child2", InstanceStatus.ERROR));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job3", "context1", "instanceId3", "child3", InstanceStatus.COMPLETE));

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.COMPLETE.name());

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_skipped_status() {
        dao.save(createInternalEventDrivenJobRecordWithStatus("job1", "context1", "instanceId1", "child1", InstanceStatus.SKIPPED));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job2", "context1", "instanceId2", "child2", InstanceStatus.SKIPPED_COMPLETE));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job3", "context1", "instanceId3", "child3", InstanceStatus.COMPLETE));

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.SKIPPED.name());

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_targetResidingContextOnly() {
        dao.save(createInternalEventDrivenJobRecordWithTargetResiding("job1", "context1", "instanceId1", "child1", true));
        dao.save(createInternalEventDrivenJobRecordWithTargetResiding("job2", "context1", "instanceId2", "child2", false));

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setTargetResidingContextOnly(true);

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_participatesInLock() {
        dao.save(createInternalEventDrivenJobRecordWithLockParticipation("job1", "context1", "instanceId1", "child1", true));
        dao.save(createInternalEventDrivenJobRecordWithLockParticipation("job2", "context1", "instanceId2", "child2", false));

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setParticipatesInLock(true);

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_doesJobPlanInstanceContainRepeatingJobs_returns_true() {
        dao.save(createRepeatableInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1", true));

        boolean result = dao.doesJobPlanInstanceContainRepeatingJobs("instanceId1");

        Assert.assertTrue(result);
    }

    @Test
    public void test_doesJobPlanInstanceContainRepeatingJobs_returns_false() {
        dao.save(createRepeatableInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1", false));

        boolean result = dao.doesJobPlanInstanceContainRepeatingJobs("instanceId1");

        Assert.assertFalse(result);
    }

    @Test
    public void test_getJobStatusCountForContextInstances() {
        dao.save(createInternalEventDrivenJobRecordWithStatus("job1", "context1", "instanceId1", "child1", InstanceStatus.COMPLETE));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job2", "context1", "instanceId1", "child2", InstanceStatus.COMPLETE));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job3", "context1", "instanceId1", "child3", InstanceStatus.ERROR));

        List<ContextInstanceAggregateJobStatus> results = dao.getJobStatusCountForContextInstances(List.of("instanceId1"));

        Assert.assertEquals(1, results.size());
        Assert.assertEquals("instanceId1", results.get(0).getContextInstanceId());
        Assert.assertEquals(2, results.get(0).getStatusCount(InstanceStatus.COMPLETE));
        Assert.assertEquals(1, results.get(0).getStatusCount(InstanceStatus.ERROR));
    }

    @Test
    public void test_getJobStatusCountForContextInstancesConsiderNonTargetedDuplication() {
        dao.save(createInternalEventDrivenJobRecordWithTargetResiding("job1", "context1", "instanceId1", "child1", false));
        dao.save(createInternalEventDrivenJobRecordWithTargetResiding("job1", "context1", "instanceId1", "child2", true));
        dao.save(createInternalEventDrivenJobRecordWithStatus("job2", "context1", "instanceId1", "child3", InstanceStatus.COMPLETE));

        List<ContextInstanceAggregateJobStatus> results = dao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List.of("instanceId1"));

        Assert.assertEquals(1, results.size());
        Assert.assertNotNull(results.get(0));
    }

    @Test
    public void test_deleteSchedulerJobInstances() {
        dao.save(createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1"));
        dao.save(createFileEventDrivenJobRecord("job2", "context1", "instanceId1", "child2"));
        dao.save(createInternalEventDrivenJobRecord("job3", "context2", "instanceId2", "child1"));

        dao.deleteSchedulerJobInstances("instanceId1");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getSchedulerJobInstancesByContextInstanceId("instanceId1", 10, 0, null, null);
        Assert.assertEquals(0, results.getResultList().size());

        SearchResults<SchedulerJobInstanceRecord> results2 = dao.getSchedulerJobInstancesByContextInstanceId("instanceId2", 10, 0, null, null);
        Assert.assertEquals(1, results2.getResultList().size());
    }

    @Test
    public void test_save_with_error_acknowledged() {
        SchedulerJobInstanceRecord record = createInternalEventDrivenJobRecordWithStatus("job1", "context1", "instanceId1", "child1", InstanceStatus.ERROR);
        record.setStatus(InstanceStatus.ERROR_ACKNOWLEDGED.name());
        InternalEventDrivenJobInstance job = (InternalEventDrivenJobInstance) record.getSchedulerJobInstance();
        job.setErrorAcknowledged(true);
        dao.save(record);

        String id = "job1_instanceId1_child1_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals(InstanceStatus.ERROR_ACKNOWLEDGED.name(), result.getStatus());
    }

    @Test
    public void test_save_with_modifiedBy_field() {
        SchedulerJobInstanceRecord record = createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1");
        record.setModifiedBy("testUser");
        dao.save(record);

        String id = "job1_instanceId1_child1_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("testUser", result.getModifiedBy());
    }

    @Test
    public void test_save_with_manuallySubmittedBy_field() {
        SchedulerJobInstanceRecord record = createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1");
        record.setManuallySubmittedBy("manualUser");
        dao.save(record);

        String id = "job1_instanceId1_child1_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE;
        SchedulerJobInstanceRecord result = dao.findById(id);

        Assert.assertNotNull(result);
        Assert.assertEquals("manualUser", result.getManuallySubmittedBy());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_sorting_ascending() throws Exception {
        dao.save(createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1"));
        Thread.sleep(100);
        dao.save(createInternalEventDrivenJobRecord("job2", "context1", "instanceId2", "child2"));

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName("context1");

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 10, 0, "timestamp", "ASCENDING");

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_time_windows() {
        long now = System.currentTimeMillis();

        SchedulerJobInstanceRecord record = createInternalEventDrivenJobRecord("job1", "context1", "instanceId1", "child1");
        ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
        event.setFireTime(now);
        event.setCompletionTime(now + 1000);
        SchedulerJobInstance instance = record.getSchedulerJobInstance();
        instance.setScheduledProcessEvent(event);
        record.setSchedulerJobInstance(instance);
        dao.save(record);

        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setStartTimeWindowStart(now - 1000);
        filter.setStartTimeWindowEnd(now + 1000);
        filter.setEndTimeWindowStart(now);
        filter.setEndTimeWindowEnd(now + 2000);

        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter
            (filter, 10, 0, null, null);

        Assert.assertEquals(1, results.getResultList().size());
    }

    // Helper methods to create test records
    private SchedulerJobInstanceRecord createInternalEventDrivenJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        InternalEventDrivenJobInstanceImpl job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createInternalEventDrivenJobRecordWithStatus(String jobName, String contextName, String contextInstanceId, String childContextName, InstanceStatus status) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(status.name());
        record.setTimestamp(System.currentTimeMillis());

        InternalEventDrivenJobInstanceImpl job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(status);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createInternalEventDrivenJobRecordWithTargetResiding(String jobName, String contextName, String contextInstanceId, String childContextName, boolean targetResiding) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        InternalEventDrivenJobInstanceImpl job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        job.setTargetResidingContextOnly(targetResiding);
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createInternalEventDrivenJobRecordWithLockParticipation(String jobName, String contextName, String contextInstanceId, String childContextName, boolean participatesInLock) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        InternalEventDrivenJobInstanceImpl job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        job.setParticipatesInLock(participatesInLock);
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createRepeatableInternalEventDrivenJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName, boolean repeatable) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        InternalEventDrivenJobInstanceImpl job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        job.setJobRepeatable(repeatable);
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createFileEventDrivenJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        SolrFileEventDrivenJobInstanceImpl job = new SolrFileEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createQuartzScheduleDrivenJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        QuartzScheduleDrivenJobInstanceImpl job = new QuartzScheduleDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createGlobalEventJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        GlobalEventJobInstanceImpl job = new GlobalEventJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createContextStartJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        ContextStartJobInstanceImpl job = new ContextStartJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createContextTerminalJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        ContextTerminalJobInstanceImpl job = new ContextTerminalJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createLocalEventJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        LocalEventJobInstanceImpl job = new LocalEventJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }

    private SchedulerJobInstanceRecord createBridgingJobRecord(String jobName, String contextName, String contextInstanceId, String childContextName) {
        SolrSchedulerJobInstanceRecordImpl record = new SolrSchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(InstanceStatus.WAITING.name());
        record.setTimestamp(System.currentTimeMillis());

        BridgingJobInstanceImpl job = new BridgingJobInstanceImpl();
        job.setJobName(jobName);
        job.setStatus(InstanceStatus.WAITING);
        job.setDisplayName(jobName + " Display");
        record.setSchedulerJobInstance(job);

        return record;
    }
}
