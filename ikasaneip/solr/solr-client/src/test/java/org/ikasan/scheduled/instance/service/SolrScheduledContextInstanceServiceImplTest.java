package org.ikasan.scheduled.instance.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrDryRunParameters;
import org.ikasan.scheduled.event.model.SolrSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.junit.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

public class SolrScheduledContextInstanceServiceImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao;
    private SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao;
    private SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao;

    private ScheduledContextInstanceService service;

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

        this.scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        this.scheduledContextInstanceDao.setSolrClient(this.server);

        this.scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        this.scheduledContextInstanceAuditAggregateDao.setSolrClient(this.server);

        this.scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        this.scheduledContextInstanceAuditDao.setSolrClient(this.server);

        service = new SolrScheduledContextInstanceServiceImpl(this.scheduledContextInstanceDao, this.scheduledContextInstanceAuditDao
            , this.scheduledContextInstanceAuditAggregateDao, true);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfContextInstanceDaoIsNull() {
        service = new SolrScheduledContextInstanceServiceImpl(null, this.scheduledContextInstanceAuditDao
            , scheduledContextInstanceAuditAggregateDao, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfContextInstanceAuditDaoIsNull() {
        service = new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, null
            , scheduledContextInstanceAuditAggregateDao, true);
    }

    @Test
    public void should_not_save_audits_if_flag_not_set() {
        ReflectionTestUtils.setField(service, "saveContextInstanceAuditRecords", Boolean.FALSE);

        ScheduledContextInstanceAuditAggregateRecord record = createAuditRecord();

        SolrContextInstanceImpl previous = new SolrContextInstanceImpl();
        SolrContextInstanceImpl updated = new SolrContextInstanceImpl();

        service.saveAudit(record, previous, updated);

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> allAuditRecords = service.findAllAuditRecords(100, 0, null, null);
        assertEquals(0, allAuditRecords.getResultList().size());
    }

    @Test
    public void test_save_find_audit_record() {
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> allAuditRecords = service.findAllAuditRecords(100, 0, null, null);
        assertEquals(0, allAuditRecords.getResultList().size());

        ScheduledContextInstanceAuditAggregateRecord record = createAuditRecord();

        SolrContextInstanceImpl previous = new SolrContextInstanceImpl();
        SolrContextInstanceImpl updated = new SolrContextInstanceImpl();
        service.saveAudit(record, previous, updated);

        allAuditRecords = service.findAllAuditRecords(100, 1, null, null);
        assertEquals(0, allAuditRecords.getResultList().size());

        allAuditRecords = service.findAllAuditRecords(100, 0, null, null);
        assertEquals(1, allAuditRecords.getResultList().size());

        ScheduledContextInstanceAuditAggregateRecord savedRecord = allAuditRecords.getResultList().get(0);

        ContextualisedScheduledProcessEvent<String, DryRunParameters> processEvent = record.getScheduledContextInstanceAuditAggregate().getProcessEvent();
        List<SchedulerJobInitiationEvent> jobInitiationEvents = record.getScheduledContextInstanceAuditAggregate().getSchedulerJobInitiationEvents();

        validateAuditRecord(processEvent, jobInitiationEvents, record, savedRecord);

        String id = record.getScheduledContextInstanceAuditAggregate().getPreviousContextInstanceAuditId();

        ScheduledContextInstanceRecord scheduledContextInstanceRecord = this.service.findAuditRecordById(id);
        assertNotNull(scheduledContextInstanceRecord);

        savedRecord = allAuditRecords.getResultList().get(0);

        validateAuditRecord(processEvent, jobInitiationEvents, record, savedRecord);

        scheduledContextInstanceRecord = this.service.findById("bad_id");
        assertNull(scheduledContextInstanceRecord);
    }

    @Test
    public void test_find_audit_records_by_filter() {
        ScheduledContextInstanceAuditAggregateRecord record = createAuditRecord();

        SolrContextInstanceImpl previous = new SolrContextInstanceImpl();
        SolrContextInstanceImpl updated = new SolrContextInstanceImpl();
        service.saveAudit(record, previous, updated);

        ScheduledContextInstanceAuditAggregateSearchFilter filter
            = new ScheduledContextInstanceAuditAggregateSearchFilter();

        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextInstanceId("bad");
        Assert.assertEquals(0, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextInstanceId("contextInstanceId");
        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextInstanceId("context");
        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextInstanceId("InstanceId");
        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());

        filter.setContextName("contextName");
        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextName("contextname");
        Assert.assertEquals(0, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextName("Name");
        Assert.assertEquals(1, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextName("CONTEXT");
        Assert.assertEquals(0, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
        filter.setContextName("bad");
        Assert.assertEquals(0, service.findAllAuditRecordsByFilter(filter, 0, 100, null, null).getTotalNumberOfResults());
    }

    private void validateAuditRecord(ContextualisedScheduledProcessEvent<String, DryRunParameters> processEventInstance,
                                     List<SchedulerJobInitiationEvent> jobInitiationEvents,
                                     ScheduledContextInstanceAuditAggregateRecord record,
                                     ScheduledContextInstanceAuditAggregateRecord savedRecord) {

        assertTrue(savedRecord.getId().startsWith("scheduledContextInstanceAuditAggregateId_"));
        // make sure we have an uuid length afterwards
        assertEquals(36, savedRecord.getId().substring("scheduledContextInstanceAuditAggregateId_".length()).length());
        assertEquals(record.getContextName(), savedRecord.getContextName());
        // make sure the timestamp is within the last little bit
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());
        assertEquals(record.getScheduledContextInstanceAuditAggregate().getPreviousContextInstanceAuditId()
            , savedRecord.getScheduledContextInstanceAuditAggregate().getPreviousContextInstanceAuditId());
    }

    @Test
    public void test_save_and_find_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus("RUNNING");
        service.save(scheduledContextRecord);

        ScheduledContextInstanceRecord found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());

        Assert.assertNull(service.findById("bad_id"));
    }

    @Test
    public void test_status_update_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus("WAITING");
        service.save(scheduledContextRecord);

        ScheduledContextInstanceRecord found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("WAITING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());

        found.setStatus("RUNNING");

        service.save(found);

        found = service.findById(contextInstance.getId() + "_scheduledContextInstance");

        Assert.assertEquals(contextInstance.getId() + "_scheduledContextInstance", found.getId());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("contextInstance", found.getContextInstance().getName());
        Assert.assertEquals("RUNNING", found.getStatus());
        Assert.assertEquals(1000000L, found.getTimestamp());
    }

    @Test
    public void test_find_by_status_success() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
        service.save(scheduledContextRecord);

        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ERROR)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RELEASED)).getResultList().size());
        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.COMPLETE)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.ON_HOLD)).getResultList().size());
        Assert.assertEquals(1, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.RUNNING)).getResultList().size());
        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING)).getResultList().size());

        Assert.assertEquals(8, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED)).getResultList().size());
    }

    @Test
    public void test_find_by_status_success_limit_offset() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000000L);
        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
        service.save(scheduledContextRecord);

        Assert.assertEquals(2, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), 2, 0).getResultList().size());

        Assert.assertEquals(8, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), -1, -1).getResultList().size());

        Assert.assertEquals(0, service.getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING
            , InstanceStatus.ERROR, InstanceStatus.COMPLETE, InstanceStatus.ON_HOLD, InstanceStatus.RUNNING, InstanceStatus.RELEASED), 10, 10).getResultList().size());
    }

    @Test
    public void test_find_by_content_name_limit_offset_sort() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000001L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000002L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000003L);
        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000004L);
        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000005L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000006L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000007L);
        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000008L);
        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
        service.save(scheduledContextRecord);

        Assert.assertEquals(2, service.getScheduledContextInstancesByContextName("contextName1", 2, 0, null, null).getResultList().size());
        Assert.assertEquals(4, service.getScheduledContextInstancesByContextName("contextName1", -1, -1, null, null).getResultList().size());

        SearchResults<ScheduledContextInstanceRecord> searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1", -1, -1, SolrDaoBase.CREATED_DATE_TIME, "desc");

        Assert.assertEquals(4, searchResults.getResultList().size());
        Assert.assertEquals(1000004L, searchResults.getResultList().get(0).getTimestamp());

        searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1", -1, -1, SolrDaoBase.CREATED_DATE_TIME, "asc");

        Assert.assertEquals(4, searchResults.getResultList().size());
        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());
    }

    @Test
    public void test_find_by_content_name_limit_offset_sort_timestamp() {

        SolrContextInstanceImpl contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000001L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000002L);
        scheduledContextRecord.setStatus(InstanceStatus.WAITING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000003L);
        scheduledContextRecord.setStatus(InstanceStatus.RUNNING.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName1");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000004L);
        scheduledContextRecord.setStatus(InstanceStatus.ON_HOLD.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000005L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000006L);
        scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000007L);
        scheduledContextRecord.setStatus(InstanceStatus.RELEASED.name());
        service.save(scheduledContextRecord);

        contextInstance = new SolrContextInstanceImpl();
        contextInstance.setName("contextInstance");
        scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
        scheduledContextRecord.setContextName("contextName2");
        scheduledContextRecord.setContextInstance(contextInstance);
        scheduledContextRecord.setTimestamp(1000008L);
        scheduledContextRecord.setStatus(InstanceStatus.ERROR.name());
        service.save(scheduledContextRecord);

        Assert.assertEquals(2, service.getScheduledContextInstancesByContextName("contextName1", 0, 1200001L,2, 0, null, null).getResultList().size());
        Assert.assertEquals(4, service.getScheduledContextInstancesByContextName("contextName1", 0, 1200001L,-1, -1, null, null).getResultList().size());

        SearchResults<ScheduledContextInstanceRecord> searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1", 0, 1200001L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "DESCENDING");

        Assert.assertEquals(4, searchResults.getResultList().size());
        Assert.assertEquals(1000004L, searchResults.getResultList().get(0).getTimestamp());

        searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1",0, 1200001L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "ASCENDING");

        Assert.assertEquals(4, searchResults.getResultList().size());
        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());

        searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1",1000001L, 1000003L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "ASCENDING");

        Assert.assertEquals(3, searchResults.getResultList().size());
        Assert.assertEquals(1000001L, searchResults.getResultList().get(0).getTimestamp());

        searchResults = service.getScheduledContextInstancesByContextName
            ("contextName1",1000001L, 1000003L,-1, -1, SolrDaoBase.CREATED_DATE_TIME, "DESCENDING");

        Assert.assertEquals(3, searchResults.getResultList().size());
        Assert.assertEquals(1000003L, searchResults.getResultList().get(0).getTimestamp());
    }

    @Test
    @Ignore
    public void test_save() throws IOException {
        SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        scheduledContextInstanceDao.setSolrUsername("ikasan");
        scheduledContextInstanceDao.setSolrPassword("1ka5an");
        scheduledContextInstanceDao.initStandalone("http://localhost:8983/solr", 365);

        SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        scheduledContextInstanceAuditDao.setSolrUsername("ikasan");
        scheduledContextInstanceAuditDao.setSolrPassword("1ka5an");
        scheduledContextInstanceAuditDao.initStandalone("http://localhost:8983/solr", 365);

        SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        scheduledContextInstanceAuditAggregateDao.setSolrUsername("ikasan");
        scheduledContextInstanceAuditAggregateDao.setSolrPassword("1ka5an");
        scheduledContextInstanceAuditAggregateDao.initStandalone("http://localhost:8983/solr", 365);

        IntStream.range(0, 1000).forEach(i -> {

        SolrScheduledContextInstanceServiceImpl service = new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, scheduledContextInstanceAuditDao
            , scheduledContextInstanceAuditAggregateDao, true);

            String data = null;
            try {
                data = loadDataFile("/data/contexts/CONTEXT-369160711-with-or-logic.json");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            ContextService contextService = new ContextService();
            ContextInstance contextInstance = null;
            try {
                contextInstance = contextService.getContextInstance(data);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            SolrScheduledContextInstanceRecordImpl scheduledContextRecord = new SolrScheduledContextInstanceRecordImpl();
            scheduledContextRecord.setContextName("CONTEXT-369160711");
            scheduledContextRecord.setContextInstance(contextInstance);
            scheduledContextRecord.setTimestamp(System.currentTimeMillis() - (i * 5000));
            scheduledContextRecord.setStatus(InstanceStatus.COMPLETE.name());

            service.save(scheduledContextRecord);
        });
    }

    private ScheduledContextInstanceAuditAggregateRecord createAuditRecord() {
        ScheduledContextInstanceAuditAggregate audit = new SolrScheduledContextInstanceAuditAggregateImpl();

        ContextualisedScheduledProcessEvent<String, DryRunParameters> processEventInstance = createProcessEvent();
        audit.setProcessEvent(processEventInstance);

        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJobInstance, DryRunParameters> jobInitiationEvent1 = createJobInitiationEvent();
        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJobInstance, DryRunParameters> jobInitiationEvent2 = createJobInitiationEvent();
        SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJobInstance, DryRunParameters> jobInitiationEvent3 = createJobInitiationEvent();
        List<SchedulerJobInitiationEvent> jobInitiationEvents = List.of(jobInitiationEvent1, jobInitiationEvent2, jobInitiationEvent3);
        audit.setSchedulerJobInitiationEvents(jobInitiationEvents);

        ScheduledContextInstanceAuditAggregateRecord record = new SolrScheduledContextInstanceAuditAggregateRecordImpl();
        record.setContextName("contextName");
        record.setContextInstanceId("contextInstanceId");
        record.setScheduledProcessEventName("eventName");
        record.setScheduledContextInstanceAuditAggregate(audit);
        return record;
    }

    private ContextualisedScheduledProcessEvent<String, DryRunParameters> createProcessEvent() {
        ContextualisedScheduledProcessEvent event = new SolrContextualisedScheduledProcessEventImpl();
        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));
        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
        event.setJobGroup("JobGroup " + RandomStringUtils.randomAlphabetic(5));
        event.setJobDescription("JobDescription " + RandomStringUtils.randomAlphabetic(5));
        event.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        event.setReturnCode(0);
        event.setSuccessful(true);
        event.setOutcome("Outcome: " + RandomStringUtils.randomAlphabetic(5));
        event.setResultOutput("resultOutput " + RandomStringUtils.randomAlphabetic(5));
        event.setResultError("resultError " + RandomStringUtils.randomAlphabetic(5));
        event.setPid(78321);
        event.setUser("User " + RandomStringUtils.randomAlphabetic(5));
        event.setFireTime(System.currentTimeMillis());
        event.setNextFireTime(System.currentTimeMillis() + 1000);
        event.setCompletionTime(System.currentTimeMillis() + 2000);
        event.setDryRun(true);
        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
        event.setChildContextIds(List.of("childContextId1", "childContextId2"));
        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));
        event.setJobStarting(true);

        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
        event.setDryRunParameters(dryRunParams);

        event.setSkipped(false);

        SolrInternalEventDrivenJobInstanceImpl internalEventDrivenJob = new SolrInternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("Code1", "Code2"));
        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);

        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));

        event.setInternalEventDrivenJob(internalEventDrivenJob);

        return event;
    }

    private SchedulerJobInitiationEvent<ContextParameterInstance, InternalEventDrivenJobInstance, DryRunParameters> createJobInitiationEvent() {
        SchedulerJobInitiationEvent event = new SolrSchedulerJobInitiationEventImpl();
        event.setAgentName("Agent " + RandomStringUtils.randomAlphabetic(5));
        event.setAgentUrl("AgentUrl " + RandomStringUtils.randomAlphabetic(5));
        event.setJobName("Job " + RandomStringUtils.randomAlphabetic(5));

        InternalEventDrivenJobInstance internalEventDrivenJob = new SolrInternalEventDrivenJobInstanceImpl();
        internalEventDrivenJob.setSuccessfulReturnCodes(List.of("EventCode1", "EventCode2"));
        internalEventDrivenJob.setWorkingDirectory("workingDirectory/" + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setCommandLine("commandLine " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setMinExecutionTime(System.currentTimeMillis());
        internalEventDrivenJob.setMaxExecutionTime(System.currentTimeMillis() + 3000);

        SolrContextParameterImpl contextParameter = new SolrContextParameterImpl();
        contextParameter.setType("type " + RandomStringUtils.randomAlphabetic(5));
        contextParameter.setName("name " + RandomStringUtils.randomAlphabetic(5));
        internalEventDrivenJob.setContextParameters(List.of(contextParameter));
        internalEventDrivenJob.setDaysOfWeekToRun(List.of(1, 2, 3, 4, 5));
        event.setInternalEventDrivenJob(internalEventDrivenJob);

        event.setContextId("contextId " + RandomStringUtils.randomAlphabetic(5));
        event.setChildContextIds(List.of("EventChildContextId1", "EventChildContextId2"));
        event.setContextInstanceId("contextInstanceId " + RandomStringUtils.randomAlphabetic(5));

        SolrContextParameterInstanceImpl contextParameter1 = new SolrContextParameterInstanceImpl();
        contextParameter1.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
        contextParameter1.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
        SolrContextParameterInstanceImpl contextParameter2 = new SolrContextParameterInstanceImpl();
        contextParameter2.setType("type1 " + RandomStringUtils.randomAlphabetic(5));
        contextParameter2.setName("name1 " + RandomStringUtils.randomAlphabetic(5));
        event.setContextParameters(List.of(contextParameter1, contextParameter2));

        event.setDryRun(true);
        SolrDryRunParameters dryRunParams = new SolrDryRunParameters();
        event.setDryRunParameters(dryRunParams);

        event.setSkipped(false);

        return event;
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}