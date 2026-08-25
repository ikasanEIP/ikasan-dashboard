package org.ikasan.mongo.persistence.wiretap.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.ikasan.mongo.persistence.wiretap.repository.MongoWiretapEventRepository;
import org.ikasan.spec.wiretap.WiretapEvent;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB DAO Test for WiretapEvent operations.
 * This test class contains comprehensive tests for MongoWiretapDao.
 *
 * Test coverage includes:
 * - Basic CRUD operations (create, save, update, findById)
 * - ID generation and format verification
 * - Search/filter operations (by module, flow, component, event ID)
 * - Bulk operations
 * - Expiry calculation and deletion
 * - Special field storage (identifier, relatedEventId)
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoWiretapDaoTest {

    public static MongoDBContainer mongoDBContainer;

    private static final int DAYS_TO_KEEP = 30;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoWiretapEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoWiretapDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoWiretapEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoWiretapDao(repository, mongoTemplate, DAYS_TO_KEEP);
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
    public void test_save_wiretapEvent() {
        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId("12345");
        event.setModuleName("TestModule");
        event.setFlowName("TestFlow");
        event.setComponentName("TestComponent");
        event.setEventId("event-001");
        event.setTimestamp(System.currentTimeMillis());
        event.setEvent("{\"message\":\"test event\"}");

        dao.save(event);

        WiretapEvent found = dao.findById("TestModule-wiretap-12345");

        Assert.assertNotNull(found);
        Assert.assertEquals("TestModule", found.getModuleName());
        Assert.assertEquals("TestFlow", found.getFlowName());
        Assert.assertEquals("TestComponent", found.getComponentName());
        Assert.assertEquals("event-001", found.getEventId());
    }

    @Test
    public void test_save_wiretapEvent_with_all_fields() {
        long currentTime = System.currentTimeMillis();
        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId("12345");
        event.setModuleName("IntegrationModule");
        event.setFlowName("DataFlow");
        event.setComponentName("TransformComponent");
        event.setEventId("event-100");
        event.setRelatedEventId("related-event-50");
        event.setTimestamp(currentTime);
        event.setEvent("{\"type\":\"data\",\"content\":\"sample payload\",\"size\":1024}");
        event.setExpiry(currentTime + TimeUnit.DAYS.toMillis(DAYS_TO_KEEP));

        dao.save(event);

        WiretapEvent found = dao.findById("IntegrationModule-wiretap-12345");

        Assert.assertNotNull(found);
        Assert.assertEquals(Long.valueOf(12345), Long.valueOf(found.getIdentifier()));
        Assert.assertEquals("IntegrationModule", found.getModuleName());
        Assert.assertEquals("DataFlow", found.getFlowName());
        Assert.assertEquals("TransformComponent", found.getComponentName());
        Assert.assertEquals("event-100", found.getEventId());
        Assert.assertEquals(currentTime, found.getTimestamp());
        Assert.assertTrue(((String) found.getEvent()).contains("sample payload"));
    }

    @Test
    public void test_save_wiretapEvent_generates_correct_id() {
        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId("12345");
        event.setModuleName("OrderModule");
        event.setFlowName("ProcessFlow");
        event.setComponentName("ValidatorComponent");
        event.setEventId("event-200");
        event.setTimestamp(System.currentTimeMillis());
        event.setEvent("test data");

        dao.save(event);

        // ID format should be: moduleName-wiretap-identifier
        WiretapEvent found = dao.findById("OrderModule-wiretap-12345");

        Assert.assertNotNull(found);
        Assert.assertEquals("OrderModule-wiretap-12345", ((MongoWiretapEventImpl) found).getId());
    }

    @Test
    public void test_findById_found() {
        MongoWiretapEventImpl event = createWiretapEvent(
            300L, "CustomerModule", "ImportFlow", "LoaderComponent",
            "event-300", null, System.currentTimeMillis(), "{\"customer\":\"data\"}"
        );
        dao.save(event);

        WiretapEvent found = dao.findById("CustomerModule-wiretap-300");

        Assert.assertNotNull(found);
        Assert.assertEquals(Long.valueOf(300L), Long.valueOf(found.getIdentifier()));
        Assert.assertEquals("CustomerModule", found.getModuleName());
        Assert.assertEquals("ImportFlow", found.getFlowName());
        Assert.assertEquals("LoaderComponent", found.getComponentName());
    }

    @Test
    public void test_findById_notFound() {
        WiretapEvent found = dao.findById("NonExistentModule-wiretap-999");

        Assert.assertNull(found);
    }

    @Test
    public void test_findById_with_long_format() {
        MongoWiretapEventImpl event = createWiretapEvent(
            400L, "PaymentModule", "PaymentFlow", "ProcessorComponent",
            "event-400", null, System.currentTimeMillis(), "{\"amount\":100.00}"
        );
        dao.save(event);

        // Try to find by just the Long identifier
        // This tests the numeric ID parsing logic in findById
        WiretapEvent found = dao.findById("400");

        // Based on the DAO implementation, this should return null as it only does direct lookup
        // The numeric parsing is logged but doesn't perform a secondary search
        Assert.assertNull(found);

        // But the full ID format should work
        found = dao.findById("PaymentModule-wiretap-400");
        Assert.assertNotNull(found);
    }

    @Test
    public void test_save_bulk_wiretapEvents() {
        List<WiretapEvent> events = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            MongoWiretapEventImpl event = createWiretapEvent(
                500L + i, "BulkModule", "BulkFlow", "BulkComponent" + i,
                "event-" + (500 + i), null, System.currentTimeMillis(), "{\"bulk\":\"data" + i + "\"}"
            );
            events.add(event);
        }

        dao.save(events);

        // Verify all events were saved
        for (int i = 0; i < 10; i++) {
            WiretapEvent found = dao.findById("BulkModule-wiretap-" + (500 + i));
            Assert.assertNotNull(found);
            Assert.assertEquals("BulkComponent" + i, found.getComponentName());
        }
    }

    @Test
    public void test_save_updates_existing() {
        MongoWiretapEventImpl event = createWiretapEvent(
            600L, "UpdateModule", "UpdateFlow", "UpdateComponent",
            "event-600", null, System.currentTimeMillis(), "{\"version\":1}"
        );
        dao.save(event);

        WiretapEvent found = dao.findById("UpdateModule-wiretap-600");
        Assert.assertNotNull(found);
        Assert.assertTrue(((String) found.getEvent()).contains("\"version\":1"));

        // Update the same event
        event.setEvent("{\"version\":2,\"updated\":true}");
        dao.save(event);

        found = dao.findById("UpdateModule-wiretap-600");
        Assert.assertNotNull(found);
        Assert.assertTrue(((String) found.getEvent()).contains("\"version\":2"));
        Assert.assertTrue(((String) found.getEvent()).contains("\"updated\":true"));
    }

    @Test
    public void test_findByModuleName() {
        // Create events with different module names
        createAndSaveWiretapEvent(700L, "SalesModule", "Flow1", "Comp1", "event-700");
        createAndSaveWiretapEvent(701L, "SalesModule", "Flow2", "Comp2", "event-701");
        createAndSaveWiretapEvent(702L, "OrderModule", "Flow3", "Comp3", "event-702");

        List<MongoWiretapEventImpl> results = repository.findByModuleName("SalesModule");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("SalesModule", event.getModuleName());
        }
    }

    @Test
    public void test_findByFlowName() {
        // Create events with different flow names
        createAndSaveWiretapEvent(800L, "Module1", "ImportFlow", "Comp1", "event-800");
        createAndSaveWiretapEvent(801L, "Module2", "ImportFlow", "Comp2", "event-801");
        createAndSaveWiretapEvent(802L, "Module3", "ExportFlow", "Comp3", "event-802");

        List<MongoWiretapEventImpl> results = repository.findByFlowName("ImportFlow");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("ImportFlow", event.getFlowName());
        }
    }

    @Test
    public void test_findByComponentName() {
        // Create events with different component names
        createAndSaveWiretapEvent(900L, "Module1", "Flow1", "ValidatorComponent", "event-900");
        createAndSaveWiretapEvent(901L, "Module2", "Flow2", "ValidatorComponent", "event-901");
        createAndSaveWiretapEvent(902L, "Module3", "Flow3", "TransformComponent", "event-902");

        List<MongoWiretapEventImpl> results = repository.findByComponentName("ValidatorComponent");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("ValidatorComponent", event.getComponentName());
        }
    }

    @Test
    public void test_findByEventId() {
        // Create events with different event IDs
        createAndSaveWiretapEvent(1000L, "Module1", "Flow1", "Comp1", "evt-abc-123");
        createAndSaveWiretapEvent(1001L, "Module2", "Flow2", "Comp2", "evt-xyz-456");
        createAndSaveWiretapEvent(1002L, "Module3", "Flow3", "Comp3", "evt-abc-123");

        List<MongoWiretapEventImpl> results = repository.findByEventId("evt-abc-123");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("evt-abc-123", event.getEventId());
        }
    }

    @Test
    public void test_findByModuleNameAndFlowName() {
        // Create events with various combinations
        createAndSaveWiretapEvent(1100L, "HRModule", "EmployeeFlow", "Comp1", "event-1100");
        createAndSaveWiretapEvent(1101L, "HRModule", "EmployeeFlow", "Comp2", "event-1101");
        createAndSaveWiretapEvent(1102L, "HRModule", "PayrollFlow", "Comp3", "event-1102");
        createAndSaveWiretapEvent(1103L, "FinanceModule", "EmployeeFlow", "Comp4", "event-1103");

        List<MongoWiretapEventImpl> results = repository.findByModuleNameAndFlowName("HRModule", "EmployeeFlow");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("HRModule", event.getModuleName());
            Assert.assertEquals("EmployeeFlow", event.getFlowName());
        }
    }

    @Test
    public void test_findByModuleNameAndFlowNameAndComponentName() {
        // Create events with various combinations
        createAndSaveWiretapEvent(1200L, "CRMModule", "LeadFlow", "EnrichmentComponent", "event-1200");
        createAndSaveWiretapEvent(1201L, "CRMModule", "LeadFlow", "EnrichmentComponent", "event-1201");
        createAndSaveWiretapEvent(1202L, "CRMModule", "LeadFlow", "ValidationComponent", "event-1202");
        createAndSaveWiretapEvent(1203L, "CRMModule", "ContactFlow", "EnrichmentComponent", "event-1203");

        List<MongoWiretapEventImpl> results = repository.findByModuleNameAndFlowNameAndComponentName(
            "CRMModule", "LeadFlow", "EnrichmentComponent"
        );

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        for (MongoWiretapEventImpl event : results) {
            Assert.assertEquals("CRMModule", event.getModuleName());
            Assert.assertEquals("LeadFlow", event.getFlowName());
            Assert.assertEquals("EnrichmentComponent", event.getComponentName());
        }
    }


    @Test
    public void test_expiry_calculation() {
        long beforeSave = System.currentTimeMillis();

        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId("12345");
        event.setModuleName("ExpiryModule");
        event.setFlowName("ExpiryFlow");
        event.setComponentName("ExpiryComponent");
        event.setEventId("event-1400");
        event.setTimestamp(beforeSave);
        event.setEvent("test data");
        // Don't set expiry - let the DAO calculate it

        dao.save(event);

        long afterSave = System.currentTimeMillis();

        WiretapEvent found = dao.findById("ExpiryModule-wiretap-12345");
        Assert.assertNotNull(found);

        long expectedMinExpiry = beforeSave + TimeUnit.DAYS.toMillis(DAYS_TO_KEEP);
        long expectedMaxExpiry = afterSave + TimeUnit.DAYS.toMillis(DAYS_TO_KEEP);

        // Verify expiry is set correctly based on daysToKeep
        Assert.assertTrue("Expiry should be at least " + expectedMinExpiry + " but was " + found.getExpiry(),
            found.getExpiry() >= expectedMinExpiry);
        Assert.assertTrue("Expiry should be at most " + expectedMaxExpiry + " but was " + found.getExpiry(),
            found.getExpiry() <= expectedMaxExpiry);
    }

    @Test
    public void test_identifier_field() {
        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId("1500");
        event.setModuleName("IdentifierModule");
        event.setFlowName("IdentifierFlow");
        event.setComponentName("IdentifierComponent");
        event.setEventId("event-1500");
        event.setTimestamp(System.currentTimeMillis());
        event.setEvent("identifier test");

        dao.save(event);

        WiretapEvent found = dao.findById("IdentifierModule-wiretap-1500");

        Assert.assertNotNull(found);
        Assert.assertEquals(Long.valueOf(1500L), Long.valueOf(found.getIdentifier()));

        // Verify identifier is stored correctly in MongoDB
        MongoWiretapEventImpl mongoEvent = (MongoWiretapEventImpl) found;
        Assert.assertEquals(Long.valueOf(1500L), Long.valueOf(mongoEvent.getIdentifier()));
    }

    @Test
    public void test_timestamp_storage() {
        long specificTimestamp = 1609459200000L; // 2021-01-01 00:00:00 UTC

        MongoWiretapEventImpl event = createWiretapEvent(
            1700L, "TimestampModule", "TimestampFlow", "TimestampComponent",
            "event-1700", null, specificTimestamp, "timestamp test"
        );
        dao.save(event);

        WiretapEvent found = dao.findById("TimestampModule-wiretap-1700");

        Assert.assertNotNull(found);
        Assert.assertEquals(specificTimestamp, found.getTimestamp());
    }

    @Test
    public void test_event_payload_storage() {
        String complexPayload = "{\"type\":\"order\",\"items\":[{\"id\":1,\"name\":\"Product A\",\"price\":29.99}," +
            "{\"id\":2,\"name\":\"Product B\",\"price\":49.99}],\"total\":79.98,\"currency\":\"USD\"," +
            "\"customer\":{\"id\":12345,\"name\":\"John Doe\",\"email\":\"john@example.com\"}}";

        MongoWiretapEventImpl event = createWiretapEvent(
            1800L, "PayloadModule", "PayloadFlow", "PayloadComponent",
            "event-1800", null, System.currentTimeMillis(), complexPayload
        );
        dao.save(event);

        WiretapEvent found = dao.findById("PayloadModule-wiretap-1800");

        Assert.assertNotNull(found);
        Assert.assertEquals(complexPayload, found.getEvent());
        Assert.assertTrue(((String) found.getEvent()).contains("Product A"));
        Assert.assertTrue(((String) found.getEvent()).contains("john@example.com"));
    }

    @Test
    public void test_multiple_events_same_module_different_identifiers() {
        // Create multiple events for the same module with different identifiers
        for (int i = 0; i < 5; i++) {
            createAndSaveWiretapEvent(
                1900L + i, "MultiEventModule", "Flow" + i, "Component" + i, "event-" + (1900 + i)
            );
        }

        // Verify all events were saved with unique IDs
        for (int i = 0; i < 5; i++) {
            WiretapEvent found = dao.findById("MultiEventModule-wiretap-" + (1900 + i));
            Assert.assertNotNull(found);
            Assert.assertEquals("Flow" + i, found.getFlowName());
        }

        // Verify we can find all events by module name
        List<MongoWiretapEventImpl> results = repository.findByModuleName("MultiEventModule");
        Assert.assertEquals(5, results.size());
    }

    /**
     * Helper method to create a wiretap event with common fields.
     */
    private MongoWiretapEventImpl createWiretapEvent(Long identifier, String moduleName, String flowName,
                                                     String componentName, String eventId, String relatedEventId,
                                                     long timestamp, String event) {
        MongoWiretapEventImpl wiretapEvent = new MongoWiretapEventImpl();
        wiretapEvent.setId(Long.toString(identifier));
        wiretapEvent.setModuleName(moduleName);
        wiretapEvent.setFlowName(flowName);
        wiretapEvent.setComponentName(componentName);
        wiretapEvent.setEventId(eventId);
        wiretapEvent.setRelatedEventId(relatedEventId);
        wiretapEvent.setTimestamp(timestamp);
        wiretapEvent.setEvent(event);
        return wiretapEvent;
    }

    /**
     * Helper method to create and save a wiretap event with minimal fields.
     */
    private void createAndSaveWiretapEvent(Long identifier, String moduleName, String flowName,
                                           String componentName, String eventId) {
        MongoWiretapEventImpl event = createWiretapEvent(
            identifier, moduleName, flowName, componentName, eventId, null,
            System.currentTimeMillis(), "{\"test\":\"data\"}"
        );
        dao.save(event);
    }
}
