package org.ikasan.scheduled.instance.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl.SCHEDULED_CONTEXT_INSTANCE;

public class SolrScheduledContextInstanceDaoImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceDaoImpl dao;
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

        this.dao = new SolrScheduledContextInstanceDaoImpl();
        this.dao.setSolrClient(this.server);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_save_and_findById() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1_" + SCHEDULED_CONTEXT_INSTANCE);

        Assert.assertNotNull(result);
        Assert.assertEquals("context1", result.getContextName());
        Assert.assertEquals(InstanceStatus.RUNNING.name(), result.getStatus());
    }

    @Test
    public void test_findById_returns_null_when_not_found() {
        ScheduledContextInstanceRecord result = dao.findById("nonexistent");

        Assert.assertNull(result);
    }

    @Test
    public void test_deleteById() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        dao.save(record);

        ScheduledContextInstanceRecord found = dao.findById("id1_" + SCHEDULED_CONTEXT_INSTANCE);
        Assert.assertNotNull(found);

        dao.deleteById("id1");

        ScheduledContextInstanceRecord notFound = dao.findById("id1_" + SCHEDULED_CONTEXT_INSTANCE);
        Assert.assertNull(notFound);
    }

    @Test
    public void test_getScheduledContextInstancesByStatus() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
            List.of(InstanceStatus.COMPLETE)
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByStatus_with_limit_and_offset() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
            List.of(InstanceStatus.COMPLETE), 2, 0
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByStatus_with_multiple_statuses() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.RUNNING));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByStatus(
            List.of(InstanceStatus.COMPLETE, InstanceStatus.ERROR)
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context2", InstanceStatus.RUNNING));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            "context1", 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_sorting_asc() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            "context1", 10, 0, "timestamp", "asc"
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_sorting_desc() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            "context1", 10, 0, "timestamp", "desc"
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_timestamp_range() {
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 24 * 60 * 60 * 1000;
        long twoDaysAgo = now - 2 * 24 * 60 * 60 * 1000;

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.setTimestamp(oneDayAgo);
        dao.save(record);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            "context1", twoDaysAgo, now, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByContextName_with_timestamp_range_and_sorting() {
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 24 * 60 * 60 * 1000;

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record1.setTimestamp(oneDayAgo);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "context1", InstanceStatus.COMPLETE);
        record2.setTimestamp(oneDayAgo + 1000);
        dao.save(record2);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByContextName(
            "context1", oneDayAgo - 1000, now, 10, 0, "timestamp", "ASCENDING"
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_names() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.RUNNING));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setContextInstanceNames(List.of("context1", "context2"));

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_search_filter() {
        dao.save(createContextInstanceRecord("id1", "myContext1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("id2", "myContext2", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id3", "otherContext", InstanceStatus.RUNNING));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setContextSearchFilter("myContext");

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_context_instance_id() {
        dao.save(createContextInstanceRecord("testId123", "context1", InstanceStatus.RUNNING));
        dao.save(createContextInstanceRecord("otherId456", "context2", InstanceStatus.COMPLETE));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setContextInstanceId("testId");

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
        Assert.assertEquals("testId123", results.getResultList().get(0).getContextInstance().getId());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_status() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE));
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.ERROR));
        dao.save(createContextInstanceRecord("id3", "context3", InstanceStatus.COMPLETE));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.COMPLETE.name());

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_created_timestamp() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record1.setTimestamp(now);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE);
        record2.setTimestamp(now - 2 * 24 * 60 * 60 * 1000); // 2 days ago
        dao.save(record2);

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setCreatedTimestamp(now);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_modified_timestamp() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.setTimestamp(now);
        dao.save(record);

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setModifiedTimestamp(now);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_start_time_range() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.getContextInstance().setStartTime(now);
        dao.save(record);

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStartTimeStart(now - 1000);
        filter.setStartTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_end_time_range() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.COMPLETE);
        ContextInstance contextInstance = record.getContextInstance();
        contextInstance.setEndTime(now);
        record.setContextInstance(contextInstance);
        dao.save(record);

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setEndTimeStart(now - 1000);
        filter.setEndTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_default_sorting() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(2, results.getResultList().size());
        // Default is descending by createdDateTime, so most recent first
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_custom_sorting() {
        dao.save(createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING));
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        dao.save(createContextInstanceRecord("id2", "context2", InstanceStatus.COMPLETE));

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, "timestamp", "ASCENDING"
        );

        Assert.assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_save_with_modifiedBy() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        record.setModifiedBy("testUser");
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1_" + SCHEDULED_CONTEXT_INSTANCE);
        Assert.assertNotNull(result);
        Assert.assertEquals("testUser", result.getModifiedBy());
    }

    @Test
    public void test_save_with_contains_repeating_jobs() {
        ScheduledContextInstanceRecord record = createContextInstanceRecord("id1", "context1", InstanceStatus.RUNNING);
        ContextInstance contextInstance = record.getContextInstance();
        contextInstance.setContainsRepeatingJobs(true);
        record.setContextInstance(contextInstance);
        dao.save(record);

        ScheduledContextInstanceRecord result = dao.findById("id1_" + SCHEDULED_CONTEXT_INSTANCE);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getContextInstance().isContainsRepeatingJobs());
    }

    @Test
    public void test_atStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 14, 30, 45);
        Date testDate = cal.getTime();

        long result = dao.atStartOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(result);

        Assert.assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, resultCal.get(Calendar.MINUTE));
        Assert.assertEquals(0, resultCal.get(Calendar.SECOND));
        Assert.assertEquals(0, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void test_atEndOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 10, 20, 30);
        Date testDate = cal.getTime();

        long result = dao.atEndOfDay(testDate);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(result);

        Assert.assertEquals(23, resultCal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(59, resultCal.get(Calendar.MINUTE));
        Assert.assertEquals(59, resultCal.get(Calendar.SECOND));
        Assert.assertEquals(999, resultCal.get(Calendar.MILLISECOND));
    }

    @Test
    public void test_getScheduledContextInstancesByFilter_with_multiple_filters() {
        long now = System.currentTimeMillis();

        ScheduledContextInstanceRecord record1 = createContextInstanceRecord("id1", "myContext1", InstanceStatus.COMPLETE);
        record1.setTimestamp(now);
        record1.getContextInstance().setStartTime(now);
        dao.save(record1);

        ScheduledContextInstanceRecord record2 = createContextInstanceRecord("id2", "otherContext", InstanceStatus.ERROR);
        record2.setTimestamp(now);
        dao.save(record2);

        SolrContextInstanceSearchFilterImpl filter = new SolrContextInstanceSearchFilterImpl();
        filter.setContextSearchFilter("myContext");
        filter.setStatus(InstanceStatus.COMPLETE.name());
        filter.setStartTimeStart(now - 1000);
        filter.setStartTimeEnd(now + 1000);

        SearchResults<ScheduledContextInstanceRecord> results = dao.getScheduledContextInstancesByFilter(
            filter, 10, 0, null, null
        );

        Assert.assertEquals(1, results.getResultList().size());
        Assert.assertEquals("id1", results.getResultList().get(0).getContextInstance().getId());
    }

    // Helper methods
    private ScheduledContextInstanceRecord createContextInstanceRecord(String id, String contextName, InstanceStatus status) {
        SolrScheduledContextInstanceRecordImpl record = new SolrScheduledContextInstanceRecordImpl();
        record.setContextName(contextName);
        record.setStatus(status.name());
        record.setTimestamp(System.currentTimeMillis());

        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(id);
        contextInstance.setName(contextName);
        contextInstance.setStartTime(System.currentTimeMillis());
        contextInstance.setEndTime(System.currentTimeMillis() + 1000);
        contextInstance.setContainsRepeatingJobs(false);

        record.setContextInstance(contextInstance);

        return record;
    }
}
