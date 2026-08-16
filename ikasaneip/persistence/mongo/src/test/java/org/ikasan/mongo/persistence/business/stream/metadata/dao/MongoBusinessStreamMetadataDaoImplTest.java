package org.ikasan.mongo.persistence.business.stream.metadata.dao;

import org.apache.commons.io.IOUtils;
import org.ikasan.mongo.persistence.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.mongo.persistence.business.stream.metadata.model.MongoBusinessStream;
import org.ikasan.mongo.persistence.business.stream.metadata.repository.MongoBusinessStreamRepository;
import org.ikasan.mongo.persistence.module.metadata.model.MongoFlowMetaDataImpl;
import org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetaDataImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.metadata.BusinessStreamMetadataSearchResults;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.model.*;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoBusinessStreamMetadataDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoBusinessStreamMetadataDaoImplTest {

    public static final String BUSINESS_STREAM_JSON = "/data/businessStream.json";

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoBusinessStreamRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private BusinessStreamMetadataDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoBusinessStreamRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoBusinessStreamMetadataDaoImpl(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    private BusinessStreamMetaData createBusinessStreamMetaData(String id, String name, String description, String json) {
        BusinessStreamMetaData metaData = new BusinessStreamMetaDataImpl();
        metaData.setId(id);
        metaData.setName(name);
        metaData.setDescription(description);
        metaData.setJson(json);
        return metaData;
    }

    private String loadDataFile(String fileName) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + fileName);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void testSave() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "businessStream1",
            "Test business stream",
            businessStreamJson
        );

        // When
        dao.save(metaData);

        // Then
        assertEquals(1, repository.count());
        MongoBusinessStream saved = repository.findById("businessStream1").orElse(null);
        assertNotNull(saved);
        assertEquals("businessStream1", saved.getName());
        assertEquals("Test business stream", saved.getDescription());
        assertEquals(businessStreamJson, saved.getBusinessStreamMetaData());
    }

    @Test
    public void testSaveAndDelete() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "businessStream1",
            "Test business stream",
            businessStreamJson
        );

        // When
        dao.save(metaData);
        assertEquals(1, repository.count());

        dao.delete("businessStream1");

        // Then
        assertEquals(0, repository.count());
        assertNull(dao.findById("businessStream1"));
    }

    @Test
    public void testFindById() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "businessStream1",
            "Test business stream",
            businessStreamJson
        );
        dao.save(metaData);

        // When
        BusinessStreamMetaData found = dao.findById("businessStream1");

        // Then
        assertNotNull(found);
        assertEquals("businessStream1", found.getName());
        assertEquals("Test business stream", found.getDescription());
        assertEquals(businessStreamJson, found.getJson());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        BusinessStreamMetaData result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindAll() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 5; i++) {
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "businessStream" + i,
                "businessStream" + i,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When
        List<BusinessStreamMetaData> results = dao.findAll(0, 10);

        // Then
        assertNotNull(results);
        assertEquals(5, results.size());
        assertEquals("businessStream1", results.get(0).getName());
    }

    @Test
    public void testFindAllWithPagination() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 5; i++) {
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "businessStream" + i,
                "businessStream" + i,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When - Get page 2 with size 2
        List<BusinessStreamMetaData> results = dao.findAll(2, 2);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFind() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 5; i++) {
            String name = (i <= 4) ? "businessStream" + i : "otherStream";
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "id" + i,
                name,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When - Filter by business stream names
        List<String> businessStreamNames = Arrays.asList("businessStream1", "businessStream2");
        BusinessStreamMetadataSearchResults results = dao.find(businessStreamNames, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.getResultList().size());
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindWithPagination() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 5; i++) {
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "businessStream" + i,
                "businessStream" + i,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When - Get page with limit 3
        BusinessStreamMetadataSearchResults results = dao.find(null, 0, 3);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(5, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindBusinessStreamsContainingFlow() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);

        // Save business stream containing tradeSystem1-trade flows
        BusinessStreamMetaData metaData1 = createBusinessStreamMetaData(
            "businessStream1",
            "Trade Stream 1",
            "Contains tradeSystem1 flows",
            businessStreamJson
        );
        dao.save(metaData1);

        // Save business stream with different content
        BusinessStreamMetaData metaData2 = createBusinessStreamMetaData(
            "businessStream2",
            "Trade Stream 2",
            "Different content",
            "{\"flows\": []}"
        );
        dao.save(metaData2);

        // When - Search for flows from tradeSystem1-trade module
        List<BusinessStreamMetaData> results = dao.findBusinessStreamsContainingFlow(
            "tradeSystem1-trade",
            "tradeSystem1 FIX Messages Consumer Flow",
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("businessStream1", results.get(0).getId());
    }

    @Test
    public void testFindBusinessStreamsContainingFlowNotFound() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "Trade Stream 1",
            "Test",
            businessStreamJson
        );
        dao.save(metaData);

        // When
        List<BusinessStreamMetaData> results = dao.findBusinessStreamsContainingFlow(
            "nonExistentModule",
            "nonExistentFlow",
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testFindBusinessStreamsForModules() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);

        BusinessStreamMetaData metaData1 = createBusinessStreamMetaData(
            "businessStream1",
            "Trade Stream 1",
            "Contains tradeSystem1 flows",
            businessStreamJson
        );
        dao.save(metaData1);

        BusinessStreamMetaData metaData2 = createBusinessStreamMetaData(
            "businessStream2",
            "Trade Stream 2",
            "Different content",
            "{\"flows\": []}"
        );
        dao.save(metaData2);

        // Create module metadata using MongoDB model classes
        List<ModuleMetaData> modules = new ArrayList<>();
        MongoModuleMetaDataImpl module = new MongoModuleMetaDataImpl();
        module.setName("tradeSystem1-trade");

        List<FlowMetaData> flows = new ArrayList<>();
        MongoFlowMetaDataImpl flow = new MongoFlowMetaDataImpl();
        flow.setName("tradeSystem1 FIX Messages Consumer Flow");
        flows.add(flow);

        module.setFlows(flows);
        modules.add(module);

        // When
        BusinessStreamMetadataSearchResults results = dao.findBusinessStreamsForModules(
            null,
            modules,
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1, results.getTotalNumberOfResults());
        assertEquals("businessStream1", results.getResultList().get(0).getId());
    }

    @Test
    public void testFindBusinessStreamsForModulesWithFilter() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);

        BusinessStreamMetaData metaData1 = createBusinessStreamMetaData(
            "businessStream1",
            "Trade Stream 1",
            "Contains tradeSystem1 flows",
            businessStreamJson
        );
        dao.save(metaData1);

        BusinessStreamMetaData metaData2 = createBusinessStreamMetaData(
            "businessStream2",
            "Other Stream",
            "Contains tradeSystem1 flows",
            businessStreamJson
        );
        dao.save(metaData2);

        // Create module metadata using MongoDB model classes
        List<ModuleMetaData> modules = new ArrayList<>();
        MongoModuleMetaDataImpl module = new MongoModuleMetaDataImpl();
        module.setName("tradeSystem1-trade");

        List<FlowMetaData> flows = new ArrayList<>();
        MongoFlowMetaDataImpl flow = new MongoFlowMetaDataImpl();
        flow.setName("tradeSystem1 FIX Messages Consumer Flow");
        flows.add(flow);

        module.setFlows(flows);
        modules.add(module);

        // When - Filter by name containing "Trade"
        BusinessStreamMetadataSearchResults results = dao.findBusinessStreamsForModules(
            "Trade",
            modules,
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals("businessStream1", results.getResultList().get(0).getId());
    }

    @Test
    public void testFindBusinessStreamsForModulesEmpty() {
        // When - Empty module list
        BusinessStreamMetadataSearchResults results = dao.findBusinessStreamsForModules(
            null,
            new ArrayList<>(),
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindBusinessStreamsForModulesNull() {
        // When - Null module list
        BusinessStreamMetadataSearchResults results = dao.findBusinessStreamsForModules(
            null,
            null,
            0,
            10
        );

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
        assertEquals(0, results.getTotalNumberOfResults());
    }

    @Test
    public void testDeleteNonExistent() {
        // When - Delete non-existent record (should not throw exception)
        dao.delete("non-existent-id");

        // Then - No exception thrown
        assertEquals(0, repository.count());
    }

    @Test
    public void testMultipleSaves() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "businessStream1",
            "Original description",
            businessStreamJson
        );

        // When - Save initial
        dao.save(metaData);
        assertEquals(1, repository.count());

        // Update and save again
        metaData.setDescription("Updated description");
        dao.save(metaData);

        // Then
        assertEquals(1, repository.count()); // Still only one record
        BusinessStreamMetaData found = dao.findById("businessStream1");
        assertEquals("Updated description", found.getDescription());
    }

    @Test
    public void testConversionPreservesAllFields() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData original = createBusinessStreamMetaData(
            "test-id",
            "test-name",
            "test-description",
            businessStreamJson
        );

        // When
        dao.save(original);
        BusinessStreamMetaData retrieved = dao.findById("test-id");

        // Then
        assertNotNull(retrieved);
        assertEquals(original.getId(), retrieved.getId());
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.getDescription(), retrieved.getDescription());
        assertEquals(original.getJson(), retrieved.getJson());
    }

    @Test
    public void testFindWithNullBusinessStreamNames() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 3; i++) {
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "businessStream" + i,
                "businessStream" + i,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When - null business stream names should return all
        BusinessStreamMetadataSearchResults results = dao.find(null, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(3, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindWithEmptyBusinessStreamNames() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        for (int i = 1; i <= 3; i++) {
            BusinessStreamMetaData metaData = createBusinessStreamMetaData(
                "businessStream" + i,
                "businessStream" + i,
                "Description " + i,
                businessStreamJson
            );
            dao.save(metaData);
        }

        // When - empty business stream names should return all
        BusinessStreamMetadataSearchResults results = dao.find(new ArrayList<>(), 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(3, results.getTotalNumberOfResults());
    }

    @Test
    public void testBusinessStreamDeserializationUsesMongoModelEntities() throws IOException {
        // Given
        String businessStreamJson = loadDataFile(BUSINESS_STREAM_JSON);
        BusinessStreamMetaData metaData = createBusinessStreamMetaData(
            "businessStream1",
            "businessStream1",
            "Test business stream",
            businessStreamJson
        );

        // When
        dao.save(metaData);
        BusinessStreamMetaData retrieved = dao.findById("businessStream1");

        // Then - Verify getBusinessStream() deserializes to MongoDB model entities
        assertNotNull(retrieved);
        assertNotNull(retrieved.getBusinessStream());

        // Verify the deserialized object uses MongoDB model classes
        org.ikasan.mongo.persistence.business.stream.metadata.model.BusinessStream businessStream =
            (org.ikasan.mongo.persistence.business.stream.metadata.model.BusinessStream) retrieved.getBusinessStream();

        assertNotNull(businessStream);
        assertNotNull(businessStream.getFlows());
        assertNotNull(businessStream.getDestinations());
        assertNotNull(businessStream.getIntegratedSystems());
        assertNotNull(businessStream.getEdges());

        // Verify flows are deserialized correctly
        assertTrue(businessStream.getFlows().size() > 0);

        // Verify the flow is a MongoDB model entity
        assertTrue(businessStream.getFlows().get(0) instanceof org.ikasan.mongo.persistence.business.stream.metadata.model.Flow);

        // Verify flow properties
        org.ikasan.mongo.persistence.business.stream.metadata.model.Flow firstFlow = businessStream.getFlows().get(0);
        assertNotNull(firstFlow.getId());
        assertNotNull(firstFlow.getModuleName());
        assertNotNull(firstFlow.getFlowName());
    }
}
