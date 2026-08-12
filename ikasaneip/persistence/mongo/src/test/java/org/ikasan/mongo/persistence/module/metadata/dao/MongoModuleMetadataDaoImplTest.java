package org.ikasan.mongo.persistence.module.metadata.dao;

import org.apache.commons.io.IOUtils;
import org.ikasan.mongo.persistence.module.metadata.model.*;
import org.ikasan.mongo.persistence.module.metadata.repository.MongoModuleMetadataRepository;
import org.ikasan.mongo.persistence.scheduled.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.context.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.*;
import org.ikasan.spec.module.ModuleType;
import org.junit.After;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoModuleMetadataDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoModuleMetadataDaoImplTest {

    public static final String MODULE_JSON = "/data/module.json";
    public static final String MODULE_SCHEDULER_AGENT_JSON = "/data/module-scheduler-agent.json";

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoModuleMetadataRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ModuleMetadataDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoModuleMetadataRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoModuleMetadataDaoImpl(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    private String loadDataFile(String fileName) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + fileName);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private ModuleMetaData loadModuleMetaDataFromFile(String fileName) throws IOException {
        SimpleModule m = new SimpleModule();
        m.addAbstractTypeMapping(ModuleMetaData.class, MongoModuleMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowMetaData.class, MongoFlowMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowElementMetaData.class, MongoFlowElementMetaDataImpl.class);
        m.addAbstractTypeMapping(Transition.class, MongoTransitionImpl.class);
        m.addAbstractTypeMapping(DecoratorMetaData.class, MongoDecoratorMetaDataImpl.class);

        JsonMapper objectMapper = JsonMapper.builder()
            .addModule(m)
            .build();

        String jsonContent = loadDataFile(fileName);
        return objectMapper.readValue(jsonContent, MongoModuleMetaDataImpl.class);
    }

    @Test
    public void testSaveModuleMetadataList() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();
        moduleMetaDataList.add(moduleMetaData);

        // When - Saving twice should update the first
        dao.save(moduleMetaDataList);
        dao.save(moduleMetaDataList);

        // Then - Only one document should exist
        assertEquals(1, repository.count());
    }

    @Test
    public void testFindById() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();
        moduleMetaDataList.add(moduleMetaData);

        dao.save(moduleMetaDataList);

        // When
        ModuleMetaData found = dao.findById("module name");

        // Then
        assertNotNull(found);
        assertEquals("module name", found.getName());
        assertEquals(6, found.getFlows().size());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        ModuleMetaData result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindAll() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);
        List<ModuleMetaData> moduleMetaDataList = new ArrayList<>();
        moduleMetaDataList.add(moduleMetaData);

        dao.save(moduleMetaDataList);

        // When
        List<ModuleMetaData> results = dao.findAll(0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("module name", results.get(0).getName());
        assertEquals(6, results.get(0).getFlows().size());
    }

    @Test
    public void testFindAllMultiple() throws IOException {
        // Given
        ModuleMetaData module1 = loadModuleMetaDataFromFile(MODULE_JSON);
        ModuleMetaData module2 = loadModuleMetaDataFromFile(MODULE_SCHEDULER_AGENT_JSON);

        List<ModuleMetaData> moduleList1 = new ArrayList<>();
        moduleList1.add(module1);
        dao.save(moduleList1);

        List<ModuleMetaData> moduleList2 = new ArrayList<>();
        moduleList2.add(module2);
        dao.save(moduleList2);

        // When
        List<ModuleMetaData> results = dao.findAll(0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testFindWithModuleNamesFilter() throws IOException {
        // Given
        ModuleMetaData module1 = loadModuleMetaDataFromFile(MODULE_JSON);
        ModuleMetaData module2 = loadModuleMetaDataFromFile(MODULE_SCHEDULER_AGENT_JSON);

        dao.save(Arrays.asList(module1));
        dao.save(Arrays.asList(module2));

        // When
        List<String> moduleNames = Arrays.asList("module name");
        ModuleMetadataSearchResults results = dao.find(moduleNames, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1, results.getTotalNumberOfResults());
        assertEquals("module name", results.getResultList().get(0).getName());
    }

    @Test
    public void testFindWithNullModuleNames() throws IOException {
        // Given
        ModuleMetaData module = loadModuleMetaDataFromFile(MODULE_JSON);
        dao.save(Arrays.asList(module));

        // When - null module names should return all
        ModuleMetadataSearchResults results = dao.find(null, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(1, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindWithEmptyModuleNames() throws IOException {
        // Given
        ModuleMetaData module = loadModuleMetaDataFromFile(MODULE_JSON);
        dao.save(Arrays.asList(module));

        // When - empty module names should return all
        ModuleMetadataSearchResults results = dao.find(new ArrayList<>(), 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
    }

    @Test
    public void testFindWithPagination() throws IOException {
        // Given - Create 5 modules
        for (int i = 1; i <= 5; i++) {
            ModuleMetaData module = loadModuleMetaDataFromFile(MODULE_JSON);
            module.setName("module" + i);
            dao.save(Arrays.asList(module));
        }

        // When - Get page with limit 3
        ModuleMetadataSearchResults results = dao.find(null, 0, 3);

        // Then
        assertNotNull(results);
        assertEquals(3, results.getResultList().size());
        assertEquals(5, results.getTotalNumberOfResults());
    }

    @Test
    public void testFindWithModuleType() throws IOException {
        // Given
        ModuleMetaData module = loadModuleMetaDataFromFile(MODULE_JSON);
        module.setType(ModuleType.INTEGRATION_MODULE);
        dao.save(Arrays.asList(module));

        // When
        ModuleMetadataSearchResults results = dao.find(null, ModuleType.INTEGRATION_MODULE, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals(ModuleType.INTEGRATION_MODULE, results.getResultList().get(0).getType());
    }

    @Test
    public void testFindWithModuleTypeNoMatch() throws IOException {
        // Given
        ModuleMetaData module = loadModuleMetaDataFromFile(MODULE_JSON);
        module.setType(ModuleType.INTEGRATION_MODULE);
        dao.save(Arrays.asList(module));

        // When - Search for different type
        ModuleMetadataSearchResults results = dao.find(null, ModuleType.SCHEDULER_AGENT, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getResultList().size());
    }

    @Test
    public void testFindWithModuleNamesAndType() throws IOException {
        // Given
        ModuleMetaData module1 = loadModuleMetaDataFromFile(MODULE_JSON);
        module1.setType(ModuleType.INTEGRATION_MODULE);

        ModuleMetaData module2 = loadModuleMetaDataFromFile(MODULE_SCHEDULER_AGENT_JSON);
        module2.setType(ModuleType.SCHEDULER_AGENT);

        dao.save(Arrays.asList(module1));
        dao.save(Arrays.asList(module2));

        // When
        List<String> moduleNames = Arrays.asList("module name");
        ModuleMetadataSearchResults results = dao.find(moduleNames, ModuleType.INTEGRATION_MODULE, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.getResultList().size());
        assertEquals("module name", results.getResultList().get(0).getName());
        assertEquals(ModuleType.INTEGRATION_MODULE, results.getResultList().get(0).getType());
    }

    @Test
    public void testDeleteById() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);
        dao.save(Arrays.asList(moduleMetaData));

        // Verify it exists
        assertNotNull(dao.findById("module name"));
        assertEquals(1, repository.count());

        // When
        dao.deleteById("module name");

        // Then
        assertNull(dao.findById("module name"));
        assertEquals(0, repository.count());
    }

    @Test
    public void testDeleteByIdNotFound() {
        // When - should not throw exception
        dao.deleteById("non-existent-id");

        // Then - no exception thrown
        assertEquals(0, repository.count());
    }

    @Test
    public void testModuleMetadataPreservesAllFields() throws IOException {
        // Given
        ModuleMetaData original = loadModuleMetaDataFromFile(MODULE_JSON);
        original.setType(ModuleType.INTEGRATION_MODULE);
        original.setUrl("http://localhost:8080");
        original.setHost("localhost");
        original.setPort(8080);
        original.setContext("/ikasan");
        original.setProtocol("http");
        original.setVersion("1.0.0");
        original.setIkasanVersion("3.0.0");
        original.setDescription("Test module");

        // When
        dao.save(Arrays.asList(original));
        ModuleMetaData retrieved = dao.findById(original.getName());

        // Then
        assertNotNull(retrieved);
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.getType(), retrieved.getType());
        assertEquals(original.getUrl(), retrieved.getUrl());
        assertEquals(original.getHost(), retrieved.getHost());
        assertEquals(original.getPort(), retrieved.getPort());
        assertEquals(original.getContext(), retrieved.getContext());
        assertEquals(original.getProtocol(), retrieved.getProtocol());
        assertEquals(original.getVersion(), retrieved.getVersion());
        assertEquals(original.getIkasanVersion(), retrieved.getIkasanVersion());
        assertEquals(original.getDescription(), retrieved.getDescription());
        assertEquals(original.getFlows().size(), retrieved.getFlows().size());
    }

    @Test
    public void testFlowMetadataIsPreserved() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);

        // When
        dao.save(Arrays.asList(moduleMetaData));
        ModuleMetaData retrieved = dao.findById(moduleMetaData.getName());

        // Then
        assertNotNull(retrieved);
        assertNotNull(retrieved.getFlows());
        assertEquals(6, retrieved.getFlows().size());

        // Verify first flow details are preserved
        FlowMetaData firstFlow = retrieved.getFlows().get(0);
        assertNotNull(firstFlow);
        assertNotNull(firstFlow.getName());
        assertNotNull(firstFlow.getConsumer());
    }

    @Test
    public void testModuleMetadataUsesMongoModelEntities() throws IOException {
        // Given
        ModuleMetaData moduleMetaData = loadModuleMetaDataFromFile(MODULE_JSON);
        moduleMetaData.setType(ModuleType.INTEGRATION_MODULE);

        // When
        dao.save(Arrays.asList(moduleMetaData));
        ModuleMetaData retrieved = dao.findById(moduleMetaData.getName());

        // Then - Verify retrieved object uses MongoDB model classes
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MongoModuleMetaDataImpl);

        // Verify flows use MongoDB model classes
        assertNotNull(retrieved.getFlows());
        assertTrue(retrieved.getFlows().size() > 0);
        assertTrue(retrieved.getFlows().get(0) instanceof MongoFlowMetaDataImpl);

        MongoFlowMetaDataImpl firstFlow = (MongoFlowMetaDataImpl) retrieved.getFlows().get(0);
        assertNotNull(firstFlow.getName());
        assertNotNull(firstFlow.getConsumer());

        // Verify flow consumer uses MongoDB model class
        assertTrue(firstFlow.getConsumer() instanceof MongoFlowElementMetaDataImpl);

        MongoFlowElementMetaDataImpl consumer = (MongoFlowElementMetaDataImpl) firstFlow.getConsumer();
        assertNotNull(consumer.getComponentName());
        assertNotNull(consumer.getComponentType());

        // Verify transitions use MongoDB model classes
        if (firstFlow.getTransitions() != null && !firstFlow.getTransitions().isEmpty()) {
            assertTrue(firstFlow.getTransitions().get(0) instanceof MongoTransitionImpl);

            MongoTransitionImpl transition = (MongoTransitionImpl) firstFlow.getTransitions().get(0);
            assertNotNull(transition.getFrom());
            assertNotNull(transition.getTo());
        }

        // Verify flow elements use MongoDB model classes
        if (firstFlow.getFlowElements() != null && !firstFlow.getFlowElements().isEmpty()) {
            assertTrue(firstFlow.getFlowElements().get(0) instanceof MongoFlowElementMetaDataImpl);

            MongoFlowElementMetaDataImpl flowElement = (MongoFlowElementMetaDataImpl) firstFlow.getFlowElements().get(0);
            assertNotNull(flowElement.getComponentName());

            // Verify decorators use MongoDB model classes if present
            if (flowElement.getDecorators() != null && !flowElement.getDecorators().isEmpty()) {
                assertTrue(flowElement.getDecorators().get(0) instanceof MongoDecoratorMetaDataImpl);

                MongoDecoratorMetaDataImpl decorator = (MongoDecoratorMetaDataImpl) flowElement.getDecorators().get(0);
                assertNotNull(decorator.getName());
            }
        }
    }
}
