package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateQuartzScheduleDrivenJobRecord;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateQuartzScheduleDrivenJobDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateQuartzScheduleDrivenJobDaoImplTest {

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
    private QuartzScheduleDrivenJobDao<HibernateQuartzScheduleDrivenJobRecord> dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data using EntityManager
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> all = dao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private QuartzScheduleDrivenJob createQuartzScheduleDrivenJob(String jobName, String contextName) {
        return new TestQuartzScheduleDrivenJob(jobName, contextName);
    }

    private HibernateQuartzScheduleDrivenJobRecord createQuartzScheduleDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateQuartzScheduleDrivenJobRecord record = new HibernateQuartzScheduleDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setQuartzScheduleDrivenJob(createQuartzScheduleDrivenJob(jobName, contextName));
        record.setModifiedBy("test-user");
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateQuartzScheduleDrivenJobRecord record = createQuartzScheduleDrivenJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        dao.save(record);

        // Then
        HibernateQuartzScheduleDrivenJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals("agent-1", found.getAgentName());
        assertNotNull(found.getQuartzScheduleDrivenJob());
        assertEquals("test-job-1", found.getQuartzScheduleDrivenJob().getJobName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        HibernateQuartzScheduleDrivenJobRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateQuartzScheduleDrivenJobRecord record = createQuartzScheduleDrivenJobRecord("test-job-2", "test-context-2", "agent-2");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateQuartzScheduleDrivenJobRecord record = createQuartzScheduleDrivenJobRecord("test-job-3", "test-context-3", "agent-3");

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

        HibernateQuartzScheduleDrivenJobRecord found = dao.findById(record.getId());
        assertEquals("Updated Display Name", found.getDisplayName());
        assertEquals("updated-user", found.getModifiedBy());
    }

    @Test
    public void testFindAll() {
        // Given
        dao.save(createQuartzScheduleDrivenJobRecord("job-1", "context-1", "agent-1"));
        dao.save(createQuartzScheduleDrivenJobRecord("job-2", "context-2", "agent-2"));
        dao.save(createQuartzScheduleDrivenJobRecord("job-3", "context-3", "agent-3"));

        // When
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createQuartzScheduleDrivenJobRecord("find-job-" + i, "context-" + i, "agent-" + i));
        }

        // When
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> results = dao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        dao.save(createQuartzScheduleDrivenJobRecord("job-1", contextName, "agent-1"));
        dao.save(createQuartzScheduleDrivenJobRecord("job-2", contextName, "agent-2"));
        dao.save(createQuartzScheduleDrivenJobRecord("job-3", "other-context", "agent-3"));

        // When
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> results = dao.findByContext(contextName, -1, -1);

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
            dao.save(createQuartzScheduleDrivenJobRecord("job-" + i, contextName, "agent-" + i));
        }

        // When
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> results = dao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContextNotFound() {
        // When
        SearchResults<HibernateQuartzScheduleDrivenJobRecord> results = dao.findByContext("non-existent-context", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(0L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testJsonSerializationDeserialization() {
        // Given
        HibernateQuartzScheduleDrivenJobRecord record = createQuartzScheduleDrivenJobRecord("json-job", "json-context", "json-agent");
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = record.getQuartzScheduleDrivenJob();
        quartzScheduleDrivenJob.setJobDescription("Test description");
        quartzScheduleDrivenJob.setDisplayName("Test Display");
        quartzScheduleDrivenJob.setStartupControlType("MANUAL");
        quartzScheduleDrivenJob.setCronExpression("0 0 12 * * ?");
        quartzScheduleDrivenJob.setTimeZone("UTC");
        quartzScheduleDrivenJob.setJobGroup("test-group");

        record.setQuartzScheduleDrivenJob(quartzScheduleDrivenJob);

        // When
        dao.save(record);

        // Then
        HibernateQuartzScheduleDrivenJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertNotNull(found.getQuartzScheduleDrivenJob());
        assertEquals("Test description", found.getQuartzScheduleDrivenJob().getJobDescription());
        assertEquals("Test Display", found.getQuartzScheduleDrivenJob().getDisplayName());
        assertEquals("MANUAL", found.getQuartzScheduleDrivenJob().getStartupControlType());
        assertEquals("0 0 12 * * ?", found.getQuartzScheduleDrivenJob().getCronExpression());
        assertEquals("UTC", found.getQuartzScheduleDrivenJob().getTimeZone());
        assertEquals("test-group", found.getQuartzScheduleDrivenJob().getJobGroup());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // When - should throw IllegalArgumentException
        dao.save((HibernateQuartzScheduleDrivenJobRecord) null);
    }

    // Helper class for creating test QuartzScheduleDrivenJob instances
    private static class TestQuartzScheduleDrivenJob implements QuartzScheduleDrivenJob {
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

        // QuartzScheduleDrivenJob fields
        private String jobGroup;
        private String cronExpression;
        private String timeZone;
        private Map<String, String> passthroughProperties;
        private boolean eager;
        private int maxEagerCallbacks;
        private boolean ignoreMisfire;
        private long recoveryTolerance;
        private boolean persistentRecovery;
        private List<String> blackoutWindowCronExpressions;
        private Map<String, String> blackoutWindowDateTimeRanges;
        private boolean dropEventOnBlackout;

        public TestQuartzScheduleDrivenJob(String jobName, String contextName) {
            this.jobName = jobName;
            this.contextName = contextName;
            this.identifier = jobName + "_" + contextName;
            this.skippedContexts = new HashMap<>();
            this.heldContexts = new HashMap<>();
            this.passthroughProperties = new HashMap<>();
            this.blackoutWindowCronExpressions = new ArrayList<>();
            this.blackoutWindowDateTimeRanges = new HashMap<>();
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

        @Override
        public String getJobGroup() {
            return jobGroup;
        }

        @Override
        public void setJobGroup(String jobGroup) {
            this.jobGroup = jobGroup;
        }

        @Override
        public String getCronExpression() {
            return cronExpression;
        }

        @Override
        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        @Override
        public String getTimeZone() {
            return timeZone;
        }

        @Override
        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }

        @Override
        public Map<String, String> getPassthroughProperties() {
            return passthroughProperties;
        }

        @Override
        public void setPassthroughProperties(Map<String, String> passthroughProperties) {
            this.passthroughProperties = passthroughProperties;
        }

        @Override
        public boolean isEager() {
            return eager;
        }

        @Override
        public int getMaxEagerCallbacks() {
            return maxEagerCallbacks;
        }

        @Override
        public void setMaxEagerCallbacks(int maxEagerCallbacks) {
            this.maxEagerCallbacks = maxEagerCallbacks;
        }

        @Override
        public void setEager(boolean eager) {
            this.eager = eager;
        }

        @Override
        public void setIgnoreMisfire(boolean ignoreMisfire) {
            this.ignoreMisfire = ignoreMisfire;
        }

        @Override
        public boolean isIgnoreMisfire() {
            return ignoreMisfire;
        }

        @Override
        public long getRecoveryTolerance() {
            return recoveryTolerance;
        }

        @Override
        public void setRecoveryTolerance(long recoveryTolerance) {
            this.recoveryTolerance = recoveryTolerance;
        }

        @Override
        public boolean isPersistentRecovery() {
            return persistentRecovery;
        }

        @Override
        public void setPersistentRecovery(boolean persistentRecovery) {
            this.persistentRecovery = persistentRecovery;
        }

        @Override
        public List<String> getBlackoutWindowCronExpressions() {
            return blackoutWindowCronExpressions;
        }

        @Override
        public void setBlackoutWindowCronExpressions(List<String> blackoutWindowCronExpressions) {
            this.blackoutWindowCronExpressions = blackoutWindowCronExpressions;
        }

        @Override
        public Map<String, String> getBlackoutWindowDateTimeRanges() {
            return blackoutWindowDateTimeRanges;
        }

        @Override
        public void setBlackoutWindowDateTimeRanges(Map<String, String> blackoutWindowDateTimeRanges) {
            this.blackoutWindowDateTimeRanges = blackoutWindowDateTimeRanges;
        }

        @Override
        public boolean isDropEventOnBlackout() {
            return dropEventOnBlackout;
        }

        @Override
        public void setDropEventOnBlackout(boolean dropEventOnBlackout) {
            this.dropEventOnBlackout = dropEventOnBlackout;
        }
    }
}
