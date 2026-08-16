package org.ikasan.mongo.persistence.scheduled.event.dao;

import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.mongo.persistence.scheduled.event.repository.MongoScheduledProcessEventRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
import java.util.concurrent.TimeUnit;

/**
 * MongoDB DAO Test for ScheduledProcessEvent operations.
 * This test class contains comprehensive tests for MongoScheduledProcessEventDao.
 *
 * Test coverage includes:
 * - Basic CRUD operations (save, get by various filters)
 * - Search/filter operations (by agent, job group, job name, time range)
 * - Complex filtering with multiple criteria
 * - Failures-only filtering
 * - Pagination and sorting
 * - Bulk operations
 * - Expiry/deletion operations
 * - Getting all distinct agent names
 *
 * @author Ikasan Development Team
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoScheduledProcessEventDaoTest {

    public static MongoDBContainer mongoDBContainer;

    private static final int DAYS_TO_KEEP = 90;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoScheduledProcessEventRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoScheduledProcessEventDao dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoScheduledProcessEventRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoScheduledProcessEventDao(repository, mongoTemplate, DAYS_TO_KEEP);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_scheduledProcessEvent() {
        ContextualisedScheduledProcessEventImpl event = createTestEvent(
                "agent1", "group1", "job1", System.currentTimeMillis(), true,
                "SUCCESS", "Output text", null);

        dao.save(event);

        // Verify it was saved by querying
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents("agent1",
                        System.currentTimeMillis() - 10000,
                        System.currentTimeMillis() + 10000);

        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.getResultList().size());
        ContextualisedScheduledProcessEventImpl found = results.getResultList().get(0);
        Assert.assertEquals("agent1", found.getAgentName());
        Assert.assertEquals("group1", found.getJobGroup());
        Assert.assertEquals("job1", found.getJobName());
        Assert.assertTrue(found.isSuccessful());
    }

    @Test
    public void test_save_multiple_scheduledProcessEvents() {
        List<ContextualisedScheduledProcessEventImpl> events = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        events.add(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        events.add(createTestEvent("agent1", "group1", "job2", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        events.add(createTestEvent("agent2", "group2", "job3", currentTime + 2000, false, "FAILED", "Output 3", "Error occurred"));

        dao.save(events);

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 10000,
                        currentTime + 10000);

        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.getResultList().size());
        Assert.assertEquals(3, results.getTotalNumberOfResults());
    }

    @Test
    public void test_get_all_agent_names() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent2", "group1", "job2", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent1", "group2", "job3", currentTime + 2000, true, "SUCCESS", "Output 3", null));
        dao.save(createTestEvent("agent3", "group3", "job4", currentTime + 3000, false, "FAILED", "Output 4", "Error"));

        List<String> agentNames = dao.getAllAgentNames();

        Assert.assertNotNull(agentNames);
        Assert.assertEquals(3, agentNames.size());
        Assert.assertTrue(agentNames.contains("agent1"));
        Assert.assertTrue(agentNames.contains("agent2"));
        Assert.assertTrue(agentNames.contains("agent3"));
    }

    @Test
    public void test_get_scheduledProcessEvents_by_agent_and_time_range() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime - 5000, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent2", "group1", "job3", currentTime + 1000, false, "FAILED", "Output 3", "Error"));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents("agent1",
                        currentTime - 10000,
                        currentTime + 10000);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event ->
                Assert.assertEquals("agent1", event.getAgentName()));
    }

    @Test
    public void test_get_scheduledProcessEvents_by_time_range_only() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime - 20000, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent2", "group1", "job2", currentTime, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent3", "group1", "job3", currentTime + 1000, false, "FAILED", "Output 3", "Error"));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_get_scheduledProcessEvents_with_accessible_modules_filter() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent2", "group1", "job2", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent3", "group1", "job3", currentTime + 2000, false, "FAILED", "Output 3", "Error"));

        List<String> accessibleModules = Arrays.asList("agent1", "agent2");

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(accessibleModules,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event ->
                Assert.assertTrue(accessibleModules.contains(event.getAgentName())));
    }

    @Test
    public void test_get_scheduledProcessEvents_with_empty_accessible_modules() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));

        List<String> accessibleModules = new ArrayList<>();

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(accessibleModules,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_get_scheduledProcessEvents_failures_only() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 1000, false, "FAILED", "Output 2", "Error 1"));
        dao.save(createTestEvent("agent1", "group1", "job3", currentTime + 2000, false, "FAILED", "Output 3", "Error 2"));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        true,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event ->
                Assert.assertFalse(event.isSuccessful()));
    }

    @Test
    public void test_get_scheduledProcessEvents_with_text_filter() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Special output text", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 1000, false, "FAILED", "Normal output", "Special error message"));
        dao.save(createTestEvent("agent1", "group1", "job3", currentTime + 2000, true, "SUCCESS", "Regular output", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000,
                        "Special",
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_get_scheduledProcessEvents_with_pagination() {
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < 25; i++) {
            dao.save(createTestEvent("agent1", "group1", "job" + i,
                    currentTime + (i * 1000), true, "SUCCESS", "Output " + i, null));
        }

        // Get first page
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> page1 =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 30000,
                        null,
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(page1);
        Assert.assertEquals(10, page1.getResultList().size());
        Assert.assertEquals(25, page1.getTotalNumberOfResults());

        // Get second page
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> page2 =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 30000,
                        null,
                        false,
                        10,
                        10,
                        "desc");

        Assert.assertNotNull(page2);
        Assert.assertEquals(10, page2.getResultList().size());
        Assert.assertEquals(25, page2.getTotalNumberOfResults());

        // Get third page
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> page3 =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 30000,
                        null,
                        false,
                        20,
                        10,
                        "desc");

        Assert.assertNotNull(page3);
        Assert.assertEquals(5, page3.getResultList().size());
        Assert.assertEquals(25, page3.getTotalNumberOfResults());
    }

    @Test
    public void test_get_scheduledProcessEvents_with_sorting_asc() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime + 2000, true, "SUCCESS", "Output 3", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job3", currentTime + 1000, true, "SUCCESS", "Output 2", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        false,
                        0,
                        10,
                        "asc");

        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.getResultList().size());
        // Verify ascending order by fireTime
        List<ContextualisedScheduledProcessEventImpl> resultList = results.getResultList();
        Assert.assertTrue(resultList.get(0).getFireTime() <= resultList.get(1).getFireTime());
        Assert.assertTrue(resultList.get(1).getFireTime() <= resultList.get(2).getFireTime());
    }

    @Test
    public void test_get_scheduledProcessEvents_with_sorting_desc() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent1", "group1", "job3", currentTime + 2000, true, "SUCCESS", "Output 3", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.getResultList().size());
        // Verify descending order by fireTime
        List<ContextualisedScheduledProcessEventImpl> resultList = results.getResultList();
        Assert.assertTrue(resultList.get(0).getFireTime() >= resultList.get(1).getFireTime());
        Assert.assertTrue(resultList.get(1).getFireTime() >= resultList.get(2).getFireTime());
    }

    @Test
    public void test_get_scheduledProcessEvents_by_agent_job_group_job_name() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent1", "group2", "job1", currentTime + 2000, true, "SUCCESS", "Output 3", null));
        dao.save(createTestEvent("agent2", "group1", "job1", currentTime + 3000, true, "SUCCESS", "Output 4", null));

        // Filter by agent and job group
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents("agent1", "group1", null,
                        currentTime - 5000,
                        currentTime + 5000,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event -> {
            Assert.assertEquals("agent1", event.getAgentName());
            Assert.assertEquals("group1", event.getJobGroup());
        });
    }

    @Test
    public void test_get_scheduledProcessEvents_by_job_name_only() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group2", "job1", currentTime + 1000, true, "SUCCESS", "Output 2", null));
        dao.save(createTestEvent("agent2", "group1", "job2", currentTime + 2000, true, "SUCCESS", "Output 3", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null, null, "job1",
                        currentTime - 5000,
                        currentTime + 5000,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event ->
                Assert.assertEquals("job1", event.getJobName()));
    }

    @Test
    public void test_get_scheduledProcessEvents_by_all_filters() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));
        dao.save(createTestEvent("agent1", "group1", "job1", currentTime + 1000, false, "FAILED", "Output 2", "Error"));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 2000, true, "SUCCESS", "Output 3", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents("agent1", "group1", "job1",
                        currentTime - 5000,
                        currentTime + 5000,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
        results.getResultList().forEach(event -> {
            Assert.assertEquals("agent1", event.getAgentName());
            Assert.assertEquals("group1", event.getJobGroup());
            Assert.assertEquals("job1", event.getJobName());
        });
    }

    @Test
    public void test_delete_expired_events() {
        long currentTime = System.currentTimeMillis();
        long oldTime = currentTime - TimeUnit.DAYS.toMillis(DAYS_TO_KEEP + 10);

        // Save events - one old, one current
        dao.save(createTestEvent("agent1", "group1", "job1", oldTime, true, "SUCCESS", "Old output", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime, true, "SUCCESS", "Current output", null));

        // Verify both exist
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> beforeDelete =
                dao.getScheduleProcessEvents(null,
                        oldTime - 10000,
                        currentTime + 10000);
        Assert.assertEquals(2, beforeDelete.getResultList().size());

        // Delete expired
        dao.deleteExpired();

        // Verify only current event remains
        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> afterDelete =
                dao.getScheduleProcessEvents(null,
                        oldTime - 10000,
                        currentTime + 10000);
        Assert.assertEquals(1, afterDelete.getResultList().size());
        Assert.assertEquals("job2", afterDelete.getResultList().get(0).getJobName());
    }

    @Test
    public void test_query_response_time_is_tracked() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output 1", null));

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(null,
                        currentTime - 5000,
                        currentTime + 5000,
                        null,
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertTrue(results.getQueryResponseTime() >= 0);
    }

    @Test
    public void test_complex_filter_with_multiple_criteria() {
        long currentTime = System.currentTimeMillis();

        dao.save(createTestEvent("agent1", "group1", "job1", currentTime, true, "SUCCESS", "Output with keyword", null));
        dao.save(createTestEvent("agent1", "group1", "job2", currentTime + 1000, false, "FAILED", "Normal output", "Error with keyword"));
        dao.save(createTestEvent("agent2", "group1", "job3", currentTime + 2000, true, "SUCCESS", "Regular output", null));

        List<String> accessibleModules = Arrays.asList("agent1", "agent2");

        ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> results =
                dao.getScheduleProcessEvents(accessibleModules,
                        currentTime - 5000,
                        currentTime + 5000,
                        "keyword",
                        false,
                        0,
                        10,
                        "desc");

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.getResultList().size());
    }

    /**
     * Helper method to create a test ScheduledProcessEvent
     */
    private ContextualisedScheduledProcessEventImpl createTestEvent(String agentName, String jobGroup,
                                                                     String jobName, long fireTime,
                                                                     boolean successful, String outcome,
                                                                     String resultOutput, String resultError) {
        ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
        event.setAgentName(agentName);
        event.setJobGroup(jobGroup);
        event.setJobName(jobName);
        event.setFireTime(fireTime);
        event.setSuccessful(successful);
        event.setOutcome(outcome);
        event.setResultOutput(resultOutput);
        event.setResultError(resultError);
        event.setCommandLine("test-command");
        return event;
    }
}
