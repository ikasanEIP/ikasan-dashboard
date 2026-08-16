package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayEvent;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayEventRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.replay.ReplayEvent;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoReplayDao using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoReplayDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoReplayEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoReplayDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoReplayEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoReplayDao(repository, mongoTemplate);
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
    public void testSaveSingleReplayEvent() {
        // Given
        MongoReplayEvent replayEvent = createReplayEvent(
                "module1", "flow1", "event-id-1", "payload-1".getBytes(), "payload-1"
        );

        // When
        dao.save(replayEvent);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleReplayEvents() {
        // Given
        List<ReplayEvent> replayEvents = Arrays.asList(
                createReplayEvent("module1", "flow1", "event-id-1", "payload-1".getBytes(), "payload-1"),
                createReplayEvent("module2", "flow2", "event-id-2", "payload-2".getBytes(), "payload-2"),
                createReplayEvent("module3", "flow3", "event-id-3", "payload-3".getBytes(), "payload-3")
        );

        // When
        dao.save(replayEvents);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testFindWithModuleNameFilter() {
        // Given
        dao.save(createReplayEvent("module1", "flow1", "event-id-1", "payload-1".getBytes(), "payload-1"));
        dao.save(createReplayEvent("module1", "flow2", "event-id-2", "payload-2".getBytes(), "payload-2"));
        dao.save(createReplayEvent("module2", "flow3", "event-id-3", "payload-3".getBytes(), "payload-3"));

        // When
        List<ReplayEvent> results = dao.find(
                Arrays.asList("module1"),
                null,
                null,
                0,
                0,
                0,
                100
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(event -> assertEquals("module1", event.getModuleName()));
    }

    @Test
    public void testFindWithFlowNameFilter() {
        // Given
        dao.save(createReplayEvent("module1", "flow1", "event-id-1", "payload-1".getBytes(), "payload-1"));
        dao.save(createReplayEvent("module2", "flow1", "event-id-2", "payload-2".getBytes(), "payload-2"));
        dao.save(createReplayEvent("module3", "flow2", "event-id-3", "payload-3".getBytes(), "payload-3"));

        // When
        List<ReplayEvent> results = dao.find(
                null,
                Arrays.asList("flow1"),
                null,
                0,
                0,
                0,
                100
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(event -> assertEquals("flow1", event.getFlowName()));
    }

    @Test
    public void testFindWithSearchString() {
        // Given
        dao.save(createReplayEvent("module1", "flow1", "event-id-1", "payload-1".getBytes(), "special-payload"));
        dao.save(createReplayEvent("module2", "flow2", "event-id-2", "payload-2".getBytes(), "normal-payload"));
        dao.save(createReplayEvent("module3", "flow3", "special-id", "payload-3".getBytes(), "payload-3"));

        // When
        List<ReplayEvent> results = dao.find(
                null,
                null,
                "special",
                0,
                0,
                0,
                100
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithTimestampRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        MongoReplayEvent event1 = createReplayEvent("module1", "flow1", "event-id-1", "payload-1".getBytes(), "payload-1");
        event1.setTimestamp(baseTime);
        dao.save(event1);

        MongoReplayEvent event2 = createReplayEvent("module2", "flow2", "event-id-2", "payload-2".getBytes(), "payload-2");
        event2.setTimestamp(baseTime + 5000);
        dao.save(event2);

        MongoReplayEvent event3 = createReplayEvent("module3", "flow3", "event-id-3", "payload-3".getBytes(), "payload-3");
        event3.setTimestamp(baseTime + 10000);
        dao.save(event3);

        // When
        List<ReplayEvent> results = dao.find(
                null,
                null,
                null,
                baseTime,
                baseTime + 7000,
                0,
                100
        );

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithPagination() {
        // Given
        for (int i = 0; i < 10; i++) {
            dao.save(createReplayEvent("module" + i, "flow" + i, "event-id-" + i,
                    ("payload-" + i).getBytes(), "payload-" + i));
        }

        // When
        List<ReplayEvent> page1 = dao.find(null, null, null, 0, 0, 0, 5);
        List<ReplayEvent> page2 = dao.find(null, null, null, 0, 0, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testDelete() {
        // Given
        MongoReplayEvent replayEvent = createReplayEvent("module1", "flow1", "event-id-1",
                "payload-1".getBytes(), "payload-1");
        dao.save(replayEvent);
        assertEquals(1, repository.count());

        // When
        dao.delete(replayEvent.getIdAsString());

        // Then
        assertEquals(0, repository.count());
    }

    @Test
    public void testDeleteExpired() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        MongoReplayEvent expiredEvent = createReplayEvent("module1", "flow1", "event-id-1",
                "payload-1".getBytes(), "payload-1");
        expiredEvent.setExpiry(oneDayAgo);
        dao.save(expiredEvent);

        MongoReplayEvent validEvent = createReplayEvent("module2", "flow2", "event-id-2",
                "payload-2".getBytes(), "payload-2");
        validEvent.setExpiry(currentTime + (24 * 60 * 60 * 1000));
        dao.save(validEvent);

        assertEquals(2, repository.count());

        // When
        dao.deleteExpired();

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testEventFieldsPreserved() {
        // Given
        byte[] eventBytes = "test-payload".getBytes();
        MongoReplayEvent replayEvent = createReplayEvent("testModule", "testFlow", "test-event-id",
                eventBytes, "test-payload-string");
        replayEvent.setRelatedEventIdentifier("related-event-123");

        dao.save(replayEvent);

        // When
        List<ReplayEvent> results = dao.find(
                Arrays.asList("testModule"),
                Arrays.asList("testFlow"),
                null,
                0,
                0,
                0,
                100
        );

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        ReplayEvent retrieved = results.get(0);
        assertEquals("testModule", retrieved.getModuleName());
        assertEquals("testFlow", retrieved.getFlowName());
        assertEquals("test-event-id", retrieved.getEventId());
        assertArrayEquals(eventBytes, retrieved.getEvent());
        assertEquals("test-payload-string", retrieved.getEventAsString());
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getExpiry() > 0);

        // Also check MongoDB-specific field if instance is MongoReplayEvent
        if (retrieved instanceof MongoReplayEvent) {
            assertEquals("related-event-123", ((MongoReplayEvent) retrieved).getRelatedEventIdentifier());
        }
    }

    private MongoReplayEvent createReplayEvent(String moduleName, String flowName, String eventId,
                                               byte[] event, String eventAsString) {
        MongoReplayEvent replayEvent = new MongoReplayEvent();
        replayEvent.setModuleName(moduleName);
        replayEvent.setFlowName(flowName);
        replayEvent.setEventId(eventId);
        replayEvent.setEvent(event);
        replayEvent.setEventAsString(eventAsString);
        replayEvent.setTimestamp(System.currentTimeMillis());
        replayEvent.setExpiry(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days
        return replayEvent;
    }
}
