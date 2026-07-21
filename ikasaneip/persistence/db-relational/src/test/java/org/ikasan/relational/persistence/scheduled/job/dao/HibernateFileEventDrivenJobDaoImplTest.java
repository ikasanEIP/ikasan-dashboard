package org.ikasan.relational.persistence.scheduled.job.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateFileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.ReplacementPair;
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

import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateFileEventDrivenJobDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateFileEventDrivenJobDaoImplTest {

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
    private FileEventDrivenJobDao<HibernateFileEventDrivenJobRecord> dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data using EntityManager
        SearchResults<HibernateFileEventDrivenJobRecord> all = dao.findAll(-1, -1);
        all.getResultList().forEach(record -> {
        });
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private FileEventDrivenJob createFileEventDrivenJob(String jobName, String contextName) {
        return new TestFileEventDrivenJob(jobName, contextName);
    }

    private HibernateFileEventDrivenJobRecord createFileEventDrivenJobRecord(String jobName, String contextName, String agentName) {
        HibernateFileEventDrivenJobRecord record = new HibernateFileEventDrivenJobRecord();
        record.setJobName(jobName);
        record.setContextName(contextName);
        record.setAgentName(agentName);
        record.setDisplayName("Display: " + jobName);
        record.setFileEventDrivenJob(createFileEventDrivenJob(jobName, contextName));
        record.setModifiedBy("test-user");
        return record;
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("test-job-1", "test-context-1", "agent-1");

        // When
        dao.save(record);

        // Then
        HibernateFileEventDrivenJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-job-1", found.getJobName());
        assertEquals("test-context-1", found.getContextName());
        assertEquals("agent-1", found.getAgentName());
        assertNotNull(found.getFileEventDrivenJob());
        assertEquals("test-job-1", found.getFileEventDrivenJob().getJobName());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        HibernateFileEventDrivenJobRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("test-job-2", "test-context-2", "agent-2");

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("test-job-3", "test-context-3", "agent-3");

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

        HibernateFileEventDrivenJobRecord found = dao.findById(record.getId());
        assertEquals("Updated Display Name", found.getDisplayName());
        assertEquals("updated-user", found.getModifiedBy());
    }

    @Test
    public void testFindAll() {
        // Given
        dao.save(createFileEventDrivenJobRecord("job-1", "context-1", "agent-1"));
        dao.save(createFileEventDrivenJobRecord("job-2", "context-2", "agent-2"));
        dao.save(createFileEventDrivenJobRecord("job-3", "context-3", "agent-3"));

        // When
        SearchResults<HibernateFileEventDrivenJobRecord> results = dao.findAll(-1, -1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 3);
        assertTrue(results.getResultList().size() >= 3);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createFileEventDrivenJobRecord("find-job-" + i, "context-" + i, "agent-" + i));
        }

        // When
        SearchResults<HibernateFileEventDrivenJobRecord> results = dao.findAll(2, 1);

        // Then
        assertNotNull(results);
        assertTrue(results.getTotalNumberOfResults() >= 5);
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContext() {
        // Given
        String contextName = "unique-context";
        dao.save(createFileEventDrivenJobRecord("job-1", contextName, "agent-1"));
        dao.save(createFileEventDrivenJobRecord("job-2", contextName, "agent-2"));
        dao.save(createFileEventDrivenJobRecord("job-3", "other-context", "agent-3"));

        // When
        SearchResults<HibernateFileEventDrivenJobRecord> results = dao.findByContext(contextName, -1, -1);

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
            dao.save(createFileEventDrivenJobRecord("job-" + i, contextName, "agent-" + i));
        }

        // When
        SearchResults<HibernateFileEventDrivenJobRecord> results = dao.findByContext(contextName, 2, 1);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByContextNotFound() {
        // When
        SearchResults<HibernateFileEventDrivenJobRecord> results = dao.findByContext("non-existent-context", -1, -1);

        // Then
        assertNotNull(results);
        assertEquals(0L, results.getTotalNumberOfResults());
        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testJsonSerializationDeserialization() {
        // Given
        HibernateFileEventDrivenJobRecord record = createFileEventDrivenJobRecord("json-job", "json-context", "json-agent");
        FileEventDrivenJob fileEventDrivenJob = record.getFileEventDrivenJob();
        fileEventDrivenJob.setJobDescription("Test description");
        fileEventDrivenJob.setDisplayName("Test Display");
        fileEventDrivenJob.setStartupControlType("MANUAL");
        fileEventDrivenJob.setFilePath("/test/path");
        fileEventDrivenJob.setFilenames(Arrays.asList("file1.txt", "file2.txt"));
        fileEventDrivenJob.setMoveDirectory("/move/directory");

        record.setFileEventDrivenJob(fileEventDrivenJob);

        // When
        dao.save(record);

        // Then
        HibernateFileEventDrivenJobRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertNotNull(found.getFileEventDrivenJob());
        assertEquals("Test description", found.getFileEventDrivenJob().getJobDescription());
        assertEquals("Test Display", found.getFileEventDrivenJob().getDisplayName());
        assertEquals("MANUAL", found.getFileEventDrivenJob().getStartupControlType());
        assertEquals("/test/path", found.getFileEventDrivenJob().getFilePath());
        assertEquals(2, found.getFileEventDrivenJob().getFilenames().size());
        assertEquals("/move/directory", found.getFileEventDrivenJob().getMoveDirectory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveInvalidRecordType() {
        // When - should throw IllegalArgumentException
        dao.save((HibernateFileEventDrivenJobRecord) null);
    }

    // Helper class for creating test FileEventDrivenJob instances
    private static class TestFileEventDrivenJob implements FileEventDrivenJob {
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

        // FileEventDrivenJob fields
        private String filePath;
        private List<String> filenames;
        private String moveDirectory;
        private String encoding;
        private boolean includeHeader;
        private boolean includeTrailer;
        private boolean sortByModifiedDateTime;
        private boolean sortAscending;
        private int directoryDepth;
        private boolean logMatchedFilenames;
        private boolean ignoreFileRenameWhilstScanning;
        private int minFileAgeSeconds;
        private String slaCronExpression;
        private boolean isDynamic;
        private String filePathSpel;
        private String filenameSpel;
        private String moveDirectorySpel;
        private Set<ReplacementPair> filenameReplacementPairs;
        private Set<ReplacementPair> filePathReplacementPairs;
        private Set<ReplacementPair> moveDirectoryReplacementPairs;

        public TestFileEventDrivenJob(String jobName, String contextName) {
            this.jobName = jobName;
            this.contextName = contextName;
            this.identifier = jobName + "_" + contextName;
            this.skippedContexts = new HashMap<>();
            this.heldContexts = new HashMap<>();
            this.passthroughProperties = new HashMap<>();
            this.filenames = new ArrayList<>();
            this.blackoutWindowCronExpressions = new ArrayList<>();
            this.blackoutWindowDateTimeRanges = new HashMap<>();
            this.filenameReplacementPairs = new HashSet<>();
            this.filePathReplacementPairs = new HashSet<>();
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

        @Override
        public String getFilePath() {
            return filePath;
        }

        @Override
        public void setFilePath(String path) {
            this.filePath = path;
        }

        @Override
        public List<String> getFilenames() {
            return filenames;
        }

        @Override
        public void setFilenames(List<String> filenames) {
            this.filenames = filenames;
        }

        @Override
        public String getMoveDirectory() {
            return moveDirectory;
        }

        @Override
        public void setMoveDirectory(String moveDirectory) {
            this.moveDirectory = moveDirectory;
        }

        @Override
        public String getEncoding() {
            return encoding;
        }

        @Override
        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public boolean isIncludeHeader() {
            return includeHeader;
        }

        @Override
        public void setIncludeHeader(boolean includeHeader) {
            this.includeHeader = includeHeader;
        }

        @Override
        public boolean isIncludeTrailer() {
            return includeTrailer;
        }

        @Override
        public void setIncludeTrailer(boolean includeTrailer) {
            this.includeTrailer = includeTrailer;
        }

        @Override
        public boolean isSortByModifiedDateTime() {
            return sortByModifiedDateTime;
        }

        @Override
        public void setSortByModifiedDateTime(boolean sortByModifiedDateTime) {
            this.sortByModifiedDateTime = sortByModifiedDateTime;
        }

        @Override
        public boolean isSortAscending() {
            return sortAscending;
        }

        @Override
        public void setSortAscending(boolean sortAscending) {
            this.sortAscending = sortAscending;
        }

        @Override
        public int getDirectoryDepth() {
            return directoryDepth;
        }

        @Override
        public void setDirectoryDepth(int directoryDepth) {
            this.directoryDepth = directoryDepth;
        }

        @Override
        public boolean isLogMatchedFilenames() {
            return logMatchedFilenames;
        }

        @Override
        public void setLogMatchedFilenames(boolean logMatchedFilenames) {
            this.logMatchedFilenames = logMatchedFilenames;
        }

        @Override
        public boolean isIgnoreFileRenameWhilstScanning() {
            return ignoreFileRenameWhilstScanning;
        }

        @Override
        public void setIgnoreFileRenameWhilstScanning(boolean ignoreFileRenameWhilstScanning) {
            this.ignoreFileRenameWhilstScanning = ignoreFileRenameWhilstScanning;
        }

        @Override
        public int getMinFileAgeSeconds() {
            return minFileAgeSeconds;
        }

        @Override
        public void setMinFileAgeSeconds(int minFileAgeSeconds) {
            this.minFileAgeSeconds = minFileAgeSeconds;
        }

        @Override
        public String getSlaCronExpression() {
            return slaCronExpression;
        }

        @Override
        public void setSlaCronExpression(String slaCronExpression) {
            this.slaCronExpression = slaCronExpression;
        }

        @Override
        public boolean isDynamic() {
            return isDynamic;
        }

        @Override
        public void setDynamic(boolean isDynamic) {
            this.isDynamic = isDynamic;
        }

        @Override
        public String getFilePathSpel() {
            return filePathSpel;
        }

        @Override
        public void setFilePathSpel(String filePathSpel) {
            this.filePathSpel = filePathSpel;
        }

        @Override
        public String getFilenameSpel() {
            return filenameSpel;
        }

        @Override
        public void setFilenameSpel(String filenameSpel) {
            this.filenameSpel = filenameSpel;
        }

        @Override
        public Set<ReplacementPair> getFilenameReplacementPairs() {
            return filenameReplacementPairs;
        }

        @Override
        public void setFilenameReplacementPairs(Set<ReplacementPair> filenameReplacementPairs) {
            this.filenameReplacementPairs = filenameReplacementPairs;
        }

        @Override
        public Set<ReplacementPair> getFilePathReplacementPairs() {
            return filePathReplacementPairs;
        }

        @Override
        public void setFilePathReplacementPairs(Set<ReplacementPair> filePathReplacementPairs) {
            this.filePathReplacementPairs = filePathReplacementPairs;
        }

        @Override
        public String getMoveDirectorySpel() {
            return moveDirectorySpel;
        }

        @Override
        public void setMoveDirectorySpel(String moveDirectorySpel) {
            this.moveDirectorySpel = moveDirectorySpel;
        }

        @Override
        public Set<ReplacementPair> getMoveDirectoryReplacementPairs() {
            return moveDirectoryReplacementPairs;
        }

        @Override
        public void setMoveDirectoryReplacementPairs(Set<ReplacementPair> moveDirectoryReplacementPairs) {
            this.moveDirectoryReplacementPairs = moveDirectoryReplacementPairs;
        }
    }
}
