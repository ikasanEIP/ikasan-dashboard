package org.ikasan.mongo.persistence.systemevent.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventSearchFilter;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.junit.*;
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

    @Autowired
    private MongoSystemEventDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
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
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
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
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
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
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
        event.setSystemEventId(200L);
        event.setModuleName(null);
        event.setActor("user123");
        event.setAction("DELETE");
        event.setSubject("Flowimpl");
        event.setTimestamp(new Date());

        dao.save(event);

        SystemEvent found = dao.findById("systemEvent-Flowimpl-200");

        Assert.assertNotNull(found);
        Assert.assertNull(found.getModuleName());
        Assert.assertEquals("user123", found.getActor());
        Assert.assertEquals("Flowimpl", found.getSubject());
        Assert.assertEquals(Long.valueOf(200L), found.getId());
    }

    @Test
    public void test_findById_found() {
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
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

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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
        createSystemEvent(20L, "Module3", "user1", "CREATE", "Flowimpl", new Date());
        createSystemEvent(21L, "Module3", "user2", "UPDATE", "Flowimpl", new Date());
        createSystemEvent(22L, "Module3", "user3", "CREATE", "Module", new Date());

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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
        MongoSystemEventRecordImpl event1 = createSystemEvent(30L, "Module4", "user1", "CREATE", "Flowimpl", new Date());
        event1.setPayload("{\"type\":\"integration\",\"name\":\"test-flow\"}");
        dao.save(event1);

        MongoSystemEventRecordImpl event2 = createSystemEvent(31L, "Module4", "user2", "UPDATE", "Component", new Date());
        event2.setPayload("{\"type\":\"component\",\"name\":\"other-component\"}");
        dao.save(event2);

        MongoSystemEventRecordImpl event3 = createSystemEvent(32L, "Module4", "user3", "DELETE", "Flowimpl", new Date());
        event3.setPayload("{\"type\":\"integration\",\"name\":\"another-flow\"}");
        dao.save(event3);

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setSearchTerm("integration");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_by_dateRange() {
        // Create test data with different timestamps
        long baseTime = System.currentTimeMillis();
        createSystemEvent(40L, "Module5", "user1", "CREATE", "Flowimpl", new Date(baseTime - 10000));
        createSystemEvent(41L, "Module5", "user2", "UPDATE", "Flowimpl", new Date(baseTime));
        createSystemEvent(42L, "Module5", "user3", "DELETE", "Flowimpl", new Date(baseTime + 10000));

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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
            createSystemEvent(60L + i, "Module7", "user" + i, "CREATE", "Flowimpl", new Date());
        }

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();

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
        createSystemEvent(70L, "Module8", "user1", "CREATE", "Flowimpl", new Date(baseTime + 2000));
        createSystemEvent(71L, "Module8", "user2", "CREATE", "Flowimpl", new Date(baseTime));
        createSystemEvent(72L, "Module8", "user3", "CREATE", "Flowimpl", new Date(baseTime + 1000));

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();

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
        createSystemEvent(80L, "Module9", "user1", "CREATE", "Flowimpl", new Date(baseTime));
        createSystemEvent(81L, "Module9", "user2", "CREATE", "Flowimpl", new Date(baseTime + 1000));
        createSystemEvent(82L, "Module9", "user3", "CREATE", "Flowimpl", new Date(baseTime + 2000));

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();

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
        createSystemEvent(90L, "Module10", "user1", "CREATE", "Flowimpl", new Date());

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setActor("nonexistent");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void test_findByFilter_with_special_characters() {
        // Create test data with special characters
        createSystemEvent(100L, "Module11", "user.name@domain.com", "CREATE", "Flowimpl-Test", new Date());
        createSystemEvent(101L, "Module11", "admin", "UPDATE", "Component", new Date());

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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
            MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
            event.setSystemEventId(110L + i);
            event.setModuleName("BulkModule");
            event.setActor("bulkUser" + i);
            event.setAction("CREATE");
            event.setSubject("Flowimpl");
            event.setTimestamp(new Date());
            events.add(event);
        }

        dao.save(events);

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        SearchResults<SystemEvent> results = dao.findByFilter(filter, 20, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(10, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_wildcard_actor() {
        createSystemEvent(120L, "Module12", "john.doe", "CREATE", "Flowimpl", new Date());
        createSystemEvent(121L, "Module12", "john.smith", "UPDATE", "Flowimpl", new Date());
        createSystemEvent(122L, "Module12", "jane.doe", "DELETE", "Flowimpl", new Date());

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
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

        MongoSystemEventSearchFilter filter = new MongoSystemEventSearchFilter();
        filter.setAction("CREATE");

        SearchResults<SystemEvent> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getTotalNumberOfResults());

        for (SystemEvent event : results.getResultList()) {
            Assert.assertTrue(event.getAction().contains("CREATE"));
        }
    }

    @Test
    public void test_save_updates_existing() {
        MongoSystemEventRecordImpl event = createSystemEvent(160L, "Module16", "user1", "CREATE", "Flowimpl", new Date());
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
    private MongoSystemEventRecordImpl createSystemEvent(Long id, String moduleName, String actor,
                                                         String action, String subject, Date timestamp) {
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
        event.setSystemEventId(id);
        event.setModuleName(moduleName);
        event.setActor(actor);
        event.setAction(action);
        event.setSubject(subject);
        event.setTimestamp(timestamp);
        dao.save(event);
        return event;
    }
    
}
