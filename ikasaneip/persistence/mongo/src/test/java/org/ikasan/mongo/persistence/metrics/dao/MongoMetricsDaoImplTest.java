package org.ikasan.mongo.persistence.metrics.dao;

import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.ikasan.mongo.persistence.metrics.repository.MongoFlowInvocationMetricRepository;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.MongoPersistenceTestAutoConfiguration;
import org.ikasan.spec.history.FlowInvocationMetric;
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
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MongoMetricsDaoImpl using Testcontainers with MongoDB.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class, MongoPersistenceTestAutoConfiguration.class})
public class MongoMetricsDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoFlowInvocationMetricRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoMetricsDaoImpl dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoFlowInvocationMetricRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoMetricsDaoImpl(repository, mongoTemplate, 1000);
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
    public void testSaveSingleMetric() {
        // Given
        FlowInvocationMetric metric = createMetric("module1", "flow1", 1000L, 2000L, "success");

        // When
        dao.save(metric);

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testSaveMultipleMetrics() {
        // Given
        List<FlowInvocationMetric> metrics = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            metrics.add(createMetric("module" + i, "flow" + i, 1000L * i, 2000L * i, "success"));
        }

        // When
        dao.save(metrics);

        // Then
        assertEquals(3, repository.count());
    }

    @Test
    public void testGetMetricsByTimeRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module2", "flow2", baseTime + 5000, baseTime + 6000, "success"));
        dao.save(createMetric("module3", "flow3", baseTime + 10000, baseTime + 11000, "success"));

        // When
        List<FlowInvocationMetric> results = dao.getMetrics(baseTime, baseTime + 7000);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testGetMetricsByTimeRangeWithPagination() {
        // Given
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            dao.save(createMetric("module" + i, "flow" + i, baseTime + (i * 1000), baseTime + (i * 1000) + 500, "success"));
        }

        // When
        List<FlowInvocationMetric> page1 = dao.getMetrics(baseTime, baseTime + 20000, 0, 5);
        List<FlowInvocationMetric> page2 = dao.getMetrics(baseTime, baseTime + 20000, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testCountByTimeRange() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module2", "flow2", baseTime + 5000, baseTime + 6000, "success"));
        dao.save(createMetric("module3", "flow3", baseTime + 10000, baseTime + 11000, "success"));

        // When
        long count = dao.count(baseTime, baseTime + 7000);

        // Then
        assertEquals(2, count);
    }

    @Test
    public void testGetMetricsByModuleName() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module1", "flow2", baseTime + 2000, baseTime + 3000, "success"));
        dao.save(createMetric("module2", "flow3", baseTime + 4000, baseTime + 5000, "success"));

        // When
        List<FlowInvocationMetric> results = dao.getMetrics("module1", baseTime, baseTime + 10000);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(metric -> assertEquals("module1", metric.getModuleName()));
    }

    @Test
    public void testGetMetricsByModuleNameWithPagination() {
        // Given
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            dao.save(createMetric("module1", "flow" + i, baseTime + (i * 1000), baseTime + (i * 1000) + 500, "success"));
        }

        // When
        List<FlowInvocationMetric> page1 = dao.getMetrics("module1", baseTime, baseTime + 20000, 0, 5);
        List<FlowInvocationMetric> page2 = dao.getMetrics("module1", baseTime, baseTime + 20000, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testCountByModuleName() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module1", "flow2", baseTime + 2000, baseTime + 3000, "success"));
        dao.save(createMetric("module2", "flow3", baseTime + 4000, baseTime + 5000, "success"));

        // When
        long count = dao.count("module1", baseTime, baseTime + 10000);

        // Then
        assertEquals(2, count);
    }

    @Test
    public void testGetMetricsByModuleAndFlowName() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module1", "flow1", baseTime + 2000, baseTime + 3000, "success"));
        dao.save(createMetric("module1", "flow2", baseTime + 4000, baseTime + 5000, "success"));
        dao.save(createMetric("module2", "flow1", baseTime + 6000, baseTime + 7000, "success"));

        // When
        List<FlowInvocationMetric> results = dao.getMetrics("module1", "flow1", baseTime, baseTime + 10000);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(metric -> {
            assertEquals("module1", metric.getModuleName());
            assertEquals("flow1", metric.getFlowName());
        });
    }

    @Test
    public void testGetMetricsByModuleAndFlowNameWithPagination() {
        // Given
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            dao.save(createMetric("module1", "flow1", baseTime + (i * 1000), baseTime + (i * 1000) + 500, "success"));
        }

        // When
        List<FlowInvocationMetric> page1 = dao.getMetrics("module1", "flow1", baseTime, baseTime + 20000, 0, 5);
        List<FlowInvocationMetric> page2 = dao.getMetrics("module1", "flow1", baseTime, baseTime + 20000, 5, 5);

        // Then
        assertNotNull(page1);
        assertEquals(5, page1.size());
        assertNotNull(page2);
        assertEquals(5, page2.size());
    }

    @Test
    public void testCountByModuleAndFlowName() {
        // Given
        long baseTime = System.currentTimeMillis();
        dao.save(createMetric("module1", "flow1", baseTime, baseTime + 1000, "success"));
        dao.save(createMetric("module1", "flow1", baseTime + 2000, baseTime + 3000, "success"));
        dao.save(createMetric("module1", "flow2", baseTime + 4000, baseTime + 5000, "success"));

        // When
        long count = dao.count("module1", "flow1", baseTime, baseTime + 10000);

        // Then
        assertEquals(2, count);
    }

    @Test(expected = RuntimeException.class)
    public void testQueryLimitExceeded() {
        // Given
        long baseTime = System.currentTimeMillis();

        // When - Try to query with limit exceeding maximum
        dao.getMetrics(baseTime, baseTime + 10000, 0, 2000);

        // Then - Should throw RuntimeException
    }

    @Test
    public void testRemoveExpired() {
        // Given
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        FlowInvocationMetric expiredMetric = createMetric("module1", "flow1", currentTime, currentTime + 1000, "success");
        expiredMetric.setExpiry(oneDayAgo); // Already expired
        dao.save(expiredMetric);

        FlowInvocationMetric validMetric = createMetric("module2", "flow2", currentTime, currentTime + 1000, "success");
        validMetric.setExpiry(currentTime + (24 * 60 * 60 * 1000)); // Expires tomorrow
        dao.save(validMetric);

        assertEquals(2, repository.count());

        // When
        dao.removeExpired();

        // Then
        assertEquals(1, repository.count());
    }

    @Test
    public void testMetricFieldsPreserved() {
        // Given
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 5000;
        FlowInvocationMetric metric = createMetric("testModule", "testFlow", startTime, endTime, "finalAction");
        metric.setErrorUri("error-uri-123");
        metric.setExpiry(startTime + 1000000);

        dao.save(metric);

        // When
        List<FlowInvocationMetric> results = dao.getMetrics("testModule", "testFlow", startTime, endTime + 1000);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        FlowInvocationMetric retrieved = results.get(0);
        assertEquals("testModule", retrieved.getModuleName());
        assertEquals("testFlow", retrieved.getFlowName());
        assertEquals(startTime, retrieved.getInvocationStartTime());
        assertEquals(endTime, retrieved.getInvocationEndTime());
        assertEquals("finalAction", retrieved.getFinalAction());
        assertEquals("error-uri-123", retrieved.getErrorUri());
        assertTrue(retrieved.getHarvestedDateTime() > 0);
    }

    private FlowInvocationMetric createMetric(String moduleName, String flowName, long startTime, long endTime, String finalAction) {
        MongoFlowInvocationMetric metric = new MongoFlowInvocationMetric();
        metric.setModuleName(moduleName);
        metric.setFlowName(flowName);
        metric.setInvocationStartTime(startTime);
        metric.setInvocationEndTime(endTime);
        metric.setFinalAction(finalAction);
        metric.setHarvested(false);
        metric.setExpiry(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)); // 7 days from now
        return metric;
    }
}
