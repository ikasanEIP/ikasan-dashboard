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

    @Test
    public void test_search_by_identifiers_success() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(100L);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(200L);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(300L);
        service.saveOrUpdate(doc3);

        MongoIkasanDocument doc4 = createTestDocument("id-4", "wiretap", "module1", "flow1");
        doc4.setTimeStamp(400L);
        service.saveOrUpdate(doc4);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1", "id-3"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());
        assertEquals("Total count should be 2", 2, results.getTotalNumberOfResults());

        Set<String> foundIds = new HashSet<>();
        results.getResultList().forEach(doc -> foundIds.add(doc.getId()));
        assertTrue("Should contain id-1", foundIds.contains("id-1"));
        assertTrue("Should contain id-3", foundIds.contains("id-3"));
    }

    @Test
    public void test_search_by_identifiers_with_offset() {
        for (int i = 1; i <= 10; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp((long) i * 100);
            service.saveOrUpdate(doc);
        }

        Set<String> identifiers = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            identifiers.add("id-" + i);
        }

        IkasanDocumentSearchResults results = service.search(identifiers, 3, 5, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 5 results", 5, results.getResultList().size());
        assertEquals("Total count should be 10", 10, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_by_identifiers_with_result_size() {
        for (int i = 1; i <= 10; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp((long) i * 100);
            service.saveOrUpdate(doc);
        }

        Set<String> identifiers = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            identifiers.add("id-" + i);
        }

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 3, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 3 results", 3, results.getResultList().size());
        assertEquals("Total count should be 10", 10, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_by_identifiers_sort_ascending() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(300L);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(100L);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(200L);
        service.saveOrUpdate(doc3);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1", "id-2", "id-3"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, "timestamp", "ASCENDING");

        assertEquals("Should have 3 results", 3, results.getResultList().size());
        assertEquals("First should be id-2", "id-2", results.getResultList().get(0).getId());
        assertEquals("Second should be id-3", "id-3", results.getResultList().get(1).getId());
        assertEquals("Third should be id-1", "id-1", results.getResultList().get(2).getId());
    }

    @Test
    public void test_search_by_identifiers_sort_descending() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(300L);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(100L);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(200L);
        service.saveOrUpdate(doc3);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1", "id-2", "id-3"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, "timestamp", "DESCENDING");

        assertEquals("Should have 3 results", 3, results.getResultList().size());
        assertEquals("First should be id-1", "id-1", results.getResultList().get(0).getId());
        assertEquals("Second should be id-3", "id-3", results.getResultList().get(1).getId());
        assertEquals("Third should be id-2", "id-2", results.getResultList().get(2).getId());
    }

    @Test
    public void test_search_by_identifiers_sort_default() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setTimeStamp(100L);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setTimeStamp(200L);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setTimeStamp(300L);
        service.saveOrUpdate(doc3);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1", "id-2", "id-3"));

        // When sort field and order are null, should default to timestamp descending
        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertEquals("Should have 3 results", 3, results.getResultList().size());
        // Should be sorted by timestamp descending by default
        assertEquals("First should be id-3", "id-3", results.getResultList().get(0).getId());
        assertEquals("Second should be id-2", "id-2", results.getResultList().get(1).getId());
        assertEquals("Third should be id-1", "id-1", results.getResultList().get(2).getId());
    }

    @Test
    public void test_search_by_identifiers_null_identifiers() {
        IkasanDocumentSearchResults results = service.search(null, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 0 results", 0, results.getResultList().size());
        assertEquals("Total count should be 0", 0, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_by_identifiers_empty_identifiers() {
        Set<String> identifiers = new HashSet<>();
        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 0 results", 0, results.getResultList().size());
        assertEquals("Total count should be 0", 0, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_by_identifiers_no_matching_ids() {
        MongoIkasanDocument doc = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-999", "id-888"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 0 results", 0, results.getResultList().size());
        assertEquals("Total count should be 0", 0, results.getTotalNumberOfResults());
    }

    @Test
    public void test_search_by_identifiers_single_identifier() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc2);

        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Should be id-1", "id-1", results.getResultList().get(0).getId());
    }

    @Test
    public void test_search_by_identifiers_with_pagination() {
        for (int i = 1; i <= 20; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp((long) i * 100);
            service.saveOrUpdate(doc);
        }

        Set<String> identifiers = new HashSet<>();
        for (int i = 1; i <= 20; i++) {
            identifiers.add("id-" + i);
        }

        // First page
        IkasanDocumentSearchResults page1 = service.search(identifiers, 0, 5, "timestamp", "ASCENDING");
        assertEquals("Page 1 should have 5 results", 5, page1.getResultList().size());
        assertEquals("Total count should be 20", 20, page1.getTotalNumberOfResults());

        // Second page
        IkasanDocumentSearchResults page2 = service.search(identifiers, 5, 5, "timestamp", "ASCENDING");
        assertEquals("Page 2 should have 5 results", 5, page2.getResultList().size());
        assertEquals("Total count should be 20", 20, page2.getTotalNumberOfResults());

        // Third page
        IkasanDocumentSearchResults page3 = service.search(identifiers, 10, 5, "timestamp", "ASCENDING");
        assertEquals("Page 3 should have 5 results", 5, page3.getResultList().size());
        assertEquals("Total count should be 20", 20, page3.getTotalNumberOfResults());

        // Last page
        IkasanDocumentSearchResults page4 = service.search(identifiers, 15, 5, "timestamp", "ASCENDING");
        assertEquals("Page 4 should have 5 results", 5, page4.getResultList().size());
        assertEquals("Total count should be 20", 20, page4.getTotalNumberOfResults());

        // Verify no overlap between pages
        assertEquals("Page 1 first should be id-1", "id-1", page1.getResultList().get(0).getId());
        assertEquals("Page 2 first should be id-6", "id-6", page2.getResultList().get(0).getId());
        assertEquals("Page 3 first should be id-11", "id-11", page3.getResultList().get(0).getId());
        assertEquals("Page 4 first should be id-16", "id-16", page4.getResultList().get(0).getId());
    }

    @Test
    public void test_search_by_identifiers_with_different_types() {
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "error", "module1", "flow1");
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "replay", "module1", "flow1");
        service.saveOrUpdate(doc3);

        // Search for all three, regardless of type
        Set<String> identifiers = new HashSet<>(Arrays.asList("id-1", "id-2", "id-3"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 10, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 3 results", 3, results.getResultList().size());

        Set<String> foundTypes = new HashSet<>();
        results.getResultList().forEach(doc -> foundTypes.add(doc.getType()));
        assertTrue("Should contain wiretap type", foundTypes.contains("wiretap"));
        assertTrue("Should contain error type", foundTypes.contains("error"));
        assertTrue("Should contain replay type", foundTypes.contains("replay"));
    }

    @Test
    public void test_search_by_identifiers_large_result_set() {
        // Create 50 documents
        for (int i = 1; i <= 50; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp((long) i * 100);
            service.saveOrUpdate(doc);
        }

        Set<String> identifiers = new HashSet<>();
        for (int i = 1; i <= 50; i++) {
            identifiers.add("id-" + i);
        }

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 100, null, null);

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 50 results", 50, results.getResultList().size());
        assertEquals("Total count should be 50", 50, results.getTotalNumberOfResults());
        assertTrue("Query time should be recorded", results.getQueryResponseTime() >= 0);
    }

    @Test
    public void test_search_by_identifiers_partial_match() {
        // Create 15 documents
        for (int i = 1; i <= 15; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setTimeStamp((long) i * 100);
            service.saveOrUpdate(doc);
        }

        // Search for only 5 of them
        Set<String> identifiers = new HashSet<>(Arrays.asList("id-3", "id-7", "id-9", "id-12", "id-15"));

        IkasanDocumentSearchResults results = service.search(identifiers, 0, 20, "timestamp", "ASCENDING");

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 5 results", 5, results.getResultList().size());
        assertEquals("Total count should be 5", 5, results.getTotalNumberOfResults());

        // Verify correct documents returned in order
        assertEquals("First should be id-3", "id-3", results.getResultList().get(0).getId());
        assertEquals("Second should be id-7", "id-7", results.getResultList().get(1).getId());
        assertEquals("Third should be id-9", "id-9", results.getResultList().get(2).getId());
        assertEquals("Fourth should be id-12", "id-12", results.getResultList().get(3).getId());
        assertEquals("Fifth should be id-15", "id-15", results.getResultList().get(4).getId());
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_module_and_flow() {
        long now = System.currentTimeMillis();

        // Create test documents with harvest received timestamps
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow2");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module2", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000);
        service.saveOrUpdate(doc3);

        // Search with filters
        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1"));
        Set<String> flowNames = new HashSet<>(Arrays.asList("flow1"));

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            moduleNames, flowNames, null, null, null, now - 1000, now + 10000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Module should match", "module1", results.getResultList().get(0).getModuleName());
        assertEquals("Flow should match", "flow1", results.getResultList().get(0).getFlowName());
    }

    @Test
    public void test_searchByHarvestReceivedTime_time_range() {
        long now = System.currentTimeMillis();

        // Create documents with different harvest timestamps
        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now - 5000); // Before range
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setHarvestReceivedTimestamp(now); // In range
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000); // In range
        service.saveOrUpdate(doc3);

        MongoIkasanDocument doc4 = createTestDocument("id-4", "wiretap", "module1", "flow1");
        doc4.setHarvestReceivedTimestamp(now + 10000); // After range
        service.saveOrUpdate(doc4);

        // Search within specific time range
        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now - 1000, now + 5000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());

        Set<String> foundIds = new HashSet<>();
        results.getResultList().forEach(doc -> foundIds.add(doc.getId()));
        assertTrue("Should contain id-2", foundIds.contains("id-2"));
        assertTrue("Should contain id-3", foundIds.contains("id-3"));
        assertFalse("Should not contain id-1", foundIds.contains("id-1"));
        assertFalse("Should not contain id-4", foundIds.contains("id-4"));
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_entity_types() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "error", "module1", "flow1");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "replay", "module1", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000);
        service.saveOrUpdate(doc3);

        List<String> entityTypes = Arrays.asList("wiretap", "error");

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now - 1000, now + 10000,
            0, 10, entityTypes, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());

        Set<String> foundTypes = new HashSet<>();
        results.getResultList().forEach(doc -> foundTypes.add(doc.getType()));
        assertTrue("Should contain wiretap type", foundTypes.contains("wiretap"));
        assertTrue("Should contain error type", foundTypes.contains("error"));
        assertFalse("Should not contain replay type", foundTypes.contains("replay"));
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_offset_and_result_size() {
        long now = System.currentTimeMillis();

        // Create 5 documents
        for (int i = 0; i < 5; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setHarvestReceivedTimestamp(now + (i * 1000));
            service.saveOrUpdate(doc);
        }

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now - 1000, now + 10000,
            2, 2, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());
        assertEquals("Total count should be 5", 5, results.getTotalNumberOfResults());
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_component_names() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setComponentName("component1");
        doc1.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setComponentName("component2");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        Set<String> componentNames = new HashSet<>(Arrays.asList("component1"));

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, componentNames, null, null, now - 1000, now + 10000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Component should match", "component1", results.getResultList().get(0).getComponentName());
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_event_id() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setEventId("event-123");
        doc1.setHarvestReceivedTimestamp(100);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setEventId("event-456");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, "event-123", null, 0, 200,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 1 result", 1, results.getResultList().size());
        assertEquals("Event ID should match", "event-123", results.getResultList().get(0).getEventId());
    }


    @Test
    public void test_searchByHarvestReceivedTime_sort_ascending() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now + 3000);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000);
        service.saveOrUpdate(doc3);

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now, now + 10000,
            0, 10, null, false, "harvestReceivedTimestamp", "ASCENDING"
        );

        assertEquals("Should have 3 results", 3, results.getResultList().size());
        assertEquals("First should be id-2", "id-2", results.getResultList().get(0).getId());
        assertEquals("Second should be id-3", "id-3", results.getResultList().get(1).getId());
        assertEquals("Third should be id-1", "id-1", results.getResultList().get(2).getId());
    }

    @Test
    public void test_searchByHarvestReceivedTime_sort_descending() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module1", "flow1");
        doc2.setHarvestReceivedTimestamp(now + 3000);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module1", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000);
        service.saveOrUpdate(doc3);

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now, now + 10000,
            0, 10, null, false, "harvestReceivedTimestamp", "DESCENDING"
        );

        assertEquals("Should have 3 results", 3, results.getResultList().size());
        assertEquals("First should be id-2", "id-2", results.getResultList().get(0).getId());
        assertEquals("Second should be id-3", "id-3", results.getResultList().get(1).getId());
        assertEquals("Third should be id-1", "id-1", results.getResultList().get(2).getId());
    }

    @Test
    public void test_searchByHarvestReceivedTime_no_results_outside_time_range() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc.setHarvestReceivedTimestamp(now - 10000);
        service.saveOrUpdate(doc);

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now, now + 5000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 0 results", 0, results.getResultList().size());
        assertEquals("Total count should be 0", 0, results.getTotalNumberOfResults());
    }

    @Test
    public void test_searchByHarvestReceivedTime_multiple_modules() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "wiretap", "module2", "flow1");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        MongoIkasanDocument doc3 = createTestDocument("id-3", "wiretap", "module3", "flow1");
        doc3.setHarvestReceivedTimestamp(now + 2000);
        service.saveOrUpdate(doc3);

        Set<String> moduleNames = new HashSet<>(Arrays.asList("module1", "module2"));

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            moduleNames, null, null, null, null, now - 1000, now + 10000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());

        Set<String> foundModules = new HashSet<>();
        results.getResultList().forEach(doc -> foundModules.add(doc.getModuleName()));
        assertTrue("Should contain module1", foundModules.contains("module1"));
        assertTrue("Should contain module2", foundModules.contains("module2"));
        assertFalse("Should not contain module3", foundModules.contains("module3"));
    }

    @Test
    public void test_searchByHarvestReceivedTime_with_pagination() {
        long now = System.currentTimeMillis();

        // Create 20 documents
        for (int i = 1; i <= 20; i++) {
            MongoIkasanDocument doc = createTestDocument("id-" + i, "wiretap", "module1", "flow1");
            doc.setHarvestReceivedTimestamp(now + (i * 100));
            service.saveOrUpdate(doc);
        }

        // First page
        IkasanDocumentSearchResults page1 = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now, now + 10000,
            0, 5, null, false, "harvestReceivedTimestamp", "ASCENDING"
        );
        assertEquals("Page 1 should have 5 results", 5, page1.getResultList().size());
        assertEquals("Total count should be 20", 20, page1.getTotalNumberOfResults());

        // Second page
        IkasanDocumentSearchResults page2 = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now, now + 10000,
            5, 5, null, false, "harvestReceivedTimestamp", "ASCENDING"
        );
        assertEquals("Page 2 should have 5 results", 5, page2.getResultList().size());
        assertEquals("Total count should be 20", 20, page2.getTotalNumberOfResults());

        // Verify no overlap between pages
        assertEquals("Page 1 first should be id-1", "id-1", page1.getResultList().get(0).getId());
        assertEquals("Page 2 first should be id-6", "id-6", page2.getResultList().get(0).getId());
    }

    @Test
    public void test_searchByHarvestReceivedTime_empty_filters() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc1 = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc1.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc1);

        MongoIkasanDocument doc2 = createTestDocument("id-2", "error", "module2", "flow2");
        doc2.setHarvestReceivedTimestamp(now + 1000);
        service.saveOrUpdate(doc2);

        // Search with no filters - should return all documents in time range
        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now - 1000, now + 10000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertEquals("Should have 2 results", 2, results.getResultList().size());
    }

    @Test
    public void test_searchByHarvestReceivedTime_query_response_time_recorded() {
        long now = System.currentTimeMillis();

        MongoIkasanDocument doc = createTestDocument("id-1", "wiretap", "module1", "flow1");
        doc.setHarvestReceivedTimestamp(now);
        service.saveOrUpdate(doc);

        IkasanDocumentSearchResults results = service.searchByHarvestReceivedTime(
            null, null, null, null, null, now - 1000, now + 10000,
            0, 10, null, false, null, null
        );

        assertNotNull("Results should not be null", results);
        assertTrue("Query response time should be recorded", results.getQueryResponseTime() >= 0);
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
