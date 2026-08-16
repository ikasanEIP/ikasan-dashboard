package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocumentSearchResults;
import org.ikasan.mongo.persistence.general.repository.MongoIkasanDocumentRepository;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test class for MongoGeneralDaoImpl.
 */
public class MongoGeneralDaoImplTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
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

    @Test
    public void test_saveOrUpdate_and_findById() {
        MongoIkasanDocument document = createTestDocument("test-id-1", "wiretap", "module1", "flow1");

        dao.saveOrUpdate(document);

        MongoIkasanDocument found = dao.findById("wiretap", "test-id-1");
        assertNotNull("Document should be found", found);
        assertEquals("ID should match", "test-id-1", found.getId());
        assertEquals("Type should match", "wiretap", found.getType());
        assertEquals("Module name should match", "module1", found.getModuleName());
        assertEquals("Flow name should match", "flow1", found.getFlowName());
    }

    @Test
    public void test_search_with_time_range() {
        long now = System.currentTimeMillis();
        long oneHourAgo = now - 3600000;
        long twoHoursAgo = now - 7200000;

        // Create documents with different timestamps
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(twoHoursAgo);
        dao.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(oneHourAgo);
        dao.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(now);
        dao.saveOrUpdate(doc3);

        // Search for documents in last hour
        IkasanDocumentSearchResults results = dao.search(null, oneHourAgo, now, 10, null, false, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());
        assertTrue("Total count should be 2", results.getTotalNumberOfResults() == 2);
    }

    @Test
    public void test_search_with_module_and_flow_filters() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        dao.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow2");
        dao.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module2", "flow1");
        dao.saveOrUpdate(doc3);

        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1"));
        Set<String> flowNames = new HashSet<>(Arrays.asList("flow1"));

        IkasanDocumentSearchResults results = dao.search(moduleNames, flowNames, null, 0, System.currentTimeMillis(), 10, false, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Module should match", "module1", results.getResultList().get(0).getModuleName());
        assertEquals("Flow should match", "flow1", results.getResultList().get(0).getFlowName());
    }

    @Test
    public void test_search_with_pagination() {
        // Create 5 documents
        for (int i = 0; i < 5; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp(System.currentTimeMillis() + i);
            dao.saveOrUpdate(doc);
        }

        // Get first page (2 results)
        IkasanDocumentSearchResults page1 = dao.search(null, 0, System.currentTimeMillis() + 10000, 0, 2, null, false, null, null);

        assertNotNull("Page 1 should not be null", page1);
        assertEquals("Page 1 should have 2 results", 2, page1.getResultList().size());
        assertEquals("Total count should be 5", 5, page1.getTotalNumberOfResults());

        // Get second page (2 results)
        IkasanDocumentSearchResults page2 = dao.search(null, 0, System.currentTimeMillis() + 10000, 2, 2, null, false, null, null);

        assertNotNull("Page 2 should not be null", page2);
        assertEquals("Page 2 should have 2 results", 2, page2.getResultList().size());
        assertEquals("Total count should be 5", 5, page2.getTotalNumberOfResults());
    }

    @Test
    public void test_search_with_sorting() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(1000);
        dao.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(3000);
        dao.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(2000);
        dao.saveOrUpdate(doc3);

        // Sort ascending
        IkasanDocumentSearchResults ascResults = dao.search(null, 0, System.currentTimeMillis(), 10, null, false, "timestamp", "ASCENDING");

        assertEquals("First document should have timestamp 1000", 1000, ascResults.getResultList().get(0).getTimeStamp());
        assertEquals("Last document should have timestamp 3000", 3000, ascResults.getResultList().get(2).getTimeStamp());

        // Sort descending
        IkasanDocumentSearchResults descResults = dao.search(null, 0, System.currentTimeMillis(), 10, null, false, "timestamp", "DESCENDING");

        assertEquals("First document should have timestamp 3000", 3000, descResults.getResultList().get(0).getTimeStamp());
        assertEquals("Last document should have timestamp 1000", 1000, descResults.getResultList().get(2).getTimeStamp());
    }

    @Test
    public void test_findByErrorUri() {
        MongoIkasanDocument doc = createTestDocument("id-1", "error", "module1", "flow1");
        doc.setErrorUri("error://test/uri/123");
        dao.saveOrUpdate(doc);

        MongoIkasanDocument found = dao.findByErrorUri("error", "error://test/uri/123");

        assertNotNull("Document should be found", found);
        assertEquals("Error URI should match", "error://test/uri/123", found.getErrorUri());
    }

    @Test
    public void test_removeById() {
        MongoIkasanDocument doc = createTestDocument("id-to-delete", "wiretap", "module1", "flow1");
        dao.saveOrUpdate(doc);

        // Verify it exists
        MongoIkasanDocument found = dao.findById("wiretap", "id-to-delete");
        assertNotNull("Document should exist before delete", found);

        // Delete it
        dao.removeById("wiretap", "id-to-delete");

        // Verify it's deleted
        MongoIkasanDocument notFound = dao.findById("wiretap", "id-to-delete");
        assertNull("Document should not exist after delete", notFound);
    }

    @Test
    public void test_removeExpired() {
        long now = System.currentTimeMillis();
        long past = now - 10000;
        long future = now + 10000;

        // Create expired document
        MongoIkasanDocument expiredDoc = createTestDocument("expired-id", "wiretap", "module1", "flow1");
        expiredDoc.setExpiry(past);
        dao.saveOrUpdate(expiredDoc);

        // Create non-expired document
        MongoIkasanDocument validDoc = createTestDocument("valid-id", "wiretap", "module1", "flow1");
        validDoc.setExpiry(future);
        dao.saveOrUpdate(validDoc);

        // Remove expired
        dao.removeExpired();

        // Verify expired is gone
        MongoIkasanDocument expiredFound = dao.findById("wiretap", "expired-id");
        assertNull("Expired document should be deleted", expiredFound);

        // Verify valid still exists
        MongoIkasanDocument validFound = dao.findById("wiretap", "valid-id");
        assertNotNull("Valid document should still exist", validFound);
    }

    @Test
    public void test_saveOrUpdate_list() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow2");
        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module2", "flow1");

        dao.saveOrUpdate(Arrays.asList(doc1, doc2, doc3));

        IkasanDocumentSearchResults results = dao.search(null, 0, System.currentTimeMillis(), 10, null, false, null, null);

        assertEquals("Should have 3 documents", 3, results.getTotalNumberOfResults());
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
