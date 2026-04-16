package org.ikasan.relational.persistence.scheduled.context.service;

import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.context.model.HibernateScheduledContextRecordImpl;
import org.ikasan.relational.persistence.scheduled.context.service.HibernateScheduledContextServiceImpl;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;

import static org.junit.Assert.*;

/**
 * Unit tests for HibernateScheduledContextServiceImpl using mocks.
 */
public class HibernateScheduledContextServiceImplTest {

    private ScheduledContextDao mockDao;
    private ScheduledContextViewDao mockViewDao;
    private HibernateScheduledContextServiceImpl service;

    @Before
    public void setUp() {
        // Create mock implementations
        mockDao = new MockScheduledContextDao();
        mockViewDao = new MockScheduledContextViewDao();
        service = new HibernateScheduledContextServiceImpl(mockDao, mockViewDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new HibernateScheduledContextServiceImpl(null, mockViewDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullViewDao() {
        new HibernateScheduledContextServiceImpl(mockDao, null);
    }

    @Test
    public void testFindAll() {
        // When
        SearchResults<? extends ScheduledContextRecord> results = service.findAll();

        // Then
        assertNotNull(results);
        assertTrue(((MockScheduledContextDao) mockDao).findAllCalled);
    }

    @Test
    public void testFindAllWithLimitAndOffset() {
        // When
        SearchResults<? extends ScheduledContextRecord> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertTrue(((MockScheduledContextDao) mockDao).findAllWithLimitCalled);
    }

    @Test
    public void testFindByFilter() {
        // Given
        ScheduledContextSearchFilter filter = createMockFilter("test");

        // When
        SearchResults<ScheduledContextRecord> results = service.findByFilter(filter, 10, 0, null, null);

        // Then
        assertNotNull(results);
        assertTrue(((MockScheduledContextDao) mockDao).findByFilterCalled);
    }

    @Test
    public void testFindByFilterLite() {
        // Given
        ScheduledContextSearchFilter filter = createMockFilter("test");
        MockScheduledContextDao dao = (MockScheduledContextDao) mockDao;
        dao.setupMockData();

        // When
        SearchResults<ScheduledContextRecordLite> results = service.findByFilterLite(filter, 10, 0, null, null);

        // Then
        assertNotNull(results);
        assertFalse(results.getResultList().isEmpty());
        ScheduledContextRecordLite lite = results.getResultList().get(0);
        assertEquals("test-context", lite.getContextName());
        assertNotNull(lite.getDescription());
    }

    @Test
    public void testFindById() {
        // When
        ScheduledContextRecord result = service.findById("test-id");

        // Then
        assertTrue(((MockScheduledContextDao) mockDao).findByIdCalled);
    }

    @Test
    public void testFindByName() {
        // When
        ScheduledContextRecord result = service.findByName("test-name");

        // Then
        assertTrue(((MockScheduledContextDao) mockDao).findByNameCalled);
    }

    @Test
    public void testSave() {
        // Given
        HibernateScheduledContextRecordImpl record = createMockRecord("save-test");

        // When
        service.save(record);

        // Then
        assertTrue(((MockScheduledContextDao) mockDao).saveCalled);
    }

    @Test
    public void testGetContextView() {
        // When
        service.getContextView("parent", "child");

        // Then
        assertTrue(((MockScheduledContextViewDao) mockViewDao).getContextViewCalled);
    }

    @Test
    public void testSaveContextView() {
        // Given
        ScheduledContextViewRecord viewRecord = new MockScheduledContextViewRecord();

        // When
        service.saveContextView(viewRecord);

        // Then
        assertTrue(((MockScheduledContextViewDao) mockViewDao).saveCalled);
    }

    @Test
    public void testDeleteContext() {
        // When
        service.deleteContext("context-to-delete");

        // Then
        assertTrue(((MockScheduledContextDao) mockDao).deleteContextCalled);
    }

    @Test
    public void testEnableScheduledJobs() {
        // Given
        MockScheduledContextDao dao = (MockScheduledContextDao) mockDao;
        dao.setupMockData();
        ContextTemplate template = dao.mockRecord.getContext();

        // When
        service.enableScheduledJobs(template, "test-user");

        // Then
        assertFalse(template.isQuartzScheduleDrivenJobsDisabledForContext());
        assertTrue(dao.saveCalled);
    }

    @Test
    public void testDisableScheduledJobs() {
        // Given
        MockScheduledContextDao dao = (MockScheduledContextDao) mockDao;
        dao.setupMockData();
        ContextTemplate template = dao.mockRecord.getContext();

        // When
        service.disableScheduledJobs(template, "test-user");

        // Then
        assertTrue(template.isQuartzScheduleDrivenJobsDisabledForContext());
        assertTrue(dao.saveCalled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnableScheduledJobsForNonExistentContext() {
        // Given
        ContextTemplateImpl template = new ContextTemplateImpl();
        template.setName("non-existent");

        // When
        service.enableScheduledJobs(template, "test-user");
    }

    // Helper methods

    private HibernateScheduledContextRecordImpl createMockRecord(String name) {
        HibernateScheduledContextRecordImpl record = new HibernateScheduledContextRecordImpl(name);
        ContextTemplateImpl template = new ContextTemplateImpl();
        template.setName(name);
        template.setDescription("Test description");
        record.setContext(template);
        record.setModifiedBy("test-user");
        return record;
    }

    private ScheduledContextSearchFilter createMockFilter(String contextName) {
        return new ScheduledContextSearchFilter() {
            @Override
            public String getContextName() {
                return contextName;
            }

            @Override
            public void setContextName(String contextName) {
            }

            @Override
            public java.util.List<String> getContextNames() {
                return null;
            }

            @Override
            public void setContextNames(java.util.List<String> contextNames) {
            }
        };
    }

    // Mock implementations

    private static class MockScheduledContextDao implements ScheduledContextDao {
        boolean findAllCalled = false;
        boolean findAllWithLimitCalled = false;
        boolean findByFilterCalled = false;
        boolean findByIdCalled = false;
        boolean findByNameCalled = false;
        boolean saveCalled = false;
        boolean deleteContextCalled = false;
        HibernateScheduledContextRecordImpl mockRecord;

        void setupMockData() {
            mockRecord = new HibernateScheduledContextRecordImpl("test-context");
            ContextTemplateImpl template = new ContextTemplateImpl();
            template.setName("test-context");
            template.setDescription("Test description");
            mockRecord.setContext(template);
            mockRecord.setModifiedBy("test-user");
        }

        @Override
        public SearchResults<ScheduledContextRecord> findAll() {
            findAllCalled = true;
            return new SearchResultsImpl<>(new java.util.ArrayList<>(), 0L, 0L);
        }

        @Override
        public SearchResults<ScheduledContextRecord> findAll(int limit, int offset) {
            findAllWithLimitCalled = true;
            return new SearchResultsImpl<>(new java.util.ArrayList<>(), 0L, 0L);
        }

        @Override
        public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter, int limit,
                                                                   int offset, String sortColumn, String sortOrder) {
            findByFilterCalled = true;
            if (mockRecord != null) {
                java.util.List<ScheduledContextRecord> results = new java.util.ArrayList<>();
                results.add(mockRecord);
                return new SearchResultsImpl<>(results, 1L, 0L);
            }
            return new SearchResultsImpl<>(new java.util.ArrayList<>(), 0L, 0L);
        }

        @Override
        public ScheduledContextRecord findById(String id) {
            findByIdCalled = true;
            return mockRecord;
        }

        @Override
        public ScheduledContextRecord findByName(String name) {
            findByNameCalled = true;
            return mockRecord;
        }

        @Override
        public void save(ScheduledContextRecord scheduledContextRecord) {
            saveCalled = true;
        }

        @Override
        public void deleteContext(String contextName) {
            deleteContextCalled = true;
        }
    }

    private static class MockScheduledContextViewDao implements ScheduledContextViewDao {
        boolean saveCalled = false;
        boolean getContextViewCalled = false;

        @Override
        public void save(ScheduledContextViewRecord scheduledContextViewRecord) {
            saveCalled = true;
        }

        @Override
        public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
            getContextViewCalled = true;
            return new MockScheduledContextViewRecord();
        }
    }

    private static class MockScheduledContextViewRecord implements ScheduledContextViewRecord {
        @Override
        public String getId() {
            return "test-view-id";
        }

        @Override
        public String getParentContextName() {
            return "parent";
        }

        @Override
        public void setParentContextName(String parentContextName) {
        }

        @Override
        public String getContextName() {
            return "child";
        }

        @Override
        public void setContextName(String contextName) {
        }

        @Override
        public String getContextView() {
            return null;
        }

        @Override
        public void setContextView(String contextView) {
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public void setTimestamp(long timestamp) {
        }

        @Override
        public long getModifiedTimestamp() {
            return 0;
        }

        @Override
        public void setModifiedTimestamp(long timestamp) {
        }

        @Override
        public String getModifiedBy() {
            return "test-user";
        }

        @Override
        public void setModifiedBy(String modifiedBy) {
        }
    }
}
