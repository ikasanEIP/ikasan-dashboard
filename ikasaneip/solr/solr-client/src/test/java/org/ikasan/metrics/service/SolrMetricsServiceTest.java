package org.ikasan.metrics.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.metrics.dao.SolrMetricsDao;
import org.ikasan.metrics.model.ComponentInvocationMetricImpl;
import org.ikasan.metrics.model.CustomMetric;
import org.ikasan.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.metrics.model.MetricEventImpl;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.solr.SolrDaoBase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrMetricsServiceTest extends SolrTestCaseJ4
{
    /**
     * Mockery for mocking concrete classes
     */
    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private ObjectMapper mapper;

    private SolrClient server = mockery.mock(SolrClient.class);

    private SolrMetricsDao dao;

    private NodeConfig config;

    private  Path tmppath;

    @Before
    public void setup()
    {

        tmppath = createTempDir();

        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();

        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        dao = new SolrMetricsDao();
        dao.setSolrClient(server);
    }


    @Test
    @DirtiesContext
    public void test_get_results_within_timeframe() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            dao = new SolrMetricsDao();
            dao.setSolrClient(server);

            SolrMetricsServiceImpl solrMetricsService = new SolrMetricsServiceImpl(dao);

            solrMetricsService.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = solrMetricsService.getMetrics(10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(5, results.size());
        }
    }

    @Test
    @DirtiesContext
    public void test_get_results_by_module_name_within_timeframe() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            dao = new SolrMetricsDao();
            dao.setSolrClient(server);

            SolrMetricsServiceImpl solrMetricsService = new SolrMetricsServiceImpl(dao);

            solrMetricsService.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = solrMetricsService.getMetrics("My Module", 10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(5, results.size());


            results = solrMetricsService.getMetrics("Bad module name", 10000000L
            , System.currentTimeMillis() + 10000000L);

            assertEquals(0, results.size());
        }
    }

    @Test
    @DirtiesContext
    public void test_get_results_by_module_name_flow_name_within_timeframe() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            dao = new SolrMetricsDao();
            dao.setSolrClient(server);

            SolrMetricsServiceImpl solrMetricsService = new SolrMetricsServiceImpl(dao);

            solrMetricsService.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = solrMetricsService.getMetrics("My Module", "Trade Consumer Flow", 10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(5, results.size());

            results = solrMetricsService.getMetrics("Bad module name", "Bad flow name", 10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(0, results.size());
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
