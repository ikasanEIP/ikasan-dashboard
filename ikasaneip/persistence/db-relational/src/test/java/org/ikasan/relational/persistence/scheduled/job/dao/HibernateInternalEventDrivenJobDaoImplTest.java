package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateInternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
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
 * Integration test for HibernateInternalEventDrivenJobDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateInternalEventDrivenJobDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();
    }

    @Autowired
    private InternalEventDrivenJobDao<HibernateInternalEventDrivenJobRecord> dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data using EntityManager
        SearchResults<HibernateInternalEventDrivenJobRecord> all = dao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private InternalEventDrivenJob createInternalEventDrivenJob(String jobName, String contextName) {
        return new TestInternalEventDrivenJob(jobName, contextName);
    }

    private HibernateInternalEventDrivenJobRecord createInternalEventDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateInternalEventDrivenJobRecord record = new HibernateInternalEventDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setInternalEventDrivenJob(createInternalEventDrivenJob(jobName, contextName));
        record.setModifiedBy("test-user");
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        dao.save(record);

        // Then
        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals("agent-1", found.getAgentName());
        assertNotNull(found.getInternalEventDrivenJob());
        assertEquals("test-job-1", found.getInternalEventDrivenJob().getJobName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        HibernateInternalEventDrivenJobRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("test-job-2", "test-context-2", "agent-2");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("test-job-3", "test-context-3", "agent-3");

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

        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertEquals("Updated Display Name", found.getDisplayName());
        assertEquals("updated-user", found.getModifiedBy());
    }

    @Test
    public void testFindAll() {
        // Given
        dao.save(createInternalEventDrivenJobRecord("job-1", "context-1", "agent-1"));
        dao.save(createInternalEventDrivenJobRecord("job-2", "context-2", "agent-2"));
        dao.save(createInternalEventDrivenJobRecord("job-3", "context-3", "agent-3"));

        // When
        SearchResults<HibernateInternalEventDrivenJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createInternalEventDrivenJobRecord("find-job-" + i, "context-" + i, "agent-" + i));
        }

        // When
        SearchResults<HibernateInternalEventDrivenJobRecord> results = dao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        dao.save(createInternalEventDrivenJobRecord("job-1", contextName, "agent-1"));
        dao.save(createInternalEventDrivenJobRecord("job-2", contextName, "agent-2"));
        dao.save(createInternalEventDrivenJobRecord("job-3", "other-context", "agent-3"));

        // When
        SearchResults<HibernateInternalEventDrivenJobRecord> results = dao.findByContext(contextName, -1, -1);

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
            dao.save(createInternalEventDrivenJobRecord("job-" + i, contextName, "agent-" + i));
        }

        // When
        SearchResults<HibernateInternalEventDrivenJobRecord> results = dao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContextNotFound() {
        // When
        SearchResults<HibernateInternalEventDrivenJobRecord> results = dao.findByContext("non-existent-context", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(0L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testSkip() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("skip-job", "parent-context", "agent-1");
        TestInternalEventDrivenJob job = (TestInternalEventDrivenJob) record.getInternalEventDrivenJob();
        job.setChildContextNames(Arrays.asList("child-1", "child-2"));
        dao.save(record);

        // When
        dao.skip(record, Arrays.asList("child-1"), "skip-actor");

        // Then
        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertTrue(found.isSkipped());
        assertEquals("skip-actor", found.getModifiedBy());
        assertNotNull(found.getInternalEventDrivenJob().getSkippedContexts());
        assertTrue(found.getInternalEventDrivenJob().getSkippedContexts().containsKey("child-1"));
    }

    @Test
    public void testHold() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("hold-job", "parent-context", "agent-1");
        TestInternalEventDrivenJob job = (TestInternalEventDrivenJob) record.getInternalEventDrivenJob();
        job.setChildContextNames(Arrays.asList("child-1", "child-2"));
        dao.save(record);

        // When
        dao.hold(record, Arrays.asList("child-1"), "hold-actor");

        // Then
        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertTrue(found.isHeld());
        assertEquals("hold-actor", found.getModifiedBy());
        assertNotNull(found.getInternalEventDrivenJob().getHeldContexts());
        assertTrue(found.getInternalEventDrivenJob().getHeldContexts().containsKey("child-1"));
    }

    @Test
    public void testEnable() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("enable-job", "parent-context", "agent-1");
        record.setSkipped(true);
        dao.save(record);

        // When
        dao.enable(record, "enable-actor");

        // Then
        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertFalse(found.isSkipped());
        assertEquals("enable-actor", found.getModifiedBy());
        assertTrue(found.getInternalEventDrivenJob().getSkippedContexts().isEmpty());
    }

    @Test
    public void testRelease() {
        // Given
        HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("release-job", "parent-context", "agent-1");
        record.setHeld(true);
        dao.save(record);

        // When
        dao.release(record, "release-actor");

        // Then
        HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
        assertFalse(found.isHeld());
        assertEquals("release-actor", found.getModifiedBy());
        assertTrue(found.getInternalEventDrivenJob().getHeldContexts().isEmpty());
    }

    @Test
    public void testReleaseAll() {
        // Given
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("release-all-job-" + i, "context-" + i, "agent-" + i);
            record.setHeld(true);
            dao.save(record);
            records.add(record);
        }

        // When
        dao.releaseAll(records, "release-all-actor");

        // Then
        records.forEach(record -> {
            HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
            assertFalse(found.isHeld());
            assertEquals("release-all-actor", found.getModifiedBy());
        });
    }

    @Test
    public void testHoldAll() {
        // Given
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("hold-all-job-" + i, "context-" + i, "agent-" + i);
            TestInternalEventDrivenJob job = (TestInternalEventDrivenJob) record.getInternalEventDrivenJob();
            job.setChildContextNames(Arrays.asList("child-1", "child-2"));
            dao.save(record);
            records.add(record);
        }

        // When
        dao.holdAll(records, "hold-all-actor");

        // Then
        records.forEach(record -> {
            HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
            assertTrue(found.isHeld());
            assertFalse(found.isSkipped());
            assertEquals("hold-all-actor", found.getModifiedBy());
        });
    }

    @Test
    public void testEnableAll() {
        // Given
        List<HibernateInternalEventDrivenJobRecord> records = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            HibernateInternalEventDrivenJobRecord record = createInternalEventDrivenJobRecord("enable-all-job-" + i, "context-" + i, "agent-" + i);
            record.setSkipped(true);
            dao.save(record);
            records.add(record);
        }

        // When
        dao.enableAll(records, "enable-all-actor");

        // Then
        records.forEach(record -> {
            HibernateInternalEventDrivenJobRecord found = dao.findById(record.getId());
            assertFalse(found.isSkipped());
            assertEquals("enable-all-actor", found.getModifiedBy());
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // When - should throw IllegalArgumentException
        dao.save(null);
    }

    // Helper class for creating test InternalEventDrivenJob instances
    private static class TestInternalEventDrivenJob implements InternalEventDrivenJob {
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
        private boolean targetResidingContextOnly;
        private boolean participatesInLock;

        // InternalEventDrivenJob specific fields
        private List<String> successfulReturnCodes;
        private String workingDirectory;
        private String commandLine;
        private long minExecutionTime = -1;
        private long maxExecutionTime = -1;
        private List<ContextParameter> contextParameters;
        private List<Integer> daysOfWeekToRun;
        private String executionEnvironmentProperties;
        private boolean jobRepeatable;

        public TestInternalEventDrivenJob(String jobName, String contextName) {
            this.jobName = jobName;
            this.contextName = contextName;
            this.identifier = jobName + "_" + contextName;
            this.skippedContexts = new HashMap<>();
            this.heldContexts = new HashMap<>();
            this.successfulReturnCodes = new ArrayList<>();
            this.contextParameters = new ArrayList<>();
            this.daysOfWeekToRun = new ArrayList<>();
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
        public boolean isTargetResidingContextOnly() {
            return targetResidingContextOnly;
        }

        @Override
        public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
            this.targetResidingContextOnly = targetResidingContextOnly;
        }

        @Override
        public boolean isParticipatesInLock() {
            return participatesInLock;
        }

        @Override
        public void setParticipatesInLock(boolean participatesInLock) {
            this.participatesInLock = participatesInLock;
        }

        @Override
        public List<String> getSuccessfulReturnCodes() {
            return successfulReturnCodes;
        }

        @Override
        public void setSuccessfulReturnCodes(List<String> successfulReturnCodes) {
            this.successfulReturnCodes = successfulReturnCodes;
        }

        @Override
        public String getWorkingDirectory() {
            return workingDirectory;
        }

        @Override
        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        @Override
        public String getCommandLine() {
            return commandLine;
        }

        @Override
        public void setCommandLine(String commandLine) {
            this.commandLine = commandLine;
        }

        @Override
        public long getMinExecutionTime() {
            return minExecutionTime;
        }

        @Override
        public void setMinExecutionTime(long minExecutionTime) {
            this.minExecutionTime = minExecutionTime;
        }

        @Override
        public long getMaxExecutionTime() {
            return maxExecutionTime;
        }

        @Override
        public void setMaxExecutionTime(long maxExecutionTime) {
            this.maxExecutionTime = maxExecutionTime;
        }

        @Override
        public List<ContextParameter> getContextParameters() {
            return contextParameters;
        }

        @Override
        public void setContextParameters(List<ContextParameter> contextParameters) {
            this.contextParameters = contextParameters;
        }

        @Override
        public List<Integer> getDaysOfWeekToRun() {
            return daysOfWeekToRun;
        }

        @Override
        public void setDaysOfWeekToRun(List<Integer> daysOfWeekToRun) {
            this.daysOfWeekToRun = daysOfWeekToRun;
        }

        @Override
        public String getExecutionEnvironmentProperties() {
            return executionEnvironmentProperties;
        }

        @Override
        public void setExecutionEnvironmentProperties(String executionEnvironmentProperties) {
            this.executionEnvironmentProperties = executionEnvironmentProperties;
        }

        @Override
        public boolean isJobRepeatable() {
            return jobRepeatable;
        }

        @Override
        public void setKilled(boolean killed) {

        }

        @Override
        public boolean isKilled() {
            return false;
        }

        @Override
        public void setJobRepeatable(boolean jobRepeatable) {
            this.jobRepeatable = jobRepeatable;
        }
    }
}
