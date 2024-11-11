package org.ikasan.scheduled.visualisation.dao;

import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.context.model.SolrContextTemplateImpl;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.scheduled.visualisation.model.SolrContextVisualisationLayoutImpl;
import org.ikasan.scheduled.visualisation.model.SolrContextVisualisationLayoutRecordImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayout;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayoutRecord;
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
import java.util.List;

import static org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl.SCHEDULED_CONTEXT;
import static org.ikasan.scheduled.visualisation.dao.SolrContextVisualisationLayoutDaoImpl.CONTEXT_VISUALISATION_LAYOUT;

public class SolrContextVisualisationLayoutDaoTest extends SolrTestCaseJ4 {

    private SolrContextVisualisationLayoutDaoImpl dao;

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

        dao = new SolrContextVisualisationLayoutDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            ContextVisualisationLayout solrContextVisualisationLayout = new SolrContextVisualisationLayoutImpl();
            solrContextVisualisationLayout.setLayoutJson("layout");
            SolrContextVisualisationLayoutRecordImpl solrContextVisualisationLayoutRecord = new SolrContextVisualisationLayoutRecordImpl();
            solrContextVisualisationLayoutRecord.setParentContext("parentContext");
            solrContextVisualisationLayoutRecord.setContext("context");
            solrContextVisualisationLayoutRecord.setTimestamp(1000000L);
            solrContextVisualisationLayoutRecord.setContextVisualisationLayout(solrContextVisualisationLayout);

            this.dao.save(solrContextVisualisationLayoutRecord);

            ContextVisualisationLayoutRecord found = this.dao.findById(CONTEXT_VISUALISATION_LAYOUT + "_"
                + solrContextVisualisationLayoutRecord.getParentContext() + "_" + solrContextVisualisationLayoutRecord.getContext());

            Assert.assertNotNull(found);

            Assert.assertEquals("contextVisualisationLayout_parentContext_context", found.getId());
            Assert.assertEquals("parentContext", found.getParentContext());
            Assert.assertEquals("context", found.getContext());
            Assert.assertEquals("layout", found.getContextVisualisationLayout().getLayoutJson());
            Assert.assertEquals(1000000L, found.getTimestamp());

            ContextVisualisationLayout contextVisualisationLayout = found.getContextVisualisationLayout();
            contextVisualisationLayout.setLayoutJson("new layout!");
            found.setContextVisualisationLayout(contextVisualisationLayout);
            this.dao.save(found);

            found = this.dao.findById(CONTEXT_VISUALISATION_LAYOUT + "_"
            + solrContextVisualisationLayoutRecord.getParentContext() + "_" + solrContextVisualisationLayoutRecord.getContext());

            Assert.assertEquals("contextVisualisationLayout_parentContext_context", found.getId());
            Assert.assertEquals("parentContext", found.getParentContext());
            Assert.assertEquals("context", found.getContext());
            Assert.assertEquals("new layout!", found.getContextVisualisationLayout().getLayoutJson());
            Assert.assertEquals(1000000L, found.getTimestamp());

            Assert.assertNull(this.dao.findById("bad_id"));
        }
    }

//    @Test
//    public void test_find_all() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrContextTemplateImpl solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName1");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            solrContextTemplate = new SolrContextTemplateImpl();
//            solrContextTemplate.setName("contextName2");
//            scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName2");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            SearchResults<ScheduledContextRecord> found =  this.dao.findAll();
//
//            Assert.assertEquals(2, found.getResultList().size());
//        }
//    }
//
//    @Test
//    public void test_find_all_limit_offset() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrContextTemplateImpl solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName1");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            solrContextTemplate = new SolrContextTemplateImpl();
//            solrContextTemplate.setName("contextName2");
//            scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName2");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            SearchResults<ScheduledContextRecord> found =  this.dao.findAll(100, 0);
//
//            Assert.assertEquals(2, found.getResultList().size());
//
//            found =  this.dao.findAll(-1, -1);
//
//            Assert.assertEquals(2, found.getResultList().size());
//
//            found =  this.dao.findAll(0, 0);
//
//            Assert.assertEquals(0, found.getResultList().size());
//        }
//    }
//
//    @Test
//    public void test_find_by_name() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrContextTemplateImpl solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("Context-Locks-1");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//            solrContextTemplate.setName("contextName2");
//            scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName2");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            ScheduledContextRecord found = this.dao.findByName("Context-Locks-1");
//
//            Assert.assertEquals("Context-Locks-1", found.getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getContext().getName());
//        }
//    }
//
//    @Test
//    public void test_delete_context() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrContextTemplateImpl solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("Context-Locks-1");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//            solrContextTemplate.setName("contextName2");
//            scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName2");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            ScheduledContextRecord found = this.dao.findByName("Context-Locks-1");
//
//            Assert.assertEquals("Context-Locks-1", found.getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getContext().getName());
//
//            this.dao.deleteContext("Context-Locks-1");
//
//            found = this.dao.findByName("Context-Locks-1");
//
//            Assert.assertNull(found);
//        }
//    }
//
//    @Test
//    public void test_find_by_filter() throws Exception {
//
//        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
//        {
//            init(server);
//
//            SolrContextTemplateImpl solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//
//            SolrScheduledContextRecordImpl scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("Context-Locks-1");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            solrContextTemplate
//                = ScheduledObjectMapperFactory.newInstance()
//                .readValue(loadDataFile("/data/context-with-different-job-locks-1.json").getBytes(), SolrContextTemplateImpl.class);
//            solrContextTemplate.setName("contextName2");
//            scheduledContextRecord = new SolrScheduledContextRecordImpl();
//            scheduledContextRecord.setContextName("contextName2");
//            scheduledContextRecord.setTimestamp(1000000L);
//            scheduledContextRecord.setContext(solrContextTemplate);
//            this.dao.save(scheduledContextRecord);
//
//            ScheduledContextSearchFilterImpl filter = new ScheduledContextSearchFilterImpl();
//            filter.setContextName("Context-Locks-1");
//
//            SearchResults<ScheduledContextRecord> found = this.dao.findByFilter(filter, 100, 0, null, null);
//
//            Assert.assertEquals(1, found.getResultList().size());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
//
//            filter.setContextName("Context-Loc");
//            found = this.dao.findByFilter(filter, 100, 0, null, null);
//
//            Assert.assertEquals(1, found.getResultList().size());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
//
//            filter.setContextName("xt-Locks-1");
//            found = this.dao.findByFilter(filter, 100, 0, null, null);
//
//            Assert.assertEquals(1, found.getResultList().size());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
//
//            filter.setContextName(null);
//            filter.setContextNames(List.of("Context-Locks-1", "Context-Locks-2", "Context-Locks-3"));
//
//            found = this.dao.findByFilter(filter, 100, 0, null, null);
//
//            Assert.assertEquals(1, found.getResultList().size());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContextName());
//            Assert.assertEquals("Context-Locks-1", found.getResultList().get(0).getContext().getName());
//        }
//    }

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
