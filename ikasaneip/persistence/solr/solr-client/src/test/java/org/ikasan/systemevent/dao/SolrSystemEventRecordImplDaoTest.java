package org.ikasan.systemevent.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.systemevent.model.SolrSystemEventRecordImpl;
import org.ikasan.systemevent.model.SolrSystemEventSearchFilter;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by Ikasan Development on 29/08/2017.
 */
public class SolrSystemEventRecordImplDaoTest extends SolrTestCaseJ4
{
    /**
     * Mockery for mocking concrete classes
     */
    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private SolrClient server = mockery.mock(SolrClient.class);

    private NodeConfig config;

    private SolrSystemEventDaoImpl dao;

    @Before
    public void setup()
    {
        config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();


    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrSystemEventDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_save_and_find_success() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            SolrSystemEventRecordImpl systemEvent = new SolrSystemEventRecordImpl();
            systemEvent.setModuleName("moduleName");
            systemEvent.setTimestampLong(System.currentTimeMillis());
            systemEvent.setId("1");
            systemEvent.setActor("actor");
            systemEvent.setSubject("subject");
            systemEvent.setAction("action");

            dao.save(systemEvent);

            SystemEvent found = this.dao.findById("moduleName-systemEvent-1");

            Assert.assertNotNull(found);
            Assert.assertEquals("moduleName", found.getModuleName());
            Assert.assertEquals(systemEvent.getTimestampLong(), ((SolrSystemEventRecordImpl)found).getTimestampLong());
            Assert.assertEquals("actor", found.getActor());
            Assert.assertEquals("subject", found.getSubject());
            Assert.assertTrue(((SolrSystemEventRecordImpl) found).getExpiryLong() > ((SolrSystemEventRecordImpl) found).getTimestampLong());
            Assert.assertEquals("moduleName", found.getModuleName());
            Assert.assertEquals("action", found.getAction());
            Assert.assertNotNull(((SolrSystemEventRecordImpl) found).getPayload());
        }
    }

    @Test
    public void test_find_by_filter() throws Exception {

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            List<SystemEvent> systemEvents = new ArrayList<>();
            IntStream.range(0, 1000).forEach(i -> {
                SolrSystemEventRecordImpl systemEvent = new SolrSystemEventRecordImpl();
                systemEvent.setModuleName("moduleName"+i);
                systemEvent.setTimestampLong(System.currentTimeMillis());
                systemEvent.setId(Integer.toString(i));
                systemEvent.setActor("the actor "+i);
                systemEvent.setSubject("the subject "+i);
                systemEvent.setAction("the action "+i);

                systemEvents.add(systemEvent);
            });

            dao.save(systemEvents);

            SystemEventSearchFilter filter = new SolrSystemEventSearchFilter();
            filter.setAction("action");

            SearchResults<SystemEvent> found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1000, found.getResultList().size());
            Assert.assertEquals(1000, found.getTotalNumberOfResults());

            found = this.dao.findByFilter(filter, 0, 0, null, null);
            Assert.assertEquals(0, found.getResultList().size());
            Assert.assertEquals(1000, found.getTotalNumberOfResults());

            filter = new SolrSystemEventSearchFilter();
            filter.setAction("ACTION");

            found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1000, found.getResultList().size());
            Assert.assertEquals(1000, found.getTotalNumberOfResults());

            filter = new SolrSystemEventSearchFilter();
            filter.setAction("action");
            filter.setActor("actor");
            filter.setSubject("subject");

            found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1000, found.getResultList().size());
            Assert.assertEquals(1000, found.getTotalNumberOfResults());

            filter = new SolrSystemEventSearchFilter();
            filter.setAction("action");
            filter.setActor("actor");
            filter.setSubject("subject 111");

            found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals(1, found.getTotalNumberOfResults());

            filter = new SolrSystemEventSearchFilter();
            filter.setAction("ACTION");
            filter.setActor("ACTOR");
            filter.setSubject("subject 111");

            found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals(1, found.getTotalNumberOfResults());

            filter = new SolrSystemEventSearchFilter();
            filter.setAction("ACTION");
            filter.setActor("actor");
            filter.setSubject("subject 111");
            filter.setStartTime(10000L);
            filter.setEndTime(System.currentTimeMillis() + 10000000L);

            found = this.dao.findByFilter(filter, 10000, 0, null, null);
            Assert.assertEquals(1, found.getResultList().size());
            Assert.assertEquals(1, found.getTotalNumberOfResults());
        }
    }


    @Test(expected = RuntimeException.class)
    @DirtiesContext
    public void test_save_exception() throws Exception
    {
        mockery.checking(new Expectations()
        {
            {
                // set event factory
                oneOf(server).request(with(any(UpdateRequest.class)));
                will(throwException(new RuntimeException("Error")));

            }
        });

        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        dao.setSolrClient(server);

        SolrSystemEventRecordImpl systemEvent = new SolrSystemEventRecordImpl();
        systemEvent.setModuleName("moduleName");
        systemEvent.setTimestampLong(System.currentTimeMillis());
        systemEvent.setExpiryLong(0);
        systemEvent.setId("1");
        systemEvent.setActor("actor");
        systemEvent.setSubject("subject");

        dao.save(systemEvent);
    }


    public static String TEST_HOME()
    {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH()
    {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
