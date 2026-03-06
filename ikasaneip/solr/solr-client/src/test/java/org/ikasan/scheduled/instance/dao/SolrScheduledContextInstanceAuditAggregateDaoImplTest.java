package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SolrScheduledContextInstanceAuditAggregateDaoImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        this.tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        this.server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        this.server.request(createRequest);

        this.scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        this.scheduledContextInstanceAuditAggregateDao.setSolrClient(this.server);

    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_get_repeating_job_status_count_success() {
        List<ScheduledContextInstanceAuditAggregateRecord> auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
            20, InstanceStatus.COMPLETE);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id1", "contextName",
            3, InstanceStatus.ERROR);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
            27, InstanceStatus.COMPLETE);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        auditAggregateRecords
            = this.createScheduledContextInstanceAuditAggregateRecords("id2", "contextName",
            5, InstanceStatus.ERROR);
        this.scheduledContextInstanceAuditAggregateDao.save(auditAggregateRecords);

        Map<String, Map<String, Integer>> statusCounts = this.scheduledContextInstanceAuditAggregateDao
            .getRepeatingJobStatusCounts(List.of("id1", "id2"));

        Assert.assertTrue(!statusCounts.isEmpty());
        Assert.assertEquals(20, statusCounts.get("id1").get("COMPLETE").intValue());
        Assert.assertEquals(3, statusCounts.get("id1").get("ERROR").intValue());
        Assert.assertEquals(27, statusCounts.get("id2").get("COMPLETE").intValue());
        Assert.assertEquals(5, statusCounts.get("id2").get("ERROR").intValue());
    }


    @Test
    public void test_findAll() {
        List<ScheduledContextInstanceAuditAggregateRecord> records = createScheduledContextInstanceAuditAggregateRecords(
            "id1", "contextName", 5, InstanceStatus.COMPLETE
        );
        this.scheduledContextInstanceAuditAggregateDao.save(records);

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findAll(10, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(5, results.getResultList().size());
    }

    @Test
    public void test_findAll_with_limit() {
        List<ScheduledContextInstanceAuditAggregateRecord> records = createScheduledContextInstanceAuditAggregateRecords(
            "id1", "contextName", 10, InstanceStatus.COMPLETE
        );
        this.scheduledContextInstanceAuditAggregateDao.save(records);

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findAll(5, 0, null, null);

        Assert.assertNotNull(results);
        Assert.assertEquals(5, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_context_name() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "myContext", 3, InstanceStatus.COMPLETE)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id2", "otherContext", 2, InstanceStatus.COMPLETE)
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("myContext");

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, null, null
            );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_context_instance_id() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("instanceId123", "context1", 4, InstanceStatus.COMPLETE)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("instanceId456", "context2", 2, InstanceStatus.COMPLETE)
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextInstanceId("instanceId123");

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, null, null
            );

        Assert.assertEquals(4, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_status() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 2, InstanceStatus.ERROR)
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setStatus(InstanceStatus.ERROR.name());

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, null, null
            );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_scheduled_process_event_name() {
        List<ScheduledContextInstanceAuditAggregateRecord> records1 =
            createScheduledContextInstanceAuditAggregateRecordsWithEvent("id1", "context1", 3, InstanceStatus.COMPLETE, "event1");
        this.scheduledContextInstanceAuditAggregateDao.save(records1);

        List<ScheduledContextInstanceAuditAggregateRecord> records2 =
            createScheduledContextInstanceAuditAggregateRecordsWithEvent("id2", "context2", 2, InstanceStatus.COMPLETE, "event2");
        this.scheduledContextInstanceAuditAggregateDao.save(records2);

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setScheduledProcessEventName("event1");

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, null, null
            );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_sorting_ascending() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, "timestamp", "ASCENDING"
            );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_sorting_descending() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE)
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, "timestamp", "DESC"
            );

        Assert.assertEquals(3, results.getResultList().size());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_multiple_context_instances() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 10, InstanceStatus.COMPLETE)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.ERROR)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 15, InstanceStatus.COMPLETE)
        );
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id2", "context2", 3, InstanceStatus.RUNNING)
        );

        Map<String, Map<String, Integer>> statusCounts =
            this.scheduledContextInstanceAuditAggregateDao.getRepeatingJobStatusCounts(List.of("id1", "id2"));

        Assert.assertEquals(2, statusCounts.size());
        Assert.assertEquals(10, statusCounts.get("id1").get("COMPLETE").intValue());
        Assert.assertEquals(5, statusCounts.get("id1").get("ERROR").intValue());
        Assert.assertEquals(15, statusCounts.get("id2").get("COMPLETE").intValue());
        Assert.assertEquals(3, statusCounts.get("id2").get("RUNNING").intValue());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_empty_list() {
        Map<String, Map<String, Integer>> statusCounts =
            this.scheduledContextInstanceAuditAggregateDao.getRepeatingJobStatusCounts(List.of());

        Assert.assertTrue(statusCounts.isEmpty());
    }

    @Test
    public void test_getRepeatingJobStatusCounts_with_nonexistent_context_instance() {
        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.COMPLETE)
        );

        Map<String, Map<String, Integer>> statusCounts =
            this.scheduledContextInstanceAuditAggregateDao.getRepeatingJobStatusCounts(List.of("nonexistent"));

        Assert.assertTrue(statusCounts.get("nonexistent").get("COMPLETE").equals(0));
    }

    @Test
    public void test_save_non_repeating_jobs_excluded_from_repeating_counts() {
        List<ScheduledContextInstanceAuditAggregateRecord> repeatingRecords =
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 5, InstanceStatus.COMPLETE);
        this.scheduledContextInstanceAuditAggregateDao.save(repeatingRecords);

        List<ScheduledContextInstanceAuditAggregateRecord> nonRepeatingRecords =
            createScheduledContextInstanceAuditAggregateRecords("id1", "context1", 3, InstanceStatus.COMPLETE);
        nonRepeatingRecords.forEach(r -> r.setRepeatingJob(false));
        this.scheduledContextInstanceAuditAggregateDao.save(nonRepeatingRecords);

        Map<String, Map<String, Integer>> statusCounts =
            this.scheduledContextInstanceAuditAggregateDao.getRepeatingJobStatusCounts(List.of("id1"));

        Assert.assertNotNull(statusCounts.get("id1"));
        Assert.assertEquals(5, statusCounts.get("id1").get("COMPLETE").intValue());
    }

    @Test
    public void test_findScheduledContextInstanceAuditAggregateRecordsByFilter_with_multiple_filters() {
        List<ScheduledContextInstanceAuditAggregateRecord> records =
            createScheduledContextInstanceAuditAggregateRecordsWithEvent("id1", "myContext", 3, InstanceStatus.COMPLETE, "myEvent");
        this.scheduledContextInstanceAuditAggregateDao.save(records);

        this.scheduledContextInstanceAuditAggregateDao.save(
            createScheduledContextInstanceAuditAggregateRecordsWithEvent("id2", "otherContext", 2, InstanceStatus.ERROR, "otherEvent")
        );

        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("myContext");
        filter.setContextInstanceId("id1");
        filter.setStatus(InstanceStatus.COMPLETE.name());
        filter.setScheduledProcessEventName("myEvent");

        org.ikasan.spec.search.SearchResults<ScheduledContextInstanceAuditAggregateRecord> results =
            this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, 10, 0, null, null
            );

        Assert.assertEquals(3, results.getResultList().size());
    }

    private List<ScheduledContextInstanceAuditAggregateRecord> createScheduledContextInstanceAuditAggregateRecords(String contextInstanceId, String contextName
        , int numberToCreate, InstanceStatus instanceStatus) {
        List<ScheduledContextInstanceAuditAggregateRecord> records = new ArrayList<>();

        for (int i=0; i<numberToCreate; i++) {
            ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord
                = new SolrScheduledContextInstanceAuditAggregateRecordImpl();
            scheduledContextInstanceAuditAggregateRecord.setRepeatingJob(true);
            scheduledContextInstanceAuditAggregateRecord.setJobType("");
            scheduledContextInstanceAuditAggregateRecord.setContextInstanceId(contextInstanceId);
            scheduledContextInstanceAuditAggregateRecord.setStatus(instanceStatus.name());
            scheduledContextInstanceAuditAggregateRecord.setContextName(contextName);
            scheduledContextInstanceAuditAggregateRecord
                .setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
            scheduledContextInstanceAuditAggregateRecord.setScheduledProcessEventName("eventName");

            records.add(scheduledContextInstanceAuditAggregateRecord);
        }

        return records;
    }

    private List<ScheduledContextInstanceAuditAggregateRecord> createScheduledContextInstanceAuditAggregateRecordsWithEvent(
        String contextInstanceId, String contextName, int numberToCreate, InstanceStatus instanceStatus, String eventName) {
        List<ScheduledContextInstanceAuditAggregateRecord> records = new ArrayList<>();

        for (int i=0; i<numberToCreate; i++) {
            ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord
                = new SolrScheduledContextInstanceAuditAggregateRecordImpl();
            scheduledContextInstanceAuditAggregateRecord.setRepeatingJob(true);
            scheduledContextInstanceAuditAggregateRecord.setJobType("");
            scheduledContextInstanceAuditAggregateRecord.setContextInstanceId(contextInstanceId);
            scheduledContextInstanceAuditAggregateRecord.setStatus(instanceStatus.name());
            scheduledContextInstanceAuditAggregateRecord.setContextName(contextName);
            scheduledContextInstanceAuditAggregateRecord
                .setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
            scheduledContextInstanceAuditAggregateRecord.setScheduledProcessEventName(eventName);

            records.add(scheduledContextInstanceAuditAggregateRecord);
        }

        return records;
    }
}