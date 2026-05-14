package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateBridgingJobRecord;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateBridgingJobDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateBridgingJobDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();

    }

    @Autowired
    private BridgingJobDao<HibernateBridgingJobRecord> bridgingJobDao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        SearchResults<HibernateBridgingJobRecord> all = bridgingJobDao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
//            bridgingJobDao.delete(record);
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private BridgingJob createBridgingJob(String jobName, String contextName) {
        return new TestBridgingJob(jobName, contextName);
    }

    private HibernateBridgingJobRecord createBridgingJobRecord(String jobName, String contextName, String agentName) {
        HibernateBridgingJobRecord record = new HibernateBridgingJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setBridgingJob(createBridgingJob(jobName, contextName));
        record.setModifiedBy("test-user");
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateBridgingJobRecord record = createBridgingJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        bridgingJobDao.save(record);

        // Then
        HibernateBridgingJobRecord found = bridgingJobDao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals("agent-1", found.getAgentName());
        assertNotNull(found.getBridgingJob());
        assertEquals("test-job-1", found.getBridgingJob().getJobName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        HibernateBridgingJobRecord result = bridgingJobDao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateBridgingJobRecord record = createBridgingJobRecord("test-job-2", "test-context-2", "agent-2");

        // When
        bridgingJobDao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateBridgingJobRecord record = createBridgingJobRecord("test-job-3", "test-context-3", "agent-3");

        // Save initial
        bridgingJobDao.save(record);

        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // Update
        record.setDisplayName("Updated Display Name");
        record.setModifiedBy("updated-user");

        bridgingJobDao.save(record);

        // Then
        assertEquals(initialTimestamp, record.getTimestamp()); // Original timestamp preserved
        assertTrue(record.getModifiedTimestamp() > initialModifiedTimestamp); // Modified timestamp updated

        HibernateBridgingJobRecord found = bridgingJobDao.findById(record.getId());
        assertEquals("Updated Display Name", found.getDisplayName());
        assertEquals("updated-user", found.getModifiedBy());
    }

    @Test
    public void testFindAll() {
        // Given
        bridgingJobDao.save(createBridgingJobRecord("job-1", "context-1", "agent-1"));
        bridgingJobDao.save(createBridgingJobRecord("job-2", "context-2", "agent-2"));
        bridgingJobDao.save(createBridgingJobRecord("job-3", "context-3", "agent-3"));

        // When
        SearchResults<HibernateBridgingJobRecord> results = bridgingJobDao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            bridgingJobDao.save(createBridgingJobRecord("find-job-" + i, "context-" + i, "agent-" + i));
        }

        // When
        SearchResults<HibernateBridgingJobRecord> results = bridgingJobDao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        bridgingJobDao.save(createBridgingJobRecord("job-1", contextName, "agent-1"));
        bridgingJobDao.save(createBridgingJobRecord("job-2", contextName, "agent-2"));
        bridgingJobDao.save(createBridgingJobRecord("job-3", "other-context", "agent-3"));

        // When
        SearchResults<HibernateBridgingJobRecord> results = bridgingJobDao.findByContext(contextName, -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
        results.getResultList().forEach(record ->
            assertEquals(contextName, record.getContextName())
        );
    }

    @Test
    public void testFindByContextWithLimitAndOffset() {
        // Given
        String contextName = "test-context";
        for (int i = 1; i <= 5; i++) {
            bridgingJobDao.save(createBridgingJobRecord("job-" + i, contextName, "agent-" + i));
        }

        // When
        SearchResults<HibernateBridgingJobRecord> results = bridgingJobDao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContextNotFound() {
        // When
        SearchResults<HibernateBridgingJobRecord> results = bridgingJobDao.findByContext("non-existent-context", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(0L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testJsonSerializationDeserialization() {
        // Given
        HibernateBridgingJobRecord record = createBridgingJobRecord("json-job", "json-context", "json-agent");
        BridgingJob bridgingJob = record.getBridgingJob();
        bridgingJob.setJobDescription("Test description");
        bridgingJob.setDisplayName("Test Display");
        bridgingJob.setStartupControlType("MANUAL");

        record.setBridgingJob(bridgingJob);

        // When
        bridgingJobDao.save(record);

        // Then
        HibernateBridgingJobRecord found = bridgingJobDao.findById(record.getId());
        assertNotNull(found);
        assertNotNull(found.getBridgingJob());
        assertEquals("Test description", found.getBridgingJob().getJobDescription());
        assertEquals("Test Display", found.getBridgingJob().getDisplayName());
        assertEquals("MANUAL", found.getBridgingJob().getStartupControlType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // This test validates that the DAO only accepts HibernateBridgingJobRecord
        // However, since the DAO signature already restricts this, this test may not be needed
        // Keeping it for consistency with the pattern

        // When - should throw IllegalArgumentException
        bridgingJobDao.save((HibernateBridgingJobRecord) null);
    }

    // Helper class for creating test BridgingJob instances
    private static class TestBridgingJob implements BridgingJob {
        private String identifier;
        private String contextName;
        private List<String> childContextNames;
        private String agentName;
        private String jobName;
        private String displayName;
        private String jobDescription;
        private String startupControlType;
        private Map<String, Boolean> skippedContexts;
        private Map<String, Boolean> heldContexts;
        private int ordinal;
        private Boolean isTemplateJob;
        private Boolean isTemplateBased;
        private String templateName;

        public TestBridgingJob(String jobName, String contextName) {
            this.jobName = jobName;
            this.contextName = contextName;
            this.identifier = jobName + "_" + contextName;
            this.skippedContexts = new HashMap<>();
            this.heldContexts = new HashMap<>();
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String getContextName() {
            return contextName;
        }

        @Override
        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        @Override
        public List<String> getChildContextNames() {
            return childContextNames;
        }

        @Override
        public void setChildContextNames(List<String> contextIds) {
            this.childContextNames = contextIds;
        }

        @Override
        public String getAgentName() {
            return agentName;
        }

        @Override
        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        @Override
        public String getJobName() {
            return jobName;
        }

        @Override
        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getJobDescription() {
            return jobDescription;
        }

        @Override
        public void setJobDescription(String jobDescription) {
            this.jobDescription = jobDescription;
        }

        @Override
        public String getStartupControlType() {
            return startupControlType;
        }

        @Override
        public void setStartupControlType(String startupControlType) {
            this.startupControlType = startupControlType;
        }

        @Override
        public void setSkippedContexts(Map<String, Boolean> skippedContexts) {
            this.skippedContexts = skippedContexts;
        }

        @Override
        public Map<String, Boolean> getSkippedContexts() {
            return skippedContexts;
        }

        @Override
        public void setHeldContexts(Map<String, Boolean> heldContexts) {
            this.heldContexts = heldContexts;
        }

        @Override
        public Map<String, Boolean> getHeldContexts() {
            return heldContexts;
        }

        @Override
        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public void setTemplateJob(Boolean isTemplateJob) {
            this.isTemplateJob = isTemplateJob;
        }

        @Override
        public Boolean isTemplateJob() {
            return isTemplateJob;
        }

        @Override
        public void setTemplateBased(Boolean isTemplateBased) {
            this.isTemplateBased = isTemplateBased;
        }

        @Override
        public Boolean isTemplateBased() {
            return isTemplateBased;
        }

        @Override
        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        @Override
        public String getTemplateName() {
            return templateName;
        }
    }
}
