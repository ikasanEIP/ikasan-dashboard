package org.ikasan.mongo.persistence.hospital.dao;

import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.hospital.repository.MongoExclusionEventActionRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
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
 * Integration test for MongoHospitalDao using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoHospitalDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoExclusionEventActionRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoHospitalDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoExclusionEventActionRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoHospitalDao(repository, mongoTemplate, 7);
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
    public void testSaveSingleExclusionEventAction() {
        // Given
        MongoExclusionEventAction action = createExclusionEventAction(
                "module1", "flow1", "error-uri-1", "user1", MongoExclusionEventAction.RESUBMIT,
                "event-payload-1", "Test comment"
        );

        // When
        dao.save(action);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleExclusionEventActions() {
        // Given
        List<ExclusionEventAction> actions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            actions.add(createExclusionEventAction(
                    "module" + i, "flow" + i, "error-uri-" + i, "user" + i,
                    MongoExclusionEventAction.RESUBMIT, "event-payload-" + i, "Comment " + i
            ));
        }

        // When
        dao.save(actions);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testFindByErrorUri() {
        // Given
        MongoExclusionEventAction action = createExclusionEventAction(
                "module1", "flow1", "error-uri-123", "user1", MongoExclusionEventAction.RESUBMIT,
                "event-payload-1", "Test comment"
        );
        dao.save(action);

        // When
        ExclusionEventAction found = dao.findByErrorUri("error-uri-123");

        // Then
        assertNotNull(found);
        assertEquals("error-uri-123", found.getErrorUri());
        assertEquals("module1", found.getModuleName());
        assertEquals("flow1", found.getFlowName());
        assertEquals("user1", found.getActionedBy());
        assertEquals(MongoExclusionEventAction.RESUBMIT, found.getAction());
    }

    @Test
    public void testFindByErrorUriNotFound() {
        // When
        ExclusionEventAction result = dao.findByErrorUri("non-existent-uri");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindByModuleName() {
        // Given
        dao.save(createExclusionEventAction("module1", "flow1", "uri1", "user1", MongoExclusionEventAction.RESUBMIT, "payload1", "comment1"));
        dao.save(createExclusionEventAction("module1", "flow2", "uri2", "user2", MongoExclusionEventAction.IGNORED, "payload2", "comment2"));
        dao.save(createExclusionEventAction("module2", "flow3", "uri3", "user3", MongoExclusionEventAction.RESUBMIT, "payload3", "comment3"));

        // When
        List<ExclusionEventAction> results = dao.findByModuleName("module1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(action -> assertEquals("module1", action.getModuleName()));
    }

    @Test
    public void testFindByModuleNameAndFlowName() {
        // Given
        dao.save(createExclusionEventAction("module1", "flow1", "uri1", "user1", MongoExclusionEventAction.RESUBMIT, "payload1", "comment1"));
        dao.save(createExclusionEventAction("module1", "flow1", "uri2", "user2", MongoExclusionEventAction.IGNORED, "payload2", "comment2"));
        dao.save(createExclusionEventAction("module1", "flow2", "uri3", "user3", MongoExclusionEventAction.RESUBMIT, "payload3", "comment3"));

        // When
        List<ExclusionEventAction> results = dao.findByModuleNameAndFlowName("module1", "flow1");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(action -> {
            assertEquals("module1", action.getModuleName());
            assertEquals("flow1", action.getFlowName());
        });
    }

    @Test
    public void testFindWithModuleNameFilter() {
        // Given
        for (int i = 1; i <= 5; i++) {
            dao.save(createExclusionEventAction("module" + (i % 2), "flow" + i, "uri" + i, "user" + i,
                    MongoExclusionEventAction.RESUBMIT, "payload" + i, "comment" + i));
        }

        // When
        List<ExclusionEventAction> results = dao.find(Arrays.asList("module1"), null, null, 0, 0, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(action -> assertEquals("module1", action.getModuleName()));
    }

    @Test
    public void testFindWithSearchString() {
        // Given
        dao.save(createExclusionEventAction("module1", "flow1", "uri1", "user1", MongoExclusionEventAction.RESUBMIT, "timeout error payload", "timeout comment"));
        dao.save(createExclusionEventAction("module2", "flow2", "uri2", "user2", MongoExclusionEventAction.IGNORED, "database error payload", "db comment"));
        dao.save(createExclusionEventAction("module3", "flow3", "uri3", "user3", MongoExclusionEventAction.RESUBMIT, "timeout network payload", "network comment"));

        // When
        List<ExclusionEventAction> results = dao.find(null, null, "timeout", 0, 0, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithTimeRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        MongoExclusionEventAction action1 = createExclusionEventAction("module1", "flow1", "uri1", "user1",
                MongoExclusionEventAction.RESUBMIT, "payload1", "comment1");
        action1.setTimestamp(baseTime - (2 * oneDayMillis));
        dao.save(action1);

        MongoExclusionEventAction action2 = createExclusionEventAction("module2", "flow2", "uri2", "user2",
                MongoExclusionEventAction.IGNORED, "payload2", "comment2");
        action2.setTimestamp(baseTime);
        dao.save(action2);

        MongoExclusionEventAction action3 = createExclusionEventAction("module3", "flow3", "uri3", "user3",
                MongoExclusionEventAction.RESUBMIT, "payload3", "comment3");
        action3.setTimestamp(baseTime + oneDayMillis);
        dao.save(action3);

        // When - Find actions from yesterday to tomorrow
        List<ExclusionEventAction> results = dao.find(null, null, null, baseTime - oneDayMillis, baseTime + (2 * oneDayMillis), 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithPagination() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createExclusionEventAction("module" + i, "flow" + i, "uri" + i, "user" + i,
                    MongoExclusionEventAction.RESUBMIT, "payload" + i, "comment" + i));
        }

        // When
        List<ExclusionEventAction> page1 = dao.find(null, null, null, 0, 0, 0, 5);
        List<ExclusionEventAction> page2 = dao.find(null, null, null, 0, 0, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testDeleteByErrorUri() {
        // Given
        MongoExclusionEventAction action = createExclusionEventAction(
                "module1", "flow1", "error-uri-delete", "user1", MongoExclusionEventAction.RESUBMIT,
                "event-payload", "comment"
        );
        dao.save(action);
        assertEquals(1, repository.count());

        // When
        dao.deleteByErrorUri("error-uri-delete");

        // Then
        assertEquals(0, repository.count());
    }

    @Test
    public void testExclusionEventActionUsesMongoModel() {
        // Given
        MongoExclusionEventAction action = createExclusionEventAction(
                "module1", "flow1", "error-uri-1", "user1", MongoExclusionEventAction.RESUBMIT,
                "event-payload-1", "Test comment"
        );
        dao.save(action);

        // When
        ExclusionEventAction retrieved = dao.findByErrorUri("error-uri-1");

        // Then
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MongoExclusionEventAction);

        MongoExclusionEventAction mongoAction = (MongoExclusionEventAction) retrieved;
        assertEquals("module1", mongoAction.getModuleName());
        assertEquals("flow1", mongoAction.getFlowName());
        assertEquals("error-uri-1", mongoAction.getErrorUri());
        assertEquals("user1", mongoAction.getActionedBy());
        assertEquals(MongoExclusionEventAction.RESUBMIT, mongoAction.getAction());
        assertEquals("event-payload-1", mongoAction.getEvent());
        assertEquals("Test comment", mongoAction.getComment());
    }

    @Test
    public void testActionConstants() {
        // Test that the action constants are available
        assertEquals("re-submitted", MongoExclusionEventAction.RESUBMIT);
        assertEquals("ignored", MongoExclusionEventAction.IGNORED);
    }

    private MongoExclusionEventAction createExclusionEventAction(String moduleName, String flowName,
                                                                 String errorUri, String actionedBy,
                                                                 String action, String event, String comment) {
        MongoExclusionEventAction exclusionEventAction = new MongoExclusionEventAction(
                moduleName, flowName, errorUri, actionedBy, action, event, System.currentTimeMillis(), comment
        );
        exclusionEventAction.setExpiry(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days from now
        return exclusionEventAction;
    }
}
