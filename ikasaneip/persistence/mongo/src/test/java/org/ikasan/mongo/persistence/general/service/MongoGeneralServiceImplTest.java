package org.ikasan.mongo.persistence.general.service;

import com.mongodb.client.MongoClients;
import org.ikasan.mongo.persistence.general.dao.MongoGeneralDaoImpl;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocumentSearchResults;
import org.ikasan.mongo.persistence.general.repository.MongoIkasanDocumentRepository;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test class for MongoGeneralServiceImpl.
 */
public class MongoGeneralServiceImplTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoGeneralServiceImpl service;
    private MongoGeneralDaoImpl dao;
    private MongoIkasanDocumentRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoIkasanDocumentRepository.class);

        dao = new MongoGeneralDaoImpl(repository, mongoTemplate);
        service = new MongoGeneralServiceImpl(dao);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoIkasanDocument.class);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_dao_throws_exception() {
        new MongoGeneralServiceImpl(null);
    }

    @Test
    public void test_search_with_module_and_flow() {
        // Create test documents
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow2");
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module2", "flow1");
        service.saveOrUpdate(doc3);

        // Search with filters
        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1"));
        Set<String> flowNames = new HashSet<>(Arrays.asList("flow1"));

        IkasanDocumentSearchResults results = service.search(
            moduleNames, flowNames, null, 0, System.currentTimeMillis(), 10, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Module should match", "module1", results.getResultList().get(0).getModuleName());
        assertEquals("Flowimpl should match", "flow1", results.getResultList().get(0).getFlowName());
    }

    @Test
    public void test_search_with_entity_types() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "error", "module1", "flow1");
        service.saveOrUpdate(doc2);

        List<String> entityTypes = Arrays.asList("wiretap");

        IkasanDocumentSearchResults results = service.search(
            null, 0, System.currentTimeMillis(), 10, entityTypes, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Type should be wiretap", "wiretap", results.getResultList().get(0).getType());
    }

    @Test
    public void test_search_with_offset_and_result_size() {
        // Create 5 documents
        for (int i = 0; i < 5; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp(System.currentTimeMillis() + i);
            service.saveOrUpdate(doc);
        }

        IkasanDocumentSearchResults results = service.search(
            null, 0, System.currentTimeMillis() + 10000, 2, 2, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());
        assertEquals("Total count should be 5", 5, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_with_module_only() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module2", "flow1");
        service.saveOrUpdate(doc2);

        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1"));

        IkasanDocumentSearchResults results = service.search(
            moduleNames, null, 0, System.currentTimeMillis(), 0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Module should be module1", "module1", results.getResultList().get(0).getModuleName());
    }

    @Test
    public void test_search_with_all_parameters() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc.setComponentName("component1");
        doc.setEventId("event-123");
        doc.setTimeStamp(now);
        service.saveOrUpdate(doc);

        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1"));
        Set<String> flowNames = new HashSet<>(Arrays.asList("flow1"));
        Set<String> componentNames = new HashSet<>(Arrays.asList("component1"));

        // Search without eventId parameter to test other filters
        IkasanDocumentSearchResults results = service.search(
            moduleNames, flowNames, componentNames, null, null,
            0, now + 10000, 0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Component should match", "component1", results.getResultList().get(0).getComponentName());
    }

    @Test
    public void test_findById() {
        MongoIkasanDocument doc = createTestDocument("test-id", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc);

        IkasanESBDocument found = service.findById("wiretap", "test-id");

        assertNotNull("Document should be found", found);
        assertEquals("ID should match", "test-id", found.getId());
        assertEquals("Type should match", "wiretap", found.getType());
    }

    @Test
    public void test_findByErrorUri() {
        MongoIkasanDocument doc = createTestDocument("id-1", "error", "module1", "flow1");
        doc.setErrorUri("error://test/uri/123");
        service.saveOrUpdate(doc);

        IkasanESBDocument found = service.findByErrorUri("error", "error://test/uri/123");

        assertNotNull("Document should be found", found);
        assertEquals("Error URI should match", "error://test/uri/123", found.getErrorUri());
    }

    @Test
    public void test_saveOrUpdate_single_document() {
        MongoIkasanDocument doc = createTestDocument("id-1", "wiretap", "module1", "flow1");

        service.saveOrUpdate(doc);

        IkasanESBDocument found = service.findById("wiretap", "id-1");
        assertNotNull("Document should be saved", found);
    }

    @Test
    public void test_saveOrUpdate_multiple_documents() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow2");
        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module2", "flow1");

        service.saveOrUpdate(Arrays.asList(doc1, doc2, doc3));

        IkasanDocumentSearchResults results = service.search(
            null, 0, System.currentTimeMillis(), 10, null, false, null, null
        );

        assertEquals("Should have 3 documents", 3, results.getTotalNumberOfResults());
    }

    @Test
    public void test_housekeep() {
        long now = System.currentTimeMillis();
        long past = now - 10000;
        long future = now + 10000;

        // Create expired document
        MongoIkasanDocument expiredDoc = createTestDocument("expired-id", "wiretap", "module1", "flow1");
        expiredDoc.setExpiry(past);
        service.saveOrUpdate(expiredDoc);

        // Create non-expired document
        MongoIkasanDocument validDoc = createTestDocument("valid-id", "wiretap", "module1", "flow1");
        validDoc.setExpiry(future);
        service.saveOrUpdate(validDoc);

        // Run housekeeping
        service.housekeep();

        // Verify expired is gone
        IkasanESBDocument expiredFound = service.findById("wiretap", "expired-id");
        assertNull("Expired document should be deleted", expiredFound);

        // Verify valid still exists
        IkasanESBDocument validFound = service.findById("wiretap", "valid-id");
        assertNotNull("Valid document should still exist", validFound);
    }

    @Test
    public void test_housekeepablesExist() {
        assertTrue("Housekeepables should exist", service.housekeepablesExist());
    }

    @Test
    public void test_setHousekeepingBatchSize() {
        // Should not throw exception
        service.setHousekeepingBatchSize(100);
    }

    @Test
    public void test_setTransactionBatchSize() {
        // Should not throw exception
        service.setTransactionBatchSize(100);
    }

    @Test
    public void test_removeById() {
        MongoIkasanDocument doc = createTestDocument("id-to-delete", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc);

        // Verify it exists
        IkasanESBDocument found = service.findById("wiretap", "id-to-delete");
        assertNotNull("Document should exist before delete", found);

        // Delete it
        service.removeById("wiretap", "id-to-delete");

        // Verify it's deleted
        IkasanESBDocument notFound = service.findById("wiretap", "id-to-delete");
        assertNull("Document should not exist after delete", notFound);
    }

    private MongoIkasanDocument createTestDocument(String id, String type, String moduleName, String flowName) {
        MongoIkasanDocument doc = new MongoIkasanDocument();
        doc.setId(id);
        doc.setType(type);
        doc.setModuleName(moduleName);
        doc.setFlowName(flowName);
        doc.setComponentName("testComponent");
        doc.setTimeStamp(System.currentTimeMillis());
        doc.setExpiry(System.currentTimeMillis() + 86400000); // 24 hours
        doc.setEventId("event-" + id);
        doc.setEvent("Test event payload");
        return doc;
    }
}
