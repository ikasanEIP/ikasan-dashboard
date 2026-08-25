package org.ikasan.scheduled.context.dao;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.context.model.SolrContextTemplateImpl;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.ikasan.spec.scheduled.context.dao.ScheduledContextDao.SCHEDULED_CONTEXT_TYPE;

public class SolrScheduledContextDaoTest extends SolrTestCaseJ4 {

    private SolrScheduledContextDaoImpl dao;

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
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            List<JobLock> jobLocks = new ArrayList<>();
            JobLock jobLock = new SolrJobLockImpl();
            jobLocks.add(jobLock);
            solrContextTemplate.setJobLocks(jobLocks);
            solrContextTemplate.setUserGeneratedLayout("user generated layout");
            solrContextTemplate.setUseAutoLayout(false);
            solrContextTemplate.setDisabled(false);
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);

            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findById("contextName");

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT_TYPE, found.getId());
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

            Assert.assertEquals("contextName-" + SCHEDULED_CONTEXT_TYPE, found.getId());
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
            this.dao.save(scheduledContextRecord);

            solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            SearchResults<ScheduledContextRecord> found =  this.dao.findAll();

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
            this.dao.save(scheduledContextRecord);

            solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            SearchResults<ScheduledContextRecord> found =  this.dao.findAll(100, 0);

            Assert.assertEquals(2, found.getResultList().size());

            found =  this.dao.findAll(-1, -1);

            Assert.assertEquals(2, found.getResultList().size());

            found =  this.dao.findAll(0, 0);

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
            this.dao.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findByName("Context-Locks-1");

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
            this.dao.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findByName("Context-Locks-1");

            Assert.assertEquals("Context-Locks-1", found.getContextName());
            Assert.assertEquals("Context-Locks-1", found.getContext().getName());

            this.dao.deleteContext("Context-Locks-1");

            found = this.dao.findByName("Context-Locks-1");

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
            this.dao.save(scheduledContextRecord);

            solrContextTemplate
                = ScheduledObjectMapperFactory.newInstance()
                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
            solrContextTemplate.setName("contextName2");
            scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName2");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextName("Context-Locks-1");

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("Context-Loc");
            found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName("xt-Locks-1");
            found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());

            filter.setContextName(null);
            filter.setContextNames(List.of("Context-Locks-1", "Context-Locks-2", "Context-Locks-3"));

            found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
        }
    }

    @Test
    public void test_find_by_filter_with_sorting_ascending() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            for(int i = 0; i < 5; i++) {
                SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
                solrContextTemplate.setName("context" + i);
                SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName("context" + i);
                scheduledContextRecord.setTimestamp(1000000L + i);
                scheduledContextRecord.setContext(solrContextTemplate);
                this.dao.save(scheduledContextRecord);
            }

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, "timestamp", "ASCENDING");

            Assert.assertEquals(5, found.getResultList().size());
            Assert.assertEquals("context0", found.getResultList().get(0).getContextName());
        }
    }

    @Test
    public void test_find_by_filter_with_sorting_descending() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            for(int i = 0; i < 5; i++) {
                SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
                solrContextTemplate.setName("context" + i);
                SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName("context" + i);
                scheduledContextRecord.setTimestamp(1000000L + i);
                scheduledContextRecord.setContext(solrContextTemplate);
                this.dao.save(scheduledContextRecord);
            }

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, "timestamp", "DESCENDING");

            Assert.assertEquals(5, found.getResultList().size());
            Assert.assertEquals("context4", found.getResultList().get(0).getContextName());
        }
    }

    @Test
    public void test_find_by_filter_with_pagination() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            for(int i = 0; i < 10; i++) {
                SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
                solrContextTemplate.setName("context" + i);
                SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName("context" + i);
                scheduledContextRecord.setTimestamp(1000000L + i);
                scheduledContextRecord.setContext(solrContextTemplate);
                this.dao.save(scheduledContextRecord);
            }

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            SearchResults<ScheduledContextRecord> page1 = this.dao.findByFilter(filter, 5, 0, null, null);

            Assert.assertEquals(5, page1.getResultList().size());
            Assert.assertEquals(10, page1.getTotalNumberOfResults());

            SearchResults<ScheduledContextRecord> page2 = this.dao.findByFilter(filter, 5, 5, null, null);

            Assert.assertEquals(5, page2.getResultList().size());
            Assert.assertEquals(10, page2.getTotalNumberOfResults());
        }
    }

    @Test
    public void test_save_with_modified_by() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            scheduledContextRecord.setModifiedBy("testUser");

            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findById("contextName");

            Assert.assertNotNull(found);
            Assert.assertEquals("testUser", found.getModifiedBy());
        }
    }

    @Test
    public void test_find_by_name_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextRecord found = this.dao.findByName("nonExistentContext");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_find_by_filter_with_context_name_containing_spaces() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("context with spaces");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("context with spaces");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextName("context with spaces");

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals("context with spaces", found.getResultList().get(0).getContextName());
        }
    }

    @Test
    public void test_find_by_filter_with_multiple_context_names() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            for(int i = 0; i < 5; i++) {
                SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
                solrContextTemplate.setName("context" + i);
                SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName("context" + i);
                scheduledContextRecord.setTimestamp(1000000L + i);
                scheduledContextRecord.setContext(solrContextTemplate);
                this.dao.save(scheduledContextRecord);
            }

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextNames(List.of("context1", "context3"));

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(2, found.getResultList().size());
        }
    }

    @Test
    public void test_find_by_filter_empty_results() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrContextTemplateImpl solrContextTemplate = new SolrContextTemplateImpl();
            solrContextTemplate.setName("contextName");
            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
            scheduledContextRecord.setContextName("contextName");
            scheduledContextRecord.setTimestamp(1000000L);
            scheduledContextRecord.setContext(solrContextTemplate);
            this.dao.save(scheduledContextRecord);

            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
            filter.setContextName("nonExistent");

            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);

            Assert.assertEquals(0, found.getResultList().size());
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
