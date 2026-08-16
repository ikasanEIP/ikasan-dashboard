package org.ikasan.mongo.persistence.systemevent.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventImpl;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MongoDB DAO Test for SystemEvent operations.
 * This test class contains comprehensive tests for MongoSystemEventDao.
 *
 * Test coverage includes:
 * - Basic CRUD operations (create, save, update, findById)
 * - Search/filter operations (by actor, subject, action, date range, search term)
 * - Wildcard searches
 * - Pagination and sorting
 * - Bulk operations
 * - Special character handling
 * - Expiry filtering
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoSystemEventDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoSystemEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoSystemEventDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoSystemEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoSystemEventDao(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_systemEvent() {
        MongoSystemEventImpl event = new MongoSystemEventImpl();
        event.setSystemEventId(1L);
        event.setModuleName("TestModule");
        event.setActor("admin");
        event.setAction("CREATE");
        event.setSubject("User");
        event.setTimestamp(new Date());

        dao.save(event);

        SystemEvent found = dao.findById("TestModule-systemEvent-1");

        Assert.assertNotNull(found);
        Assert.assertEquals("TestModule", found.getModuleName());
        Assert.assertEquals("admin", found.getActor());
        Assert.assertEquals("CREATE", found.getAction());
        Assert.assertEquals("User", found.getSubject());
    }

    @Test
    public void test_save_systemEvent_with_moduleName() {
        MongoSystemEventImpl event = new MongoSystemEventImpl();
        event.setSystemEventId(100L);
        event.setModuleName("IntegrationModule");
        event.setActor("system");
        event.setAction("UPDATE");
        event.setSubject("Configuration");
        event.setTimestamp(new Date());

        dao.save(event);

        SystemEvent found = dao.findById("IntegrationModule-systemEvent-100");

        Assert.assertNotNull(found);
        Assert.assertEquals("IntegrationModule", found.getModuleName());
        Assert.assertEquals("system", found.getActor());
        Assert.assertEquals(Long.valueOf(100L), found.getId());
    }

    @Test
    public void test_save_systemEvent_without_moduleName() {
        MongoSystemEventImpl event = new MongoSystemEventImpl();
        event.setSystemEventId(200L);
        event.setModuleName(null);
        event.setActor("user123");
        event.setAction("DELETE");
        event.setSubject("Flow");
        event.setTimestamp(new Date());

        dao.save(event);

        SystemEvent found = dao.findById("systemEvent-Flow-200");

        Assert.assertNotNull(found);
        Assert.assertNull(found.getModuleName());
        Assert.assertEquals("user123", found.getActor());
        Assert.assertEquals("Flow", found.getSubject());
        Assert.assertEquals(Long.valueOf(200L), found.getId());
    }

    @Test
    public void test_findById_found() {
        MongoSystemEventImpl event = new MongoSystemEventImpl();
        event.setSystemEventId(300L);
        event.setModuleName("FindModule");
        event.setActor("tester");
        event.setAction("READ");
        event.setSubject("Report");
        event.setTimestamp(new Date());

        dao.save(event);

        SystemEvent found = dao.findById("FindModule-systemEvent-300");

        Assert.assertNotNull(found);
        Assert.assertEquals("tester", found.getActor());
        Assert.assertEquals("READ", found.getAction());
    }

    @Test
    public void test_findById_notFound() {
        SystemEvent found = dao.findById("NonExistentModule-systemEvent-999");

        Assert.assertNull(found);
    }

    @Test
    public void test_findByFilter_by_actor() {
        // Create test data with different actors
        createSystemEvent(1L, "Module1", "john.doe", "CREATE", "User", new Date());
        createSystemEvent(2L, "Module1", "jane.smith", "UPDATE", "User", new Date());
        createSystemEvent(3L, "Module1", "john.doe", "DELETE", "Role", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setActor("john.doe");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertEquals("john.doe", event.getActor());
        }
    }

    @Test
    public void test_findByFilter_by_subject() {
        // Create test data with different subjects
        createSystemEvent(10L, "Module2", "admin", "CREATE", "Configuration", new Date());
        createSystemEvent(11L, "Module2", "admin", "UPDATE", "User", new Date());
        createSystemEvent(12L, "Module2", "admin", "DELETE", "Configuration", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setSubject("Configuration");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertTrue(event.getSubject().contains("Configuration"));
        }
    }

    @Test
    public void test_findByFilter_by_action() {
        // Create test data with different actions
        createSystemEvent(20L, "Module3", "user1", "CREATE", "Flow", new Date());
        createSystemEvent(21L, "Module3", "user2", "UPDATE", "Flow", new Date());
        createSystemEvent(22L, "Module3", "user3", "CREATE", "Module", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setAction("CREATE");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertEquals("CREATE", event.getAction());
        }
    }

    @Test
    public void test_findByFilter_by_searchTerm() {
        // Create test data with different payloads
        MongoSystemEventImpl event1 = createSystemEvent(30L, "Module4", "user1", "CREATE", "Flow", new Date());
        event1.setPayload("{\"type\":\"integration\",\"name\":\"test-flow\"}");
        dao.save(event1);

        MongoSystemEventImpl event2 = createSystemEvent(31L, "Module4", "user2", "UPDATE", "Component", new Date());
        event2.setPayload("{\"type\":\"component\",\"name\":\"other-component\"}");
        dao.save(event2);

        MongoSystemEventImpl event3 = createSystemEvent(32L, "Module4", "user3", "DELETE", "Flow", new Date());
        event3.setPayload("{\"type\":\"integration\",\"name\":\"another-flow\"}");
        dao.save(event3);

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setSearchTerm("integration");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_by_dateRange() {
        // Create test data with different timestamps
        long baseTime = System.currentTimeMillis();
        createSystemEvent(40L, "Module5", "user1", "CREATE", "Flow", new Date(baseTime - 10000));
        createSystemEvent(41L, "Module5", "user2", "UPDATE", "Flow", new Date(baseTime));
        createSystemEvent(42L, "Module5", "user3", "DELETE", "Flow", new Date(baseTime + 10000));

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setStartTime(baseTime - 5000);
        filter.setEndTime(baseTime + 5000);

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals("user2", results.getResultList().get(0).getActor());
    }

    @Test
    public void test_findByFilter_with_multiple_filters() {
        // Create test data
        long baseTime = System.currentTimeMillis();
        createSystemEvent(50L, "Module6", "admin", "CREATE", "User", new Date(baseTime));
        createSystemEvent(51L, "Module6", "admin", "UPDATE", "Role", new Date(baseTime));
        createSystemEvent(52L, "Module6", "user1", "CREATE", "User", new Date(baseTime));

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setActor("admin");
        filter.setSubject("User");
        filter.setAction("CREATE");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getTotalNumberOfResults());

        SystemEvent event = results.getResultList().get(0);
        Assert.assertEquals("admin", event.getActor());
        Assert.assertEquals("User", event.getSubject());
        Assert.assertEquals("CREATE", event.getAction());
    }

    @Test
    public void test_findByFilter_with_pagination() {
        // Create 15 system events
        for (int i = 0; i < 15; i++) {
            createSystemEvent(60L + i, "Module7", "user" + i, "CREATE", "Flow", new Date());
        }

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();

        // Get first page (5 items)
        SearchResults<SystemEvent> page1 = dao.findByFilter(filter, 5, 0, null, null);
        Assert.assertEquals(15, page1.getTotalNumberOfResults());
        Assert.assertEquals(5, page1.getResultList().size());

        // Get second page (5 items)
        SearchResults<SystemEvent> page2 = dao.findByFilter(filter, 5, 5, null, null);
        Assert.assertEquals(15, page2.getTotalNumberOfResults());
        Assert.assertEquals(5, page2.getResultList().size());

        // Get third page (5 items)
        SearchResults<SystemEvent> page3 = dao.findByFilter(filter, 5, 10, null, null);
        Assert.assertEquals(15, page3.getTotalNumberOfResults());
        Assert.assertEquals(5, page3.getResultList().size());
    }

    @Test
    public void test_findByFilter_with_sorting_ascending() {
        // Create test data with different timestamps
        long baseTime = System.currentTimeMillis();
        createSystemEvent(70L, "Module8", "user1", "CREATE", "Flow", new Date(baseTime + 2000));
        createSystemEvent(71L, "Module8", "user2", "CREATE", "Flow", new Date(baseTime));
        createSystemEvent(72L, "Module8", "user3", "CREATE", "Flow", new Date(baseTime + 1000));

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, "timestamp", "ASCENDING");

        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.getTotalNumberOfResults());

        List<SystemEvent> resultList = results.getResultList();
        Assert.assertEquals("user2", resultList.get(0).getActor());
        Assert.assertEquals("user3", resultList.get(1).getActor());
        Assert.assertEquals("user1", resultList.get(2).getActor());
    }

    @Test
    public void test_findByFilter_with_sorting_descending() {
        // Create test data with different timestamps
        long baseTime = System.currentTimeMillis();
        createSystemEvent(80L, "Module9", "user1", "CREATE", "Flow", new Date(baseTime));
        createSystemEvent(81L, "Module9", "user2", "CREATE", "Flow", new Date(baseTime + 1000));
        createSystemEvent(82L, "Module9", "user3", "CREATE", "Flow", new Date(baseTime + 2000));

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();

        // Default sorting should be descending by timestamp
        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, "timestamp", "DESCENDING");

        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.getTotalNumberOfResults());

        List<SystemEvent> resultList = results.getResultList();
        Assert.assertEquals("user3", resultList.get(0).getActor());
        Assert.assertEquals("user2", resultList.get(1).getActor());
        Assert.assertEquals("user1", resultList.get(2).getActor());
    }

    @Test
    public void test_findByFilter_empty_results() {
        createSystemEvent(90L, "Module10", "user1", "CREATE", "Flow", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setActor("nonexistent");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void test_findByFilter_with_special_characters() {
        // Create test data with special characters
        createSystemEvent(100L, "Module11", "user.name@domain.com", "CREATE", "Flow-Test", new Date());
        createSystemEvent(101L, "Module11", "admin", "UPDATE", "Component", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setActor("user.name@domain.com");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals("user.name@domain.com", results.getResultList().get(0).getActor());
    }

    @Test
    public void test_save_bulk_systemEvents() {
        List<SystemEvent> events = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            MongoSystemEventImpl event = new MongoSystemEventImpl();
            event.setSystemEventId(110L + i);
            event.setModuleName("BulkModule");
            event.setActor("bulkUser" + i);
            event.setAction("CREATE");
            event.setSubject("Flow");
            event.setTimestamp(new Date());
            events.add(event);
        }

        dao.save(events);

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        SearchResults<SystemEvent> results = dao.findByFilter(filter, 20, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_wildcard_actor() {
        createSystemEvent(120L, "Module12", "john.doe", "CREATE", "Flow", new Date());
        createSystemEvent(121L, "Module12", "john.smith", "UPDATE", "Flow", new Date());
        createSystemEvent(122L, "Module12", "jane.doe", "DELETE", "Flow", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setActor("john");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertTrue(event.getActor().contains("john"));
        }
    }

    @Test
    public void test_findByFilter_wildcard_subject() {
        createSystemEvent(130L, "Module13", "admin", "CREATE", "UserFlow", new Date());
        createSystemEvent(131L, "Module13", "admin", "UPDATE", "AdminFlow", new Date());
        createSystemEvent(132L, "Module13", "admin", "DELETE", "UserModule", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setSubject("Flow");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertTrue(event.getSubject().contains("Flow"));
        }
    }

    @Test
    public void test_findByFilter_wildcard_action() {
        createSystemEvent(140L, "Module14", "user1", "CREATE_USER", "User", new Date());
        createSystemEvent(141L, "Module14", "user2", "CREATE_ROLE", "Role", new Date());
        createSystemEvent(142L, "Module14", "user3", "UPDATE_USER", "User", new Date());

        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        filter.setAction("CREATE");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertTrue(event.getAction().contains("CREATE"));
        }
    }

    @Test
    public void test_expiry_filtering() {
        // Create events with different expiry dates
        long currentTime = System.currentTimeMillis();

        MongoSystemEventImpl expiredEvent = createSystemEvent(150L, "Module15", "user1", "CREATE", "Flow", new Date(currentTime));
        expiredEvent.setExpiry(new Date(currentTime - 10000)); // Already expired
        dao.save(expiredEvent);

        MongoSystemEventImpl validEvent = createSystemEvent(151L, "Module15", "user2", "UPDATE", "Flow", new Date(currentTime));
        validEvent.setExpiry(new Date(currentTime + 10000)); // Not expired
        dao.save(validEvent);

        // Verify both events exist before deletion
        TestSystemEventSearchFilter filter = new TestSystemEventSearchFilter();
        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        // Delete expired events
        dao.deleteExpired();

        // Verify only the valid event remains
        results = dao.findByFilter(filter, 10, 0, null, null);
        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals("user2", results.getResultList().get(0).getActor());
    }

    @Test
    public void test_save_updates_existing() {
        MongoSystemEventImpl event = createSystemEvent(160L, "Module16", "user1", "CREATE", "Flow", new Date());
        dao.save(event);

        SystemEvent found = dao.findById("Module16-systemEvent-160");
        Assert.assertNotNull(found);
        Assert.assertEquals("CREATE", found.getAction());

        // Update the same event
        event.setAction("UPDATE");
        dao.save(event);

        found = dao.findById("Module16-systemEvent-160");
        Assert.assertNotNull(found);
        Assert.assertEquals("UPDATE", found.getAction());
    }

    /**
     * Helper method to create a system event with common fields.
     */
    private MongoSystemEventImpl createSystemEvent(Long id, String moduleName, String actor,
                                                    String action, String subject, Date timestamp) {
        MongoSystemEventImpl event = new MongoSystemEventImpl();
        event.setSystemEventId(id);
        event.setModuleName(moduleName);
        event.setActor(actor);
        event.setAction(action);
        event.setSubject(subject);
        event.setTimestamp(timestamp);
        dao.save(event);
        return event;
    }

    /**
     * Test implementation of SystemEventSearchFilter for testing purposes.
     * This class provides a simple implementation with getters and setters
     * for all filter criteria used by MongoSystemEventDao.
     */
    private static class TestSystemEventSearchFilter implements SystemEventSearchFilter {
        private String actor;
        private String subject;
        private String action;
        private String searchTerm;
        private long startTime;
        private long endTime;

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
    }
}
