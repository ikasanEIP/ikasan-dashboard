package org.ikasan.mongo.persistence.replay.dao;

import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEventImpl;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoReplayAuditDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoReplayAuditDaoImplTest {

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

    private MongoReplayAuditDaoImpl dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoReplayAuditEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoReplayAuditDaoImpl(repository, mongoTemplate, 7); // 7 days to keep
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
        MongoReplayAuditEventImpl auditEvent = createAuditEvent("audit-1", true, "Success message");

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
        Awaitility.await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertEquals(3, repository.count()));
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

        MongoReplayAuditEventImpl batchEvent = repository.findAll().get(0);
        assertNotNull(batchEvent.getReplayAuditJson());
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-1"));
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-2"));
        assertTrue(batchEvent.getReplayAuditJson().contains("audit-3"));
    }

    @Test
    public void testAuditEventFieldsPreserved() {
        // Given
        MongoReplayAuditEventImpl auditEvent = createAuditEvent("test-audit", true, "Test message");

        dao.save(auditEvent);

        // When
        List<MongoReplayAuditEventImpl> results = repository.findAll();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        MongoReplayAuditEventImpl retrieved = results.get(0);
        assertTrue(retrieved.getReplayAuditJson().contains("Test message"));
        assertTrue(retrieved.getTimestamp() > 0);
        assertTrue(retrieved.getExpiry() > 0);
    }

    @Test
    public void testExpiryCalculation() {
        // Given - DAO configured with 7 days to keep
        MongoReplayAuditEventImpl auditEvent = createAuditEvent("test-audit", true, "Test message");

        // When
        dao.save(auditEvent);

        // Then
        List<MongoReplayAuditEventImpl> results = repository.findAll();
        MongoReplayAuditEventImpl retrieved = results.get(0);

        long expectedExpiry = retrieved.getTimestamp() + (7 * 24 * 60 * 60 * 1000);
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

        List<MongoReplayAuditEventImpl> allEvents = repository.findAll();
        assertTrue(allEvents.stream().anyMatch(e -> e.getReplayAuditJson().contains("batch1-1")));
        assertTrue(allEvents.stream().anyMatch(e -> e.getReplayAuditJson().contains("batch2-1")));
    }

    private MongoReplayAuditEventImpl createAuditEvent(String id, boolean success, String resultMessage) {
        MongoReplayAuditEventImpl auditEvent = new MongoReplayAuditEventImpl();
        auditEvent.setId(id);
        auditEvent.setSuccess(success);
        auditEvent.setResultMessage(resultMessage);
        auditEvent.setTimestamp(System.currentTimeMillis());
        return auditEvent;
    }
}
