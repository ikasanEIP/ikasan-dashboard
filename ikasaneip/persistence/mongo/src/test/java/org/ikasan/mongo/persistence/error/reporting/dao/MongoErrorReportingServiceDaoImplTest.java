package org.ikasan.mongo.persistence.error.reporting.dao;

import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.error.reporting.repository.MongoErrorOccurrenceRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.junit.After;
import org.junit.AfterClass;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoErrorReportingServiceDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoErrorReportingServiceDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoErrorOccurrenceRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoErrorReportingServiceDaoImpl dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoErrorOccurrenceRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoErrorReportingServiceDaoImpl(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void testSaveSingleErrorOccurrence() {
        // Given
        ErrorOccurrence errorOccurrence = createErrorOccurrence(
            "error-uri-1", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", "event-payload", System.currentTimeMillis()
        );

        // When
        dao.save(errorOccurrence);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleErrorOccurrences() {
        // Given
        List<ErrorOccurrence> errorOccurrences = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            errorOccurrences.add(createErrorOccurrence(
                "error-uri-" + i, "module" + i, "flow" + i, "component" + i,
                "Retry", "Error detail " + i, "Error message " + i, "java.lang.Exception",
                "event-id-" + i, "related-event-" + i, "event-payload-" + i, System.currentTimeMillis()
            ));
        }

        // When
        dao.save(errorOccurrences);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testFindByUri() {
        // Given
        ErrorOccurrence errorOccurrence = createErrorOccurrence(
            "error-uri-123", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", "event-payload", System.currentTimeMillis()
        );
        dao.save(errorOccurrence);

        // When
        ErrorOccurrence found = dao.findByUri("error-uri-123");

        // Then
        assertNotNull(found);
        assertEquals("error-uri-123", found.getUri());
        assertEquals("module1", found.getModuleName());
        assertEquals("flow1", found.getFlowName());
        assertEquals("component1", found.getFlowElementName());
        assertEquals("Error message", found.getErrorMessage());
    }

    @Test
    public void testFindByUriNotFound() {
        // When
        ErrorOccurrence result = dao.findByUri("non-existent-uri");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindByModuleName() {
        // Given
        dao.save(createErrorOccurrence("error-1", "module1", "flow1", "component1",
            "Retry", "Detail 1", "Message 1", "Exception1", "event-1", "rel-1", "payload-1", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-2", "module1", "flow2", "component2",
            "Retry", "Detail 2", "Message 2", "Exception2", "event-2", "rel-2", "payload-2", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-3", "module2", "flow3", "component3",
            "Retry", "Detail 3", "Message 3", "Exception3", "event-3", "rel-3", "payload-3", System.currentTimeMillis()));

        // When
        List<ErrorOccurrence> results = dao.findByModuleName("module1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(error -> assertEquals("module1", error.getModuleName()));
    }

    @Test
    public void testFindByModuleNameAndFlowName() {
        // Given
        dao.save(createErrorOccurrence("error-1", "module1", "flow1", "component1",
            "Retry", "Detail 1", "Message 1", "Exception1", "event-1", "rel-1", "payload-1", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-2", "module1", "flow1", "component2",
            "Retry", "Detail 2", "Message 2", "Exception2", "event-2", "rel-2", "payload-2", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-3", "module1", "flow2", "component3",
            "Retry", "Detail 3", "Message 3", "Exception3", "event-3", "rel-3", "payload-3", System.currentTimeMillis()));

        // When
        List<ErrorOccurrence> results = dao.findByModuleNameAndFlowName("module1", "flow1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(error -> {
            assertEquals("module1", error.getModuleName());
            assertEquals("flow1", error.getFlowName());
        });
    }

    @Test
    public void testFindWithModuleNameFilter() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createErrorOccurrence("error-" + i, "module" + (i % 2), "flow" + i, "component" + i,
                "Retry", "Detail " + i, "Message " + i, "Exception" + i, "event-" + i, "rel-" + i, "payload-" + i, System.currentTimeMillis()));
        }

        // When
        List<ErrorOccurrence> results = dao.find(
            Arrays.asList("module1"), null, null, null, 0, 0, 0, 10
        );

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(error -> assertEquals("module1", error.getModuleName()));
    }

    @Test
    public void testFindWithSearchString() {
        // Given
        dao.save(createErrorOccurrence("error-1", "module1", "flow1", "component1",
            "Retry", "Connection timeout error", "Timeout message", "TimeoutException", "event-1", "rel-1", "payload-1", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-2", "module2", "flow2", "component2",
            "Retry", "Database error", "DB connection failed", "SQLException", "event-2", "rel-2", "payload-2", System.currentTimeMillis()));
        dao.save(createErrorOccurrence("error-3", "module3", "flow3", "component3",
            "Retry", "Network timeout error", "Network message", "NetworkException", "event-3", "rel-3", "payload-3", System.currentTimeMillis()));

        // When
        List<ErrorOccurrence> results = dao.find(
            null, null, null, "timeout", 0, 0, 0, 10
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithTimeRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        dao.save(createErrorOccurrence("error-1", "module1", "flow1", "component1",
            "Retry", "Detail 1", "Message 1", "Exception1", "event-1", "rel-1", "payload-1", baseTime - (2 * oneDayMillis)));
        dao.save(createErrorOccurrence("error-2", "module2", "flow2", "component2",
            "Retry", "Detail 2", "Message 2", "Exception2", "event-2", "rel-2", "payload-2", baseTime));
        dao.save(createErrorOccurrence("error-3", "module3", "flow3", "component3",
            "Retry", "Detail 3", "Message 3", "Exception3", "event-3", "rel-3", "payload-3", baseTime + oneDayMillis));

        // When - Find errors from yesterday to tomorrow
        List<ErrorOccurrence> results = dao.find(
            null, null, null, null, baseTime - oneDayMillis, baseTime + (2 * oneDayMillis), 0, 10
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithPagination() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createErrorOccurrence("error-" + i, "module" + i, "flow" + i, "component" + i,
                "Retry", "Detail " + i, "Message " + i, "Exception" + i, "event-" + i, "rel-" + i, "payload-" + i, System.currentTimeMillis()));
        }

        // When
        List<ErrorOccurrence> page1 = dao.find(null, null, null, null, 0, 0, 0, 5);
        List<ErrorOccurrence> page2 = dao.find(null, null, null, null, 0, 0, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testDeleteByUri() {
        // Given
        ErrorOccurrence errorOccurrence = createErrorOccurrence(
            "error-to-delete", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", "event-payload", System.currentTimeMillis()
        );
        dao.save(errorOccurrence);
        assertEquals(1, repository.count());

        // When
        dao.deleteByUri("error-to-delete");

        // Then
        assertEquals(0, repository.count());
    }

    @Test
    public void testRemoveExpired() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        ErrorOccurrence expiredError = createErrorOccurrence(
            "expired-error", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", "event-payload", currentTime
        );
        expiredError.setExpiry(oneDayAgo); // Already expired

        ErrorOccurrence validError = createErrorOccurrence(
            "valid-error", "module2", "flow2", "component2",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-2", "related-event-2", "event-payload", currentTime
        );
        validError.setExpiry(currentTime + (24 * 60 * 60 * 1000)); // Expires tomorrow

        dao.save(expiredError);
        dao.save(validError);
        assertEquals(2, repository.count());

        // When
        dao.removeExpired();

        // Then
        assertEquals(1, repository.count());
        ErrorOccurrence remaining = dao.findByUri("valid-error");
        assertNotNull(remaining);
    }

    @Test
    public void testErrorOccurrenceUsesMongoModel() {
        // Given
        ErrorOccurrence errorOccurrence = createErrorOccurrence(
            "error-uri-1", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", "event-payload", System.currentTimeMillis()
        );

        dao.save(errorOccurrence);

        // When
        ErrorOccurrence retrieved = dao.findByUri("error-uri-1");

        // Then
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MongoErrorOccurrence);

        MongoErrorOccurrence mongoError = (MongoErrorOccurrence) retrieved;
        assertEquals("error-uri-1", mongoError.getUri());
        assertEquals("module1", mongoError.getModuleName());
        assertEquals("flow1", mongoError.getFlowName());
        assertEquals("component1", mongoError.getFlowElementName());
        assertEquals("Retry", mongoError.getAction());
        assertEquals("Error detail", mongoError.getErrorDetail());
        assertEquals("Error message", mongoError.getErrorMessage());
        assertEquals("java.lang.Exception", mongoError.getExceptionClass());
        assertEquals("event-id-1", mongoError.getEventLifeIdentifier());
        assertEquals("related-event-1", mongoError.getEventRelatedIdentifier());
        assertEquals("event-payload", mongoError.getEventAsString());
    }

    @Test
    public void testEventAsStringPreserved() {
        // Given
        String eventPayload = "Large event payload with complex data structure";
        ErrorOccurrence errorOccurrence = createErrorOccurrence(
            "error-uri-1", "module1", "flow1", "component1",
            "Retry", "Error detail", "Error message", "java.lang.Exception",
            "event-id-1", "related-event-1", eventPayload, System.currentTimeMillis()
        );

        dao.save(errorOccurrence);

        // When
        ErrorOccurrence retrieved = dao.findByUri("error-uri-1");

        // Then
        assertNotNull(retrieved);
        assertEquals(eventPayload, new String((byte[])retrieved.getEvent()));
        assertEquals(eventPayload, retrieved.getEventAsString());
    }

    private ErrorOccurrence createErrorOccurrence(String uri, String moduleName, String flowName,
                                                       String flowElementName, String action, String errorDetail,
                                                       String errorMessage, String exceptionClass,
                                                       String eventLifeIdentifier, String eventRelatedIdentifier,
                                                       String eventAsString, long timestamp) {
        MongoErrorOccurrence errorOccurrence = new MongoErrorOccurrence(
            uri, moduleName, flowName, flowElementName, action,
            errorDetail, errorMessage, exceptionClass, eventLifeIdentifier,
            eventRelatedIdentifier, eventAsString, timestamp
        );
        errorOccurrence.setExpiry(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days from now
        return errorOccurrence;
    }
}
