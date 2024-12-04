package org.ikasan.scheduled.context.service;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.dao.SolrScheduledContextViewDaoImpl;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.context.model.SolrContextTemplateImpl;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl.SCHEDULED_CONTEXT;

public class SolrScheduledContextServiceImplTest extends SolrTestCaseJ4 {

    private SolrScheduledContextDaoImpl dao;

    private SolrScheduledContextServiceImpl scheduledContextService;

    private NodeConfig config;

    private Path tmppath;

    @Before
    public void setup()
    {
        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();

    }

    @After
    public void teardown() throws IOException
    {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrScheduledContextDaoImpl();
        dao.setSolrClient(server);

        SolrScheduledContextViewDaoImpl contextViewDao = new SolrScheduledContextViewDaoImpl();
        contextViewDao.setSolrClient(server);

        this.scheduledContextService = new SolrScheduledContextServiceImpl(dao, contextViewDao);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            solrContextTemplate.setJobLocks(List.of(new SolrJobLockImpl()));
            solrContextTemplate.setUserGeneratedLayout("user generated layout");
            solrContextTemplate.setUseAutoLayout(false);
            solrContextTemplate.setDisabled(false);
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);

            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals("user generated layout", found.getContext().getUserGeneratedLayout());
            Assert.assertEquals(false, found.getContext().isUseAutoLayout());
            Assert.assertEquals(false, found.isDisabled());
            Assert.assertEquals(false, found.isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(false, found.getContext().isDisabled());
            Assert.assertEquals(1000000L, found.getTimestamp());

            ContextTemplate contextTemplate = found.getContext();
            contextTemplate.setDisabled(true);
            contextTemplate.setQuartzScheduleDrivenJobsDisabledForContext(true);
            contextTemplate.setUserGeneratedLayout("updated user generated layout");
            contextTemplate.setUseAutoLayout(true);
            scheduledContextRecord.setContext(contextTemplate);
            this.dao.save(scheduledContextRecord);

            found = this.dao.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals("updated user generated layout", found.getContext().getUserGeneratedLayout());
            Assert.assertEquals(true, found.getContext().isUseAutoLayout());
            Assert.assertEquals(true, found.isDisabled());
            Assert.assertEquals(true, found.isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(true, found.getContext().isDisabled());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

    @Test
    public void test_disable_scheduled_jobs() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            solrContextTemplate.setJobLocks(List.of(new SolrJobLockImpl()));
            solrContextTemplate.setDisabled(false);
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);

            this.scheduledContextService.save(scheduledContextRecord);

            this.scheduledContextService.disableScheduledJobs(solrContextTemplate, "user");

            ScheduledContextRecord found = this.scheduledContextService.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals(false, found.isDisabled());
            Assert.assertEquals(true, found.isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(true, found.getContext().isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(false, found.getContext().isDisabled());
            Assert.assertEquals("user", found.getModifiedBy());
            Assert.assertEquals(1000000L, found.getTimestamp());
        }
    }

    @Test
    public void test_enable_scheduled_jobs() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            solrContextTemplate.setJobLocks(List.of(new SolrJobLockImpl()));
            solrContextTemplate.setDisabled(false);
            solrContextTemplate.setQuartzScheduleDrivenJobsDisabledForContext(true);
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);

            this.scheduledContextService.save(scheduledContextRecord);

            ScheduledContextRecord found = this.scheduledContextService.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals(false, found.isDisabled());
            Assert.assertEquals(true, found.isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(true, found.getContext().isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(false, found.getContext().isDisabled());
            Assert.assertEquals(1000000L, found.getTimestamp());

            this.scheduledContextService.enableScheduledJobs(solrContextTemplate, "user");

            found = this.scheduledContextService.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT, found.getId());
            Assert.assertEquals("contextName", found.getContextName());
            Assert.assertEquals("contextName", found.getContext().getName());
            Assert.assertEquals(false, found.isDisabled());
            Assert.assertEquals(false, found.isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(false, found.getContext().isQuartzScheduleDrivenJobsDisabledForContext());
            Assert.assertEquals(false, found.getContext().isDisabled());
            Assert.assertEquals("user", found.getModifiedBy());
            Assert.assertEquals(1000000L, found.getTimestamp());
        }
    }

    @Test
    public void test_find_all() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            SearchResults<? extends ScheduledContextRecord> found =  this.scheduledContextService.findAll();

            Assert.assertEquals(2, found.getResultList().size());
        }
    }

    @Test
    public void test_find_all_limit_offset() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            SearchResults<? extends ScheduledContextRecord> found =  this.scheduledContextService.findAll(100, 0);

            Assert.assertEquals(2, found.getResultList().size());

            found =  this.scheduledContextService.findAll(-1, -1);

            Assert.assertEquals(2, found.getResultList().size());

            found =  this.scheduledContextService.findAll(0, 0);

            Assert.assertEquals(0, found.getResultList().size());
        }
    }

    @Test
    public void test_find_by_name() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("Context-Locks-1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            ScheduledContextRecord found = this.scheduledContextService.findByName("Context-Locks-1");

            Assert.assertEquals("Context-Locks-1", found.getContextName());
            Assert.assertEquals("Context-Locks-1", found.getContext().getName());
        }
    }

    @Test
    public void test_delete_context() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("Context-Locks-1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findByName("Context-Locks-1");

            Assert.assertEquals("Context-Locks-1", found.getContextName());
            Assert.assertEquals("Context-Locks-1", found.getContext().getName());

            this.scheduledContextService.deleteContext("Context-Locks-1");

            found = this.scheduledContextService.findByName("Context-Locks-1");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_find_by_filter() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("Context-Locks-1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextName("Context-Locks-1");

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("Context-Loc");
            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("xt-Locks-1");
            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName(null);
            filter.setContextNames(List.of("Context-Locks-1", "Context-Locks-2", "Context-Locks-3"));

            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
        }
    }

    @Test
    @Ignore
    public void test_find_by_filter_case_insensitive() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);

            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("Context-Locks-1");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextName("context-locks-1");

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("context-loc");
            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("xt-locks-1");
            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName(null);
            filter.setContextNames(List.of("Context-Locks-1", "Context-Locks-2", "Context-Locks-3"));

            found = this.scheduledContextService.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
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
