package org.ikasan.mongo.persistence.scheduled.context.dao;

import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.*;

/**
 * Integration test for MongoScheduledContextDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoScheduledContextDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private ScheduledContextDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @After
    public void tearDown() {
        this.dao.findAll().getResultList().forEach(scheduledContextRecord -> {
            this.dao.deleteContext(scheduledContextRecord.getContextName());
        });
    }

    private ContextTemplate createContextTemplate(String name, boolean disabled) {
        ContextTemplateImpl template = new ContextTemplateImpl();
        template.setName(name);
        template.setDescription("Test context template");
        template.setDisabled(disabled);
        template.setQuartzScheduleDrivenJobsDisabledForContext(false);
        return template;
    }

    private MongoScheduledContextRecordImpl createScheduledContextRecord(String contextName, boolean disabled) {
        MongoScheduledContextRecordImpl record = new MongoScheduledContextRecordImpl(contextName);
        record.setContext(createContextTemplate(contextName, disabled));
        record.setModifiedBy("test-user");
        return record;
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void testSaveAndFindById() {
        // Given
        MongoScheduledContextRecordImpl record = createScheduledContextRecord("test-context-1", false);

        // When
        dao.save(record);

        // Then
        ScheduledContextRecord found = dao.findById(record.getId());
        assertNotNull(found);
        assertEquals("test-context-1", found.getContextName());
        assertNotNull(found.getContext());
        assertEquals("Test context template", found.getContext().getDescription());
        assertFalse(found.isDisabled());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        ScheduledContextRecord result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testSaveUpdatesTimestamps() {
        // Given
        MongoScheduledContextRecordImpl record = createScheduledContextRecord("test-context-2", false);

        // When
        dao.save(record);

        // Then
        assertTrue(record.getTimestamp() > 0);
        assertTrue(record.getModifiedTimestamp() > 0);
        assertEquals(record.getContextName(), record.getId());
    }

    @Test
    public void testSaveAndUpdate() throws InterruptedException {
        // Given
        MongoScheduledContextRecordImpl record = createScheduledContextRecord("test-context-3", false);

        // Save initial
        dao.save(record);

        long initialTimestamp = record.getTimestamp();
        long initialModifiedTimestamp = record.getModifiedTimestamp();

        Thread.sleep(10); // Ensure time difference

        // Update
        ContextTemplateImpl updatedTemplate = (ContextTemplateImpl) record.getContext();
        updatedTemplate.setDescription("Updated description");
        record.setContext(updatedTemplate);

        dao.save(record);

        // Then
        assertEquals(initialTimestamp, record.getTimestamp()); // Original timestamp preserved
        assertTrue(record.getModifiedTimestamp() > initialModifiedTimestamp); // Modified timestamp updated

        ScheduledContextRecord found = dao.findById(record.getId());
        assertEquals("Updated description", found.getContext().getDescription());
    }

    @Test
    public void testFindAll() {
        // Given
        dao.save(createScheduledContextRecord("context-1", false));
        dao.save(createScheduledContextRecord("context-2", false));
        dao.save(createScheduledContextRecord("context-3", true));

        // When
        SearchResults<ScheduledContextRecord> results = dao.findAll();

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        assertEquals(3, results.getResultList().size());
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createScheduledContextRecord("context-" + i, false));
        }

        // When
        SearchResults<ScheduledContextRecord> results = dao.findAll(2, 2);

        // Then
        assertNotNull(results);
        assertEquals(5L, results.getTotalNumberOfResults());
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void testFindByName() {
        // Given
        String contextName = "unique-context";
        dao.save(createScheduledContextRecord(contextName, false));
        dao.save(createScheduledContextRecord("other-context", false));

        // When
        ScheduledContextRecord found = dao.findByName(contextName);

        // Then
        assertNotNull(found);
        assertEquals(contextName, found.getContextName());
    }

    @Test
    public void testFindByNameNotFound() {
        // When
        ScheduledContextRecord result = dao.findByName("non-existent-name");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindByFilterWithContextName() {
        // Given
        dao.save(createScheduledContextRecord("production-context", false));
        dao.save(createScheduledContextRecord("staging-context", false));
        dao.save(createScheduledContextRecord("development-context", false));

        // When
        ScheduledContextSearchFilter filter = new ScheduledContextSearchFilter() {
            @Override
            public String getContextName() {
                return "prod";
            }

            @Override
            public void setContextName(String contextName) {}

            @Override
            public java.util.List<String> getContextNames() {
                return null;
            }

            @Override
            public void setContextNames(java.util.List<String> contextNames) {}
        };

        SearchResults<ScheduledContextRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1L, results.getTotalNumberOfResults());
        assertEquals("production-context", results.getResultList().get(0).getContextName());
    }

    @Test
    public void testFindByFilterWithContextNames() {
        // Given
        dao.save(createScheduledContextRecord("production-context", false));
        dao.save(createScheduledContextRecord("staging-context", false));
        dao.save(createScheduledContextRecord("development-context", false));

        // When
        ScheduledContextSearchFilter filter = new ScheduledContextSearchFilter() {
            @Override
            public String getContextName() {
                return null;
            }

            @Override
            public void setContextName(String contextName) {}

            @Override
            public java.util.List<String> getContextNames() {
                return java.util.Arrays.asList("production-context", "staging-context");
            }

            @Override
            public void setContextNames(java.util.List<String> contextNames) {}
        };

        SearchResults<ScheduledContextRecord> results = dao.findByFilter(filter, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindByFilterNoFilter() {
        // Given
        dao.save(createScheduledContextRecord("context-a", false));
        dao.save(createScheduledContextRecord("context-b", false));

        // When
        SearchResults<ScheduledContextRecord> results = dao.findByFilter(null, -1, -1, null, null);

        // Then
        assertNotNull(results);
        assertEquals(2L, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindByFilterWithSortAscending() {
        // Given
        dao.save(createScheduledContextRecord("zebra-context", false));
        dao.save(createScheduledContextRecord("alpha-context", false));
        dao.save(createScheduledContextRecord("beta-context", false));

        // When
        SearchResults<ScheduledContextRecord> results = dao.findByFilter(null, -1, -1, "contextName", "ASCENDING");

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        assertEquals("alpha-context", results.getResultList().get(0).getContextName());
        assertEquals("beta-context", results.getResultList().get(1).getContextName());
        assertEquals("zebra-context", results.getResultList().get(2).getContextName());
    }

    @Test
    public void testFindByFilterWithSortDescending() throws InterruptedException {
        // Given
        dao.save(createScheduledContextRecord("context-1", false));
        Thread.sleep(10);
        dao.save(createScheduledContextRecord("context-2", false));
        Thread.sleep(10);
        dao.save(createScheduledContextRecord("context-3", false));

        // When
        SearchResults<ScheduledContextRecord> results = dao.findByFilter(null, -1, -1, "timestamp", "DESCENDING");

        // Then
        assertNotNull(results);
        assertEquals(3L, results.getTotalNumberOfResults());
        // Most recent first
        assertEquals("context-3", results.getResultList().get(0).getContextName());
    }

    @Test
    public void testDeleteContext() {
        // Given
        String contextName = "context-to-delete";
        ScheduledContextRecord scheduledContextRecord = createScheduledContextRecord(contextName, false);
        dao.save(scheduledContextRecord);
        // Verify it exists
        assertNotNull(dao.findByName(contextName));

        // When
        dao.deleteContext(contextName);

        // Then
        assertNull(dao.findByName(contextName));
    }

    @Test
    public void testDeleteContextNotFound() {
        // When - should not throw exception
        dao.deleteContext("non-existent-context");
        // Then - no exception thrown
    }

    @Test
    public void testSaveNonMongoRecord() {
        // Given - Create a generic ScheduledContextRecord (not MongoScheduledContextRecordImpl)
        ScheduledContextRecord genericRecord = new ScheduledContextRecord() {
            private String id = "generic-id";
            private String contextName = "generic-context";
            private ContextTemplate context = createContextTemplate("generic-context", false);
            private long timestamp = 0;
            private long modifiedTimestamp = 0;
            private String modifiedBy = "test";

            @Override
            public String getId() { return id; }

            @Override
            public String getContextName() { return contextName; }

            @Override
            public void setContextName(String contextName) { this.contextName = contextName; }

            @Override
            public ContextTemplate getContext() { return context; }

            @Override
            public void setContext(ContextTemplate context) { this.context = context; }

            @Override
            public long getTimestamp() { return timestamp; }

            @Override
            public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

            @Override
            public long getModifiedTimestamp() { return modifiedTimestamp; }

            @Override
            public void setModifiedTimestamp(long timestamp) { this.modifiedTimestamp = timestamp; }

            @Override
            public String getModifiedBy() { return modifiedBy; }

            @Override
            public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

            @Override
            public boolean isDisabled() { return false; }

            @Override
            public boolean isQuartzScheduleDrivenJobsDisabledForContext() { return false; }
        };

        // When - DAO should convert it to MongoScheduledContextRecordImpl
        dao.save(genericRecord);

        // Then
        ScheduledContextRecord found = dao.findByName("generic-context");
        assertNotNull(found);
        assertEquals("generic-context", found.getContextName());
    }

    @Test
    public void testJsonStorageAndRetrieval() {
        // Given - Create a context with complex nested structure
        MongoScheduledContextRecordImpl record = createScheduledContextRecord("complex-context", false);
        ContextTemplateImpl template = (ContextTemplateImpl) record.getContext();
        template.setTimeWindowStart("09:00");
        template.setTimezone("UTC");

        // When
        dao.save(record);

        // Then
        ScheduledContextRecord found = dao.findById(record.getId());
        assertNotNull(found);
        ContextTemplate foundTemplate = found.getContext();
        assertNotNull(foundTemplate);
        assertEquals("09:00", foundTemplate.getTimeWindowStart());
        assertEquals("UTC", foundTemplate.getTimezone());
    }

    @Test
    public void testDenormalizedFieldsAreUpdated() {
        // Given
        MongoScheduledContextRecordImpl record = createScheduledContextRecord("denormalized-test", false);

        // When - Save with disabled = false
        dao.save(record);

        // Then
        ScheduledContextRecord found = dao.findById(record.getId());
        assertFalse(found.isDisabled());

        // When - Update to disabled = true
        ContextTemplateImpl template = (ContextTemplateImpl) found.getContext();
        template.setDisabled(true);
        template.setQuartzScheduleDrivenJobsDisabledForContext(true);
        ((MongoScheduledContextRecordImpl) found).setContext(template);

        dao.save((MongoScheduledContextRecordImpl) found);

        // Then - Denormalized fields should be updated
        ScheduledContextRecord updated = dao.findById(record.getId());
        assertTrue(updated.isDisabled());
        assertTrue(updated.isQuartzScheduleDrivenJobsDisabledForContext());
    }
}
