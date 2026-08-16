package org.ikasan.mongo.persistence.configuration.metadata.dao;

import org.ikasan.mongo.persistence.configuration.metadata.model.MongoConfigurationMetaData;
import org.ikasan.mongo.persistence.configuration.metadata.model.MongoConfigurationParameterMetaData;
import org.ikasan.mongo.persistence.configuration.metadata.repository.MongoComponentConfigurationMetadataRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
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
 * Integration test for MongoComponentConfigurationMetadataDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoComponentConfigurationMetadataDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoComponentConfigurationMetadataRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ComponentConfigurationMetadataDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoComponentConfigurationMetadataRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoComponentConfigurationMetadataDaoImpl(repository, mongoTemplate);
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
    public void testSaveComponentMetadataList() {
        // Given
        MongoConfigurationParameterMetaData parameterMetaData =
            new MongoConfigurationParameterMetaData(12345L, "name", "value", "description", "implementingClass");
        List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
        parameterList.add(parameterMetaData);

        MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
            "configurationId", parameterList, "description", "implementingClass");

        List<ConfigurationMetaData> configList = new ArrayList<>();
        configList.add(configMetaData);

        // When - Saving twice should update the first
        dao.save(configList);
        dao.save(configList);

        // Then - Only one document should exist
        assertEquals(1, repository.count());
    }

    @Test
    public void testFindById() {
        // Given
        MongoConfigurationParameterMetaData parameterMetaData =
            new MongoConfigurationParameterMetaData(12345L, "name", "value", "description", "implementingClass");
        List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
        parameterList.add(parameterMetaData);

        MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
            "configurationId", parameterList, "description", "implementingClass");

        dao.save(Arrays.asList(configMetaData));

        // When
        MongoConfigurationMetaData found = (MongoConfigurationMetaData) dao.findById("configurationId");

        // Then
        assertNotNull(found);
        assertEquals("configurationId", found.getConfigurationId());
        assertEquals("description", found.getDescription());
        assertEquals("implementingClass", found.getImplementingClass());
        assertNotNull(found.getParameters());
        assertEquals(1, found.getParameters().size());
    }

    @Test
    public void testFindByIdNotFound() {
        // When
        ConfigurationMetaData result = dao.findById("non-existent-id");

        // Then
        assertNull(result);
    }

    @Test
    public void testFindAll() {
        // Given
        for (int i = 1; i <= 3; i++) {
            MongoConfigurationParameterMetaData parameterMetaData =
                new MongoConfigurationParameterMetaData((long) i, "name" + i, "value" + i, "description" + i, "class" + i);
            List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
            parameterList.add(parameterMetaData);

            MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
                "configurationId" + i, parameterList, "description" + i, "implementingClass" + i);

            dao.save(Arrays.asList(configMetaData));
        }

        // When
        List<ConfigurationMetaData> results = dao.findAll();

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    public void testFindInIdList() {
        // Given
        for (int i = 1; i <= 5; i++) {
            MongoConfigurationParameterMetaData parameterMetaData =
                new MongoConfigurationParameterMetaData((long) i, "name" + i, "value" + i, "description" + i, "class" + i);
            List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
            parameterList.add(parameterMetaData);

            MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
                "configurationId" + i, parameterList, "description" + i, "implementingClass" + i);

            dao.save(Arrays.asList(configMetaData));
        }

        // When - Search for specific IDs
        List<String> idsToFind = Arrays.asList("configurationId1", "configurationId3", "configurationId5");
        List<ConfigurationMetaData> results = dao.findInIdList(idsToFind);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());

        List<String> foundIds = new ArrayList<>();
        results.forEach(config -> foundIds.add(config.getConfigurationId()));
        assertTrue(foundIds.contains("configurationId1"));
        assertTrue(foundIds.contains("configurationId3"));
        assertTrue(foundIds.contains("configurationId5"));
    }

    @Test
    public void testFindInIdListEmpty() {
        // When - Empty ID list
        List<ConfigurationMetaData> results = dao.findInIdList(new ArrayList<>());

        // Then
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testFindInIdListNull() {
        // When - Null ID list
        List<ConfigurationMetaData> results = dao.findInIdList(null);

        // Then
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testParameterMetadataIsPreserved() {
        // Given
        MongoConfigurationParameterMetaData param1 =
            new MongoConfigurationParameterMetaData(1L, "param1", "value1", "desc1", "class1");
        MongoConfigurationParameterMetaData param2 =
            new MongoConfigurationParameterMetaData(2L, "param2", "value2", "desc2", "class2");

        List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
        parameterList.add(param1);
        parameterList.add(param2);

        MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
            "testConfig", parameterList, "test description", "test.Class");

        dao.save(Arrays.asList(configMetaData));

        // When
        MongoConfigurationMetaData retrieved = (MongoConfigurationMetaData) dao.findById("testConfig");

        // Then
        assertNotNull(retrieved);
        assertNotNull(retrieved.getParameters());
        assertEquals(2, retrieved.getParameters().size());

        // Verify first parameter
        assertEquals(1L, (long)retrieved.getParameters().get(0).getId());
        assertEquals("param1", retrieved.getParameters().get(0).getName());
        assertEquals("value1", retrieved.getParameters().get(0).getValue());
        assertEquals("desc1", retrieved.getParameters().get(0).getDescription());
        assertEquals("class1", retrieved.getParameters().get(0).getImplementingClass());
    }

    @Test
    public void testConfigurationMetadataUsesMongoModelEntities() {
        // Given
        MongoConfigurationParameterMetaData parameterMetaData =
            new MongoConfigurationParameterMetaData(12345L, "name", "value", "description", "implementingClass");
        List<MongoConfigurationParameterMetaData> parameterList = new ArrayList<>();
        parameterList.add(parameterMetaData);

        MongoConfigurationMetaData configMetaData = new MongoConfigurationMetaData(
            "configurationId", parameterList, "description", "implementingClass");

        dao.save(Arrays.asList(configMetaData));

        // When
        ConfigurationMetaData retrieved = dao.findById("configurationId");

        // Then - Verify retrieved object uses MongoDB model classes
        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MongoConfigurationMetaData);

        MongoConfigurationMetaData mongoConfig = (MongoConfigurationMetaData) retrieved;
        assertNotNull(mongoConfig.getParameters());
        assertTrue(mongoConfig.getParameters().size() > 0);
        assertTrue(mongoConfig.getParameters().get(0) instanceof MongoConfigurationParameterMetaData);

        MongoConfigurationParameterMetaData param = mongoConfig.getParameters().get(0);
        assertEquals(12345L, (long  )param.getId());
        assertEquals("name", param.getName());
        assertEquals("value", param.getValue());
    }

    @Test
    public void testSaveReplacesExistingConfiguration() {
        // Given
        MongoConfigurationParameterMetaData param1 =
            new MongoConfigurationParameterMetaData(1L, "oldParam", "oldValue", "old desc", "oldClass");

        MongoConfigurationMetaData configMetaData1 = new MongoConfigurationMetaData(
            "configId", Arrays.asList(param1), "old description", "old.Class");

        dao.save(Arrays.asList(configMetaData1));

        // When - Save with same ID but different data
        MongoConfigurationParameterMetaData param2 =
            new MongoConfigurationParameterMetaData(2L, "newParam", "newValue", "new desc", "newClass");

        MongoConfigurationMetaData configMetaData2 = new MongoConfigurationMetaData(
            "configId", Arrays.asList(param2), "new description", "new.Class");

        dao.save(Arrays.asList(configMetaData2));

        // Then - Should have only one record with new data
        assertEquals(1, repository.count());

        MongoConfigurationMetaData retrieved = (MongoConfigurationMetaData) dao.findById("configId");
        assertNotNull(retrieved);
        assertEquals("new description", retrieved.getDescription());
        assertEquals("new.Class", retrieved.getImplementingClass());
        assertEquals("newParam", retrieved.getParameters().get(0).getName());
    }
}
