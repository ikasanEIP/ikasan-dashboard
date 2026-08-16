package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEvent;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayAuditEventRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.replay.ReplayAuditEvent;
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
 * Integration test for MongoReplayAuditDao using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoReplayAuditDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoReplayAuditEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoReplayAuditDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoReplayAuditEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoReplayAuditDao(repository, mongoTemplate, 7); // 7 days to keep
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
    public void testSaveSingleReplayAuditEvent() {
        // Given
        MongoReplayAuditEvent auditEvent = createAuditEvent("audit-1", true, "Success message");

        // When
        dao.save(auditEvent);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleReplayAuditEvents() {
        // Given
        List<ReplayAuditEvent> auditEvents = Arrays.asList(
                createAuditEvent("audit-1", true, "Success 1"),
                createAuditEvent("audit-2", true, "Success 2"),
                createAuditEvent("audit-3", false, "Failed")
        );

        // When
        dao.save(auditEvents);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testBatchInsert() {
        // Given
        List<ReplayAuditEvent> auditEvents = Arrays.asList(
                createAuditEvent("audit-1", true, "Success 1"),
                createAuditEvent("audit-2", true, "Success 2"),
                createAuditEvent("audit-3", false, "Failed")
        );

        // When
        dao.insert(auditEvents);

        // Then
        // Batch insert creates a single document containing all events as JSON
        assertEquals(1, repository.count());

        MongoReplayAuditEvent batchEvent = repository.findAll().get(0);
        assertNotNull(batchEvent.getReplayAuditJson());
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-1"));
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-2"));
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-3"));
    }

    @Test
    public void testDeleteExpired() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        MongoReplayAuditEvent expiredEvent = createAuditEvent("audit-1", true, "Expired");
        expiredEvent.setExpiry(oneDayAgo);
        dao.save(expiredEvent);

        MongoReplayAuditEvent validEvent = createAuditEvent("audit-2", true, "Valid");
        validEvent.setExpiry(currentTime + (24 * 60 * 60 * 1000));
        dao.save(validEvent);

        assertEquals(2, repository.count());

        // When
        dao.deleteExpired();

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testAuditEventFieldsPreserved() {
        // Given
        MongoReplayAuditEvent auditEvent = createAuditEvent("test-audit", true, "Test message");

        dao.save(auditEvent);

        // When
        List<MongoReplayAuditEvent> results = repository.findAll();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        MongoReplayAuditEvent retrieved = results.get(0);
        assertTrue(retrieved.isSuccess());
        assertEquals("Test message", retrieved.getResultMessage());
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getExpiry() > 0);
        assertTrue(retrieved.getCreatedTimestamp() > 0);
    }

    @Test
    public void testExpiryCalculation() {
        // Given - DAO configured with 7 days to keep
        MongoReplayAuditEvent auditEvent = createAuditEvent("test-audit", true, "Test message");

        // When
        dao.save(auditEvent);

        // Then
        List<MongoReplayAuditEvent> results = repository.findAll();
        MongoReplayAuditEvent retrieved = results.get(0);

        long expectedExpiry = retrieved.getCreatedTimestamp() + (7 * 24 * 60 * 60 * 1000);
        long actualExpiry = retrieved.getExpiry();

        // Allow 1 second tolerance for test execution time
        assertTrue(Math.abs(expectedExpiry - actualExpiry) < 1000);
    }

    @Test
    public void testBatchInsertWithMultipleEvents() {
        // Given
        List<ReplayAuditEvent> batch1 = Arrays.asList(
                createAuditEvent("batch1-1", true, "Batch 1 Event 1"),
                createAuditEvent("batch1-2", true, "Batch 1 Event 2")
        );

        List<ReplayAuditEvent> batch2 = Arrays.asList(
                createAuditEvent("batch2-1", false, "Batch 2 Event 1"),
                createAuditEvent("batch2-2", false, "Batch 2 Event 2"),
                createAuditEvent("batch2-3", true, "Batch 2 Event 3")
        );

        // When
        dao.insert(batch1);
        dao.insert(batch2);

        // Then
        assertEquals(2, repository.count()); // Two batch documents

        List<MongoReplayAuditEvent> allEvents = repository.findAll();
        assertTrue(allEvents.stream().anyMatch(e -> e.getReplayAuditJson().contains("batch1-1")));
        assertTrue(allEvents.stream().anyMatch(e -> e.getReplayAuditJson().contains("batch2-1")));
    }

    private MongoReplayAuditEvent createAuditEvent(String id, boolean success, String resultMessage) {
        MongoReplayAuditEvent auditEvent = new MongoReplayAuditEvent();
        auditEvent.setId(id);
        auditEvent.setSuccess(success);
        auditEvent.setResultMessage(resultMessage);
        auditEvent.setTimestamp(System.currentTimeMillis());
        return auditEvent;
    }
}
