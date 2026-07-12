package org.ikasan.relational.persistence.module.metadata.dao;

import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.module.metadata.model.*;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for HibernateModuleMetadataDaoImpl using Testcontainers with PostgreSQL.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateModuleMetadataDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withStartupTimeout(Duration.ofSeconds(20))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
        postgres.start();
    }

    @Autowired
    private ModuleMetadataDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    @Transactional
    public void tearDown() {
        // Clean up all test data
        List<ModuleMetaData> all = dao.findAll(0, -1);
        all.forEach(record -> dao.deleteById(record.getName()));
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    @Transactional
    public void testSaveAndFindById() {
        // Given
        HibernateModuleMetaDataImpl moduleMetaData = createTestModuleMetadata("test-module-1", ModuleType.INTEGRATION_MODULE);

        // When
        dao.save(Arrays.asList(moduleMetaData));

        // Then
        ModuleMetaData found = dao.findById("test-module-1");
        assertNotNull("Module metadata should be found", found);
        assertEquals("test-module-1", found.getName());
        assertEquals(ModuleType.INTEGRATION_MODULE, found.getType());
        assertEquals("Test Module 1", found.getDescription());
        assertEquals("1.0.0", found.getVersion());
        assertEquals("3.3.0", found.getIkasanVersion());
        assertEquals("http://localhost:8080", found.getUrl());
        assertEquals("localhost", found.getHost());
        assertEquals(Integer.valueOf(8080), found.getPort());
        assertEquals("/test", found.getContext());
        assertEquals("http", found.getProtocol());
        assertEquals("config-id-1", found.getConfiguredResourceId());
    }

    @Test
    @Transactional
    public void testSaveWithFlows() {
        // Given
        HibernateModuleMetaDataImpl moduleMetaData = createTestModuleMetadata("test-module-2", ModuleType.INTEGRATION_MODULE);

        // Add flows
        HibernateFlowMetaDataImpl flow1 = createTestFlow("flow-1");
        HibernateFlowMetaDataImpl flow2 = createTestFlow("flow-2");
        moduleMetaData.setFlows(Arrays.asList(flow1, flow2));

        // When
        dao.save(Arrays.asList(moduleMetaData));

        // Then
        ModuleMetaData found = dao.findById("test-module-2");
        assertNotNull("Module metadata should be found", found);
        assertEquals(2, found.getFlows().size());
        assertEquals("flow-1", found.getFlows().get(0).getName());
        assertEquals("flow-2", found.getFlows().get(1).getName());
    }

    @Test
    @Transactional
    public void testSaveWithComplexFlowStructure() {
        // Given
        HibernateModuleMetaDataImpl moduleMetaData = createTestModuleMetadata("test-module-3", ModuleType.INTEGRATION_MODULE);

        // Create flow with elements, transitions, and decorators
        HibernateFlowMetaDataImpl flow = new HibernateFlowMetaDataImpl();
        flow.setName("complex-flow");
        flow.setConfigurationId("flow-config-1");
        flow.setFlowStartupType("AUTOMATIC");
        flow.setFlowStartupComment("Auto start");

        // Add consumer
        HibernateFlowElementMetaDataImpl consumer = createFlowElement("consumer", "Consumer", true);
        flow.setConsumer(consumer);

        // Add flow elements
        HibernateFlowElementMetaDataImpl converter = createFlowElement("converter", "Converter", false);
        HibernateFlowElementMetaDataImpl producer = createFlowElement("producer", "Producer", true);

        // Add decorator to converter
        HibernateDecoratorMetaDataImpl decorator = new HibernateDecoratorMetaDataImpl();
        decorator.setName("wiretap");
        decorator.setType("WiretapDecorator");
        decorator.setConfigurable(true);
        decorator.setConfigurationId("wiretap-config-1");
        converter.setDecorators(Arrays.asList(decorator));

        flow.setFlowElements(Arrays.asList(consumer, converter, producer));

        // Add transitions
        HibernateTransitionImpl transition1 = createTransition("consumer", "converter", "default");
        HibernateTransitionImpl transition2 = createTransition("converter", "producer", "default");
        flow.setTransitions(Arrays.asList(transition1, transition2));

        moduleMetaData.setFlows(Arrays.asList(flow));

        // When
        dao.save(Arrays.asList(moduleMetaData));

        // Then
        ModuleMetaData found = dao.findById("test-module-3");
        assertNotNull("Module metadata should be found", found);
        assertEquals(1, found.getFlows().size());

        HibernateFlowMetaDataImpl foundFlow = (HibernateFlowMetaDataImpl) found.getFlows().get(0);
        assertEquals("complex-flow", foundFlow.getName());
        assertNotNull(foundFlow.getConsumer());
        assertEquals("consumer", foundFlow.getConsumer().getComponentName());
        assertEquals(3, foundFlow.getFlowElements().size());
        assertEquals(2, foundFlow.getTransitions().size());

        // Check decorator
        HibernateFlowElementMetaDataImpl foundConverter = (HibernateFlowElementMetaDataImpl) foundFlow.getFlowElements().get(1);
        assertEquals("converter", foundConverter.getComponentName());
        assertEquals(1, foundConverter.getDecorators().size());
        assertEquals("wiretap", foundConverter.getDecorators().get(0).getName());
    }

    @Test
    @Transactional
    public void testUpdate() {
        // Given
        HibernateModuleMetaDataImpl moduleMetaData = createTestModuleMetadata("test-module-4", ModuleType.INTEGRATION_MODULE);
        dao.save(Arrays.asList(moduleMetaData));

        // When - Update
        moduleMetaData.setDescription("Updated Description");
        moduleMetaData.setVersion("2.0.0");
        dao.save(Arrays.asList(moduleMetaData));

        // Then
        ModuleMetaData found = dao.findById("test-module-4");
        assertNotNull("Module metadata should be found", found);
        assertEquals("Updated Description", found.getDescription());
        assertEquals("2.0.0", found.getVersion());
    }

    @Test
    @Transactional
    public void testDeleteById() {
        // Given
        HibernateModuleMetaDataImpl moduleMetaData = createTestModuleMetadata("test-module-5", ModuleType.INTEGRATION_MODULE);
        dao.save(Arrays.asList(moduleMetaData));

        // When
        dao.deleteById("test-module-5");

        // Then
        ModuleMetaData found = dao.findById("test-module-5");
        assertNull("Module metadata should be deleted", found);
    }

    @Test
    @Transactional
    public void testFindAll() {
        // Given
        HibernateModuleMetaDataImpl module1 = createTestModuleMetadata("test-module-6", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module2 = createTestModuleMetadata("test-module-7", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module3 = createTestModuleMetadata("test-module-8", ModuleType.INTEGRATION_MODULE);

        dao.save(Arrays.asList(module1, module2, module3));

        // When
        List<ModuleMetaData> all = dao.findAll(0, -1);

        // Then
        assertNotNull("Result should not be null", all);
        assertEquals(3, all.size());
    }

    @Test
    @Transactional
    public void testFindAllWithPagination() {
        // Given
        for (int i = 1; i <= 5; i++) {
            HibernateModuleMetaDataImpl module = createTestModuleMetadata("test-module-" + i, ModuleType.INTEGRATION_MODULE);
            dao.save(Arrays.asList(module));
        }

        // When
        List<ModuleMetaData> page1 = dao.findAll(0, 2);
        List<ModuleMetaData> page2 = dao.findAll(2, 2);

        // Then
        assertEquals(2, page1.size());
        assertEquals(2, page2.size());
        assertNotEquals(page1.get(0).getName(), page2.get(0).getName());
    }

    @Test
    @Transactional
    public void testFindByModuleNames() {
        // Given
        HibernateModuleMetaDataImpl module1 = createTestModuleMetadata("module-a", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module2 = createTestModuleMetadata("module-b", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module3 = createTestModuleMetadata("module-c", ModuleType.INTEGRATION_MODULE);

        dao.save(Arrays.asList(module1, module2, module3));

        // When
        ModuleMetadataSearchResults results = dao.find(Arrays.asList("module-a", "module-c"), 0, 10);

        // Then
        assertNotNull("Results should not be null", results);
        assertEquals(2, results.getResultList().size());
        assertEquals(2, results.getTotalNumberOfResults());
        assertTrue(results.getQueryResponseTime() >= 0);
    }

    @Test
    @Transactional
    public void testFindByModuleType() {
        // Given
        HibernateModuleMetaDataImpl integrationModule = createTestModuleMetadata("integration-module", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl schedulerModule = createTestModuleMetadata("scheduler-module", ModuleType.SCHEDULER_AGENT);

        dao.save(Arrays.asList(integrationModule, schedulerModule));

        // When
        ModuleMetadataSearchResults results = dao.find(null, ModuleType.INTEGRATION_MODULE, 0, 10);

        // Then
        assertNotNull("Results should not be null", results);
        assertEquals(1, results.getResultList().size());
        assertEquals("integration-module", results.getResultList().get(0).getName());
        assertEquals(1, results.getTotalNumberOfResults());
    }

    @Test
    @Transactional
    public void testFindByModuleNamesAndType() {
        // Given
        HibernateModuleMetaDataImpl module1 = createTestModuleMetadata("module-1", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module2 = createTestModuleMetadata("module-2", ModuleType.SCHEDULER_AGENT);
        HibernateModuleMetaDataImpl module3 = createTestModuleMetadata("module-3", ModuleType.INTEGRATION_MODULE);

        dao.save(Arrays.asList(module1, module2, module3));

        // When
        ModuleMetadataSearchResults results = dao.find(
            Arrays.asList("module-1", "module-2", "module-3"),
            ModuleType.INTEGRATION_MODULE,
            0,
            10
        );

        // Then
        assertNotNull("Results should not be null", results);
        assertEquals(2, results.getResultList().size());
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    @Transactional
    public void testFindWithNoFilters() {
        // Given
        HibernateModuleMetaDataImpl module1 = createTestModuleMetadata("module-x", ModuleType.INTEGRATION_MODULE);
        HibernateModuleMetaDataImpl module2 = createTestModuleMetadata("module-y", ModuleType.SCHEDULER_AGENT);

        dao.save(Arrays.asList(module1, module2));

        // When
        ModuleMetadataSearchResults results = dao.find(null, 0, 10);

        // Then
        assertNotNull("Results should not be null", results);
        assertEquals(2, results.getResultList().size());
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    @Transactional
    public void testFindWithOffsetMinusOne() {
        // Given
        HibernateModuleMetaDataImpl module = createTestModuleMetadata("module-offset", ModuleType.INTEGRATION_MODULE);
        dao.save(Arrays.asList(module));

        // When
        ModuleMetadataSearchResults results = dao.find(null, ModuleType.INTEGRATION_MODULE, -1, -1);

        // Then
        assertNotNull("Results should not be null", results);
        assertEquals(1, results.getResultList().size());
    }

    // Helper methods

    private HibernateModuleMetaDataImpl createTestModuleMetadata(String name, ModuleType type) {
        HibernateModuleMetaDataImpl moduleMetaData = new HibernateModuleMetaDataImpl(name);
        moduleMetaData.setType(type);
        moduleMetaData.setDescription("Test Module " + name.substring(name.lastIndexOf('-') + 1));
        moduleMetaData.setVersion("1.0.0");
        moduleMetaData.setIkasanVersion("3.3.0");
        moduleMetaData.setUrl("http://localhost:8080");
        moduleMetaData.setHost("localhost");
        moduleMetaData.setPort(8080);
        moduleMetaData.setContext("/test");
        moduleMetaData.setProtocol("http");
        moduleMetaData.setConfiguredResourceId("config-id-1");
        return moduleMetaData;
    }

    private HibernateFlowMetaDataImpl createTestFlow(String name) {
        HibernateFlowMetaDataImpl flow = new HibernateFlowMetaDataImpl();
        flow.setName(name);
        flow.setConfigurationId("config-" + name);
        flow.setFlowStartupType("MANUAL");
        return flow;
    }

    private HibernateFlowElementMetaDataImpl createFlowElement(String componentName, String componentType, boolean configurable) {
        HibernateFlowElementMetaDataImpl element = new HibernateFlowElementMetaDataImpl();
        element.setComponentName(componentName);
        element.setComponentType(componentType);
        element.setDescription("Test " + componentType);
        element.setImplementingClass("org.ikasan.test." + componentType);
        element.setConfigurable(configurable);
        if (configurable) {
            element.setConfigurationId(componentName + "-config");
        }
        return element;
    }

    private HibernateTransitionImpl createTransition(String from, String to, String name) {
        HibernateTransitionImpl transition = new HibernateTransitionImpl();
        transition.setFrom(from);
        transition.setTo(to);
        transition.setName(name);
        return transition;
    }
}
