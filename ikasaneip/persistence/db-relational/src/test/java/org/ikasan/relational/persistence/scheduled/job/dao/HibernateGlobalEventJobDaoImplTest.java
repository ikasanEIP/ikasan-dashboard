package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateGlobalEventJobRecord;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateGlobalEventJobDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateGlobalEventJobDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private GlobalEventJobDao<HibernateGlobalEventJobRecord> dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data using EntityManager
        SearchResults<HibernateGlobalEventJobRecord> all = dao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private GlobalEventJob createGlobalEventJob(String jobName, String contextName) {
        return new TestGlobalEventJob(jobName, contextName);
    }

    private HibernateGlobalEventJobRecord createGlobalEventJobRecord(String jobName, String contextName, String agentName) {
        HibernateGlobalEventJobRecord record = new HibernateGlobalEventJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setGlobalEventJob(createGlobalEventJob(jobName, contextName));
        record.setModifiedBy("test-user");
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        dao.save(record);

        // Then
        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals("agent-1", found.getAgentName());
        assertNotNull(found.getGlobalEventJob());
        assertEquals("test-job-1", found.getGlobalEventJob().getJobName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        HibernateGlobalEventJobRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("test-job-2", "test-context-2", "agent-2");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("test-job-3", "test-context-3", "agent-3");

        // Save initial
        dao.save(record);

        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // Update
        record.setDisplayName("Updated Display Name");
        record.setModifiedBy("updated-user");

        dao.save(record);

        // Then
        assertEquals(initialTimestamp, record.getTimestamp()); // Original timestamp preserved
        assertTrue(record.getModifiedTimestamp() > initialModifiedTimestamp); // Modified timestamp updated

        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertEquals("Updated Display Name", found.getDisplayName());
        assertEquals("updated-user", found.getModifiedBy());
    }

    @Test
    public void testFindAll() {
        // Given
        dao.save(createGlobalEventJobRecord("job-1", "context-1", "agent-1"));
        dao.save(createGlobalEventJobRecord("job-2", "context-2", "agent-2"));
        dao.save(createGlobalEventJobRecord("job-3", "context-3", "agent-3"));

        // When
        SearchResults<HibernateGlobalEventJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createGlobalEventJobRecord("find-job-" + i, "context-" + i, "agent-" + i));
        }

        // When
        SearchResults<HibernateGlobalEventJobRecord> results = dao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        dao.save(createGlobalEventJobRecord("job-1", contextName, "agent-1"));
        dao.save(createGlobalEventJobRecord("job-2", contextName, "agent-2"));
        dao.save(createGlobalEventJobRecord("job-3", "other-context", "agent-3"));

        // When
        SearchResults<HibernateGlobalEventJobRecord> results = dao.findByContext(contextName, -1, -1);

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
            dao.save(createGlobalEventJobRecord("job-" + i, contextName, "agent-" + i));
        }

        // When
        SearchResults<HibernateGlobalEventJobRecord> results = dao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContextNotFound() {
        // When
        SearchResults<HibernateGlobalEventJobRecord> results = dao.findByContext("non-existent-context", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(0L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testJsonSerializationDeserialization() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("json-job", "json-context", "json-agent");
        GlobalEventJob globalEventJob = record.getGlobalEventJob();
        globalEventJob.setJobDescription("Test description");
        globalEventJob.setDisplayName("Test Display");
        globalEventJob.setStartupControlType("MANUAL");

        record.setGlobalEventJob(globalEventJob);

        // When
        dao.save(record);

        // Then
        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertNotNull(found.getGlobalEventJob());
        assertEquals("Test description", found.getGlobalEventJob().getJobDescription());
        assertEquals("Test Display", found.getGlobalEventJob().getDisplayName());
        assertEquals("MANUAL", found.getGlobalEventJob().getStartupControlType());
    }

    @Test
    public void testSkipInAllContexts() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("skip-job", "skip-context", "agent-1");
        dao.save(record);

        // When - skip in all contexts (null childContextNames)
        dao.skip(record, null, "skip-actor");

        // Then
        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("skip-actor", found.getModifiedBy());
        assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("skip-context"));
        assertTrue(found.getGlobalEventJob().getSkippedContexts().get("skip-context"));
    }

    @Test
    public void testSkipInSpecificChildContexts() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("skip-job-2", "parent-context", "agent-2");
        dao.save(record);

        // When - skip in specific child contexts
        List<String> childContexts = Arrays.asList("child-context-1", "child-context-2");
        dao.skip(record, childContexts, "skip-actor");

        // Then
        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("skip-actor", found.getModifiedBy());
        assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("child-context-1"));
        assertTrue(found.getGlobalEventJob().getSkippedContexts().containsKey("child-context-2"));
        assertTrue(found.getGlobalEventJob().getSkippedContexts().get("child-context-1"));
        assertTrue(found.getGlobalEventJob().getSkippedContexts().get("child-context-2"));
    }

    @Test
    public void testEnable() {
        // Given
        HibernateGlobalEventJobRecord record = createGlobalEventJobRecord("enable-job", "enable-context", "agent-3");
        dao.save(record);

        // First skip the job
        dao.skip(record, null, "skip-actor");

        // When - enable the job
        dao.enable(record, "enable-actor");

        // Then
        HibernateGlobalEventJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("enable-actor", found.getModifiedBy());
        assertTrue(found.getGlobalEventJob().getSkippedContexts().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // When - should throw IllegalArgumentException
        dao.save((HibernateGlobalEventJobRecord) null);
    }

    // Helper class for creating test GlobalEventJob instances
    private static class TestGlobalEventJob implements GlobalEventJob {
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

        public TestGlobalEventJob(String jobName, String contextName) {
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
