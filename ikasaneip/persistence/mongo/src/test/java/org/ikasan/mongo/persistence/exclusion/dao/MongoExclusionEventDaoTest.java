package org.ikasan.mongo.persistence.exclusion.dao;

import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.exclusion.repository.MongoExclusionEventRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.exclusion.ExclusionEvent;
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
 * Integration test for MongoExclusionEventDao using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoExclusionEventDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoExclusionEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoExclusionEventDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoExclusionEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoExclusionEventDao(repository, mongoTemplate);
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
    public void testSaveSingleExclusionEvent() {
        // Given
        MongoExclusionEvent exclusionEvent = createExclusionEvent(
                "module1", "flow1", "event-id-1", "event-payload-1", "error-uri-1"
        );

        // When
        dao.save(exclusionEvent);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleExclusionEvents() {
        // Given
        List<ExclusionEvent> exclusionEvents = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            exclusionEvents.add(createExclusionEvent(
                    "module" + i, "flow" + i, "event-id-" + i, "event-payload-" + i, "error-uri-" + i
            ));
        }

        // When
        dao.save(exclusionEvents);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testFindByErrorUri() {
        // Given
        MongoExclusionEvent exclusionEvent = createExclusionEvent(
                "module1", "flow1", "event-id-1", "event-payload-1", "error-uri-123"
        );
        dao.save(exclusionEvent);

        // When
        ExclusionEvent found = dao.findByErrorUri("error-uri-123");

        // Then
        assertNotNull(found);
        assertEquals("error-uri-123", found.getErrorUri());
        assertEquals("module1", found.getModuleName());
        assertEquals("flow1", found.getFlowName());
        assertEquals("event-id-1", found.getIdentifier());
    }

    @Test
    public void testFindByErrorUriNotFound() {
        // When
        ExclusionEvent result = dao.findByErrorUri("non-existent-uri");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindByModuleName() {
        // Given
        dao.save(createExclusionEvent("module1", "flow1", "id1", "payload1", "uri1"));
        dao.save(createExclusionEvent("module1", "flow2", "id2", "payload2", "uri2"));
        dao.save(createExclusionEvent("module2", "flow3", "id3", "payload3", "uri3"));

        // When
        List<ExclusionEvent> results = dao.findByModuleName("module1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(event -> assertEquals("module1", event.getModuleName()));
    }

    @Test
    public void testFindByModuleNameAndFlowName() {
        // Given
        dao.save(createExclusionEvent("module1", "flow1", "id1", "payload1", "uri1"));
        dao.save(createExclusionEvent("module1", "flow1", "id2", "payload2", "uri2"));
        dao.save(createExclusionEvent("module1", "flow2", "id3", "payload3", "uri3"));

        // When
        List<ExclusionEvent> results = dao.findByModuleNameAndFlowName("module1", "flow1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(event -> {
            assertEquals("module1", event.getModuleName());
            assertEquals("flow1", event.getFlowName());
        });
    }

    @Test
    public void testFindWithModuleNameFilter() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createExclusionEvent("module" + (i % 2), "flow" + i, "id" + i, "payload" + i, "uri" + i));
        }

        // When
        List<ExclusionEvent> results = dao.find(Arrays.asList("module1"), null, null, 0, 0, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(event -> assertEquals("module1", event.getModuleName()));
    }

    @Test
    public void testFindWithSearchString() {
        // Given
        dao.save(createExclusionEvent("module1", "flow1", "id1", "timeout error payload", "uri1"));
        dao.save(createExclusionEvent("module2", "flow2", "id2", "database error payload", "uri2"));
        dao.save(createExclusionEvent("module3", "flow3", "id3", "timeout network payload", "uri3"));

        // When
        List<ExclusionEvent> results = dao.find(null, null, "timeout", 0, 0, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithTimeRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        MongoExclusionEvent event1 = createExclusionEvent("module1", "flow1", "id1", "payload1", "uri1");
        event1.setTimestamp(baseTime - (2 * oneDayMillis));
        dao.save(event1);

        MongoExclusionEvent event2 = createExclusionEvent("module2", "flow2", "id2", "payload2", "uri2");
        event2.setTimestamp(baseTime);
        dao.save(event2);

        MongoExclusionEvent event3 = createExclusionEvent("module3", "flow3", "id3", "payload3", "uri3");
        event3.setTimestamp(baseTime + oneDayMillis);
        dao.save(event3);

        // When - Find events from yesterday to tomorrow
        List<ExclusionEvent> results = dao.find(null, null, null, baseTime - oneDayMillis, baseTime + (2 * oneDayMillis), 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithPagination() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createExclusionEvent("module" + i, "flow" + i, "id" + i, "payload" + i, "uri" + i));
        }

        // When
        List<ExclusionEvent> page1 = dao.find(null, null, null, 0, 0, 0, 5);
        List<ExclusionEvent> page2 = dao.find(null, null, null, 0, 0, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testDeleteByErrorUri() {
        // Given
        MongoExclusionEvent exclusionEvent = createExclusionEvent(
                "module1", "flow1", "event-id-1", "event-payload", "error-uri-delete"
        );
        dao.save(exclusionEvent);
        assertEquals(1, repository.count());

        // When
        dao.deleteByErrorUri("error-uri-delete");

        // Then
        assertEquals(0, repository.count());
    }

    @Test
    public void testRemoveExpired() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        MongoExclusionEvent expiredEvent = createExclusionEvent("module1", "flow1", "id1", "payload1", "uri1");
        expiredEvent.setExpiry(oneDayAgo); // Already expired
        dao.save(expiredEvent);

        MongoExclusionEvent validEvent = createExclusionEvent("module2", "flow2", "id2", "payload2", "uri2");
        validEvent.setExpiry(currentTime + (24 * 60 * 60 * 1000)); // Expires tomorrow
        dao.save(validEvent);

        assertEquals(2, repository.count());

        // When
        dao.removeExpired();

        // Then
        assertEquals(1, repository.count());
        ExclusionEvent remaining = dao.findByErrorUri("uri2");
        assertNotNull(remaining);
    }

    @Test
    public void testExclusionEventUsesMongoModel() {
        // Given
        MongoExclusionEvent exclusionEvent = createExclusionEvent(
                "module1", "flow1", "event-id-1", "event-payload-1", "error-uri-1"
        );
        dao.save(exclusionEvent);

        // When
        ExclusionEvent retrieved = dao.findByErrorUri("error-uri-1");

        // Then
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MongoExclusionEvent);

        MongoExclusionEvent mongoExclusion = (MongoExclusionEvent) retrieved;
        assertEquals("module1", mongoExclusion.getModuleName());
        assertEquals("flow1", mongoExclusion.getFlowName());
        assertEquals("event-id-1", mongoExclusion.getIdentifier());
        assertEquals("event-payload-1", mongoExclusion.getEventAsString());
        assertEquals("error-uri-1", mongoExclusion.getErrorUri());
    }

    @Test
    public void testEventAsStringPreserved() {
        // Given
        String eventPayload = "Large event payload with complex data";
        MongoExclusionEvent exclusionEvent = createExclusionEvent(
                "module1", "flow1", "event-id-1", eventPayload, "error-uri-1"
        );
        dao.save(exclusionEvent);

        // When
        ExclusionEvent retrieved = dao.findByErrorUri("error-uri-1");

        // Then
        assertNotNull(retrieved);
        assertEquals(eventPayload, new String(retrieved.getEvent()));
        assertEquals(eventPayload, ((MongoExclusionEvent) retrieved).getEventAsString());
    }

    private MongoExclusionEvent createExclusionEvent(String moduleName, String flowName,
                                                     String identifier, String event, String errorUri) {
        MongoExclusionEvent exclusionEvent = new MongoExclusionEvent(
                moduleName, flowName, identifier, event, System.currentTimeMillis(), errorUri
        );
        exclusionEvent.setExpiry(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days from now
        exclusionEvent.setHarvested(false);
        return exclusionEvent;
    }
}
