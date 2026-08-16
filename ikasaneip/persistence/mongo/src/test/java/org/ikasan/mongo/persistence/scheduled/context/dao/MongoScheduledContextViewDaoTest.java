package org.ikasan.mongo.persistence.scheduled.context.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextViewRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextViewRepository;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.junit.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test class for MongoScheduledContextViewDao.
 */
public class MongoScheduledContextViewDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoScheduledContextViewDao dao;
    private MongoScheduledContextViewRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoScheduledContextViewRepository.class);

        dao = new MongoScheduledContextViewDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoScheduledContextViewRecordImpl.class);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_and_get_context_view() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("{ \"view\": \"data\" }");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");

        Assert.assertNotNull(found);
        Assert.assertEquals("parentContext1", found.getParentContextName());
        Assert.assertEquals("childContext1", found.getContextName());
        Assert.assertEquals("{ \"view\": \"data\" }", found.getContextView());
        Assert.assertEquals("testUser", found.getModifiedBy());
        Assert.assertEquals(1000000L, found.getTimestamp());
    }

    @Test
    public void test_get_context_view_not_found() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("{ \"view\": \"data\" }");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("nonExistent", "childContext1");

        Assert.assertNull(found);
    }

    @Test
    public void test_update_context_view() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("{ \"view\": \"data\" }");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");
        Assert.assertEquals("{ \"view\": \"data\" }", found.getContextView());

        // Update the record
        record.setContextView("{ \"view\": \"updated data\" }");
        record.setModifiedBy("anotherUser");
        dao.saveOrUpdate(record);

        found = dao.getContextView("parentContext1", "childContext1");

        Assert.assertNotNull(found);
        Assert.assertEquals("{ \"view\": \"updated data\" }", found.getContextView());
        Assert.assertEquals("anotherUser", found.getModifiedBy());
    }

    @Test
    public void test_save_multiple_context_views() {
        for (int i = 0; i < 5; i++) {
            MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
            record.setParentContextName("parentContext" + i);
            record.setContextName("childContext" + i);
            record.setContextView("{ \"view\": \"data" + i + "\" }");
            record.setTimestamp(1000000L + i);
            record.setModifiedBy("user" + i);

            dao.saveOrUpdate(record);
        }

        // Verify each context view can be retrieved
        for (int i = 0; i < 5; i++) {
            ScheduledContextViewRecord found = dao.getContextView("parentContext" + i, "childContext" + i);

            Assert.assertNotNull(found);
            Assert.assertEquals("parentContext" + i, found.getParentContextName());
            Assert.assertEquals("childContext" + i, found.getContextName());
            Assert.assertEquals("{ \"view\": \"data" + i + "\" }", found.getContextView());
        }
    }

    @Test
    public void test_save_same_child_context_different_parents() {
        // Save same child context under different parents
        MongoScheduledContextViewRecordImpl record1 = new MongoScheduledContextViewRecordImpl();
        record1.setParentContextName("parentContext1");
        record1.setContextName("childContext");
        record1.setContextView("{ \"parent\": \"1\" }");
        record1.setTimestamp(1000000L);
        record1.setModifiedBy("user1");

        MongoScheduledContextViewRecordImpl record2 = new MongoScheduledContextViewRecordImpl();
        record2.setParentContextName("parentContext2");
        record2.setContextName("childContext");
        record2.setContextView("{ \"parent\": \"2\" }");
        record2.setTimestamp(2000000L);
        record2.setModifiedBy("user2");

        dao.saveOrUpdate(record1);
        dao.saveOrUpdate(record2);

        // Verify both can be retrieved independently
        ScheduledContextViewRecord found1 = dao.getContextView("parentContext1", "childContext");
        Assert.assertNotNull(found1);
        Assert.assertEquals("{ \"parent\": \"1\" }", found1.getContextView());

        ScheduledContextViewRecord found2 = dao.getContextView("parentContext2", "childContext");
        Assert.assertNotNull(found2);
        Assert.assertEquals("{ \"parent\": \"2\" }", found2.getContextView());
    }

    @Test
    public void test_save_with_null_modified_by() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("{ \"view\": \"data\" }");
        record.setTimestamp(1000000L);
        record.setModifiedBy(null);

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");

        Assert.assertNotNull(found);
        Assert.assertEquals("parentContext1", found.getParentContextName());
        Assert.assertEquals("childContext1", found.getContextName());
    }

    @Test
    public void test_save_with_empty_context_view() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");

        Assert.assertNotNull(found);
        Assert.assertEquals("", found.getContextView());
    }

    @Test
    public void test_save_with_complex_json_context_view() {
        String complexJson = "{ \"nodes\": [ { \"id\": \"1\", \"name\": \"Node1\" }, { \"id\": \"2\", \"name\": \"Node2\" } ], \"edges\": [ { \"from\": \"1\", \"to\": \"2\" } ] }";

        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView(complexJson);
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");

        Assert.assertNotNull(found);
        Assert.assertEquals(complexJson, found.getContextView());
    }

    @Test
    public void test_delete_context_view() {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parentContext1");
        record.setContextName("childContext1");
        record.setContextView("{ \"view\": \"data\" }");
        record.setTimestamp(1000000L);
        record.setModifiedBy("testUser");

        dao.saveOrUpdate(record);

        ScheduledContextViewRecord found = dao.getContextView("parentContext1", "childContext1");
        Assert.assertNotNull(found);

        dao.delete("parentContext1", "childContext1");

        found = dao.getContextView("parentContext1", "childContext1");
        Assert.assertNull(found);
    }

    @Test
    public void test_delete_non_existent_context_view() {
        // Should not throw exception when deleting non-existent record
        dao.delete("nonExistent", "nonExistent");

        // Verify nothing is deleted (collection should be empty)
        ScheduledContextViewRecord found = dao.getContextView("nonExistent", "nonExistent");
        Assert.assertNull(found);
    }
}
