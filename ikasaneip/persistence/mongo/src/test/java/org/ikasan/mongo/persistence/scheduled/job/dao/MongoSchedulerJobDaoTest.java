package org.ikasan.mongo.persistence.scheduled.job.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetadata;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoSchedulerJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoSchedulerJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.ikasan.mongo.persistence.general.model.MongoConstants.IKASAN_COLLECTION_NAME;
import static org.ikasan.spec.metadata.dao.ModuleMetadataDao.MODULE_METADATA;

public class MongoSchedulerJobDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoSchedulerJobRepository repository;
    private MongoSchedulerJobDao dao;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        // Use Spring Data MongoDB repository factory
        var mongoMappingContext = mongoTemplate.getConverter().getMappingContext();
        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoSchedulerJobRepository.class);

        dao = new MongoSchedulerJobDao(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoSchedulerJobRecordImpl.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_find_all() {
        // Insert test data
        insertFileEventDrivenJobs("id", 50, "context1");
        insertQuartzScheduleDrivenJobs("idq", 30, "context2");
        insertInternalEventDrivenJobs("idi", 20, "context3");

        SearchResults<SchedulerJobRecord> results = dao.findAll(10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(100, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_context() {
        insertFileEventDrivenJobs("id", 100, "contextId");
        insertFileEventDrivenJobs("idd", 200, "context2Id");

        SearchResults<SchedulerJobRecord> results = dao.findByContext("contextId", 10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(100, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_agent() {
        insertFileEventDrivenJobsWithAgent("id", 50, "context1", "agent1");
        insertFileEventDrivenJobsWithAgent("id2", 30, "context2", "agent2");

        SearchResults<SchedulerJobRecord> results = dao.findByAgent("agent1", 10, 0);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getResultList().size());
        Assert.assertEquals(50, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_id() {
        MongoSchedulerJobRecordImpl record = createFileEventDrivenJobRecord("test-id-123", "testJob", "testContext", "testAgent");
        repository.save(record);

        SchedulerJobRecord result = dao.findById("test-id-123");

        Assert.assertNotNull(result);
        Assert.assertEquals("test-id-123", result.getId());
        Assert.assertEquals("testJob", result.getJobName());
        Assert.assertEquals("testContext", result.getContextName());
        Assert.assertTrue(result.getJob() instanceof FileEventDrivenJobImpl);
    }

    @Test
    public void test_find_by_context_and_job_name() {
        insertFileEventDrivenJobs("id", 10, "contextId");

        SchedulerJobRecord result = dao.findByContextIdAndJobName("contextId", "jobName5");

        Assert.assertNotNull(result);
        Assert.assertEquals("jobName5", result.getJobName());
        Assert.assertEquals("contextId", result.getContextName());
    }

    @Test
    public void test_delete() {
        MongoSchedulerJobRecordImpl record = createFileEventDrivenJobRecord("delete-test", "testJob", "testContext", "testAgent");
        repository.save(record);

        SchedulerJobRecord found = dao.findById("delete-test");
        Assert.assertNotNull(found);

        dao.delete(found);

        SchedulerJobRecord deleted = dao.findById("delete-test");
        Assert.assertNull(deleted);
    }

    @Test
    public void test_delete_by_agent_name() {
        insertFileEventDrivenJobsWithAgent("id1", 10, "context1", "agent1");
        insertFileEventDrivenJobsWithAgent("id2", 10, "context2", "agent2");

        Assert.assertEquals(20, dao.findAll(100, 0).getTotalNumberOfResults());

        dao.deleteByAgentName("agent1");

        Assert.assertEquals(10, dao.findAll(100, 0).getTotalNumberOfResults());
    }

    @Test
    public void test_delete_by_context_name() {
        insertFileEventDrivenJobs("id1", 10, "context1");
        insertFileEventDrivenJobs("id2", 10, "context2");

        Assert.assertEquals(20, dao.findAll(100, 0).getTotalNumberOfResults());

        dao.deleteByContextName("context1");

        Assert.assertEquals(10, dao.findAll(100, 0).getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_filter_job_type() {
        insertFileEventDrivenJobs("id", 10, "context1");
        insertQuartzScheduleDrivenJobs("idq", 10, "context2");
        insertInternalEventDrivenJobs("idi", 10, "context3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setJobTypeFilter(JobConstants.FILE_EVENT_DRIVEN_JOB);

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        Assert.assertEquals(10, results.getTotalNumberOfResults());
        Assert.assertTrue(results.getResultList().get(0).getJob() instanceof FileEventDrivenJobImpl);
    }

    @Test
    public void test_find_by_filter_job_name() {
        insertFileEventDrivenJobs("id", 100, "context1");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setJobNameFilter("jobName5");

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Should match jobName5, jobName50, jobName51, ..., jobName59
        Assert.assertTrue(results.getTotalNumberOfResults() >= 10);
        results.getResultList().forEach(record ->
            Assert.assertTrue(record.getJobName().contains("jobName5"))
        );
    }

    @Test
    public void test_find_by_filter_display_name() {
        insertFileEventDrivenJobs("id", 100, "context1");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setDisplayNameFilter("displayName10");

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals("displayName10", results.getResultList().get(0).getDisplayName());
    }

    @Test
    public void test_find_by_filter_context_names() {
        insertFileEventDrivenJobs("id1", 10, "context1");
        insertFileEventDrivenJobs("id2", 10, "context2");
        insertFileEventDrivenJobs("id3", 10, "context3");

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setContextNames(List.of("context1", "context2"));

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        Assert.assertEquals(20, results.getTotalNumberOfResults());
    }

    @Test
    public void test_find_by_filter_held() {
        insertFileEventDrivenJobsWithFlags("id", 50, "context1", true, false);

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setHeld(true);

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        Assert.assertEquals(50, results.getTotalNumberOfResults());
        results.getResultList().forEach(record -> Assert.assertTrue(record.isHeld()));
    }

    @Test
    public void test_find_by_filter_skipped() {
        insertFileEventDrivenJobsWithFlags("id", 50, "context1", false, true);

        TestSchedulerJobSearchFilter filter = new TestSchedulerJobSearchFilter();
        filter.setSkipped(true);

        SearchResults<SchedulerJobRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        Assert.assertEquals(50, results.getTotalNumberOfResults());
        results.getResultList().forEach(record -> Assert.assertTrue(record.isSkipped()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_save_throws_exception() {
        MongoSchedulerJobRecordImpl record = createFileEventDrivenJobRecord("test", "job", "context", "agent");
        dao.save(record);
    }

    @Test
    public void test_get_all_agent_names() {
        // Insert module metadata documents with type="moduleMetaData" and SCHEDULER_AGENT type in payload
        insertModuleMetadata("agent1");
        insertModuleMetadata("agent2");
        insertModuleMetadata("agent3");

        List<String> agentNames = dao.getAllAgentNames();

        Assert.assertNotNull(agentNames);
        Assert.assertEquals(3, agentNames.size());
    }

    // Helper classes and methods

    private static class TestSchedulerJobSearchFilter implements SchedulerJobSearchFilter {
        private String jobTypeFilter;
        private List<String> jobTypes;
        private String jobNameFilter;
        private String displayNameFilter;
        private List<String> notJobNameInFilter;
        private List<String> contextNames;
        private String contextSearchFilter;
        private boolean held;
        private boolean skipped;
        private Boolean targetResidingContextOnly;
        private Boolean participatesInLock;

        @Override
        public String getJobTypeFilter() { return jobTypeFilter; }
        @Override
        public void setJobTypeFilter(String jobTypeFilter) { this.jobTypeFilter = jobTypeFilter; }
        @Override
        public List<String> getJobTypes() { return jobTypes; }
        @Override
        public void setJobTypes(List<String> jobTypes) { this.jobTypes = jobTypes; }
        @Override
        public String getJobNameFilter() { return jobNameFilter; }
        @Override
        public void setJobNameFilter(String jobNameFilter) { this.jobNameFilter = jobNameFilter; }
        @Override
        public String getDisplayNameFilter() { return displayNameFilter; }
        @Override
        public void setDisplayNameFilter(String displayNameFilter) { this.displayNameFilter = displayNameFilter; }
        @Override
        public List<String> getNotJobNameInFilter() { return notJobNameInFilter; }
        @Override
        public void setNotJobNameInFilter(List<String> notJobNameInFilter) { this.notJobNameInFilter = notJobNameInFilter; }
        @Override
        public List<String> getContextNames() { return contextNames; }
        @Override
        public void setContextNames(List<String> contextNames) { this.contextNames = contextNames; }
        @Override
        public String getContextSearchFilter() { return contextSearchFilter; }
        @Override
        public void setContextSearchFilter(String contextSearchFilter) { this.contextSearchFilter = contextSearchFilter; }
        @Override
        public boolean isHeld() { return held; }
        @Override
        public void setHeld(boolean held) { this.held = held; }
        @Override
        public boolean isSkipped() { return skipped; }
        @Override
        public void setSkipped(boolean skipped) { this.skipped = skipped; }
        @Override
        public Boolean isTargetResidingContextOnly() { return targetResidingContextOnly; }
        @Override
        public void setTargetResidingContextOnly(Boolean targetResidingContextOnly) { this.targetResidingContextOnly = targetResidingContextOnly; }
        @Override
        public Boolean isParticipatesInLock() { return participatesInLock; }
        @Override
        public void setParticipatesInLock(Boolean participatesInLock) { this.participatesInLock = participatesInLock; }
        @Override
        public void setStatus(String status) {
            if (status == null || status.isEmpty()) {
                this.held = false;
                this.skipped = false;
            } else if (status.equals(InstanceStatus.ON_HOLD.name())) {
                this.held = true;
                this.skipped = false;
            } else if (status.equals(InstanceStatus.SKIPPED.name())) {
                this.held = false;
                this.skipped = true;
            }
        }
    }

    private void insertFileEventDrivenJobs(String idPrefix, int num, String contextId) {
        insertFileEventDrivenJobsWithAgent(idPrefix, num, contextId, null);
    }

    private void insertFileEventDrivenJobsWithAgent(String idPrefix, int num, String contextId, String agentName) {
        insertFileEventDrivenJobsWithFlags(idPrefix, num, contextId, agentName, false, false);
    }

    private void insertFileEventDrivenJobsWithFlags(String idPrefix, int num, String contextId, boolean held, boolean skipped) {
        insertFileEventDrivenJobsWithFlags(idPrefix, num, contextId, null, held, skipped);
    }

    private void insertFileEventDrivenJobsWithFlags(String idPrefix, int num, String contextId, String agentName, boolean held, boolean skipped) {
        for (int i = 0; i < num; i++) {
            String agent = agentName != null ? agentName : idPrefix + "agentName" + i;
            String id = "fileEventDrivenJob_" + idPrefix + agent + "_jobName" + i + "_" + contextId;

            MongoSchedulerJobRecordImpl record = createFileEventDrivenJobRecord(id, "jobName" + i, contextId, agent);
            record.setDisplayName("displayName" + i);
            record.setHeld(held);
            record.setSkipped(skipped);

            repository.save(record);
        }
    }

    private MongoSchedulerJobRecordImpl createFileEventDrivenJobRecord(String id, String jobName, String contextName, String agentName) {
        FileEventDrivenJobImpl job = new FileEventDrivenJobImpl();
        job.setAgentName(agentName);
        job.setJobName(jobName);
        job.setContextName(contextName);
        job.setCronExpression("0 0 * * * ?");
        job.setFilePath("/test/path");

        MongoSchedulerJobRecordImpl record = new MongoSchedulerJobRecordImpl();
        record.setId(id);
        record.setAgentName(agentName);
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setDisplayName(jobName);
        record.setTimestamp(System.currentTimeMillis());
        record.setJob(job);

        return record;
    }

    private void insertQuartzScheduleDrivenJobs(String idPrefix, int num, String contextId) {
        for (int i = 0; i < num; i++) {
            String agent = idPrefix + "agentName" + i;
            String id = "quartzScheduleDrivenJob_" + idPrefix + agent + "_jobName" + i + "_" + contextId;

            QuartzScheduleDrivenJobImpl job = new QuartzScheduleDrivenJobImpl();
            job.setAgentName(agent);
            job.setJobName("jobName" + i);
            job.setContextName(contextId);
            job.setCronExpression("0 0 * * * ?");

            MongoSchedulerJobRecordImpl record = new MongoSchedulerJobRecordImpl();
            record.setId(id);
            record.setAgentName(agent);
            record.setJobName("jobName" + i);
            record.setContextName(contextId);
            record.setDisplayName("displayName" + i);
            record.setTimestamp(System.currentTimeMillis());
            record.setJob(job);

            repository.save(record);
        }
    }

    private void insertInternalEventDrivenJobs(String idPrefix, int num, String contextId) {
        for (int i = 0; i < num; i++) {
            String agent = idPrefix + "agentName" + i;
            String id = "internalEventDrivenJob_" + idPrefix + agent + "_jobName" + i + "_" + contextId;

            InternalEventDrivenJobImpl job = new InternalEventDrivenJobImpl();
            job.setAgentName(agent);
            job.setJobName("jobName" + i);
            job.setContextName(contextId);
            job.setCommandLine("echo test");

            MongoSchedulerJobRecordImpl record = new MongoSchedulerJobRecordImpl();
            record.setId(id);
            record.setAgentName(agent);
            record.setJobName("jobName" + i);
            record.setContextName(contextId);
            record.setDisplayName("displayName" + i);
            record.setTimestamp(System.currentTimeMillis());
            record.setJob(job);

            repository.save(record);
        }
    }

    private void insertModuleMetadata(String agentName) {
        org.bson.Document document = new org.bson.Document();
        document.put(EntityFields.ID, agentName);
        document.put(EntityFields.TYPE, MODULE_METADATA);
        document.put(EntityFields.PAYLOAD_CONTENT, "{\"type\":\"SCHEDULER_AGENT\"}");
        document.put(EntityFields.CREATED_DATE_TIME, System.currentTimeMillis());
        document.put("_class", "org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetadata");

        mongoTemplate.getCollection(IKASAN_COLLECTION_NAME).insertOne(document);
    }
}
