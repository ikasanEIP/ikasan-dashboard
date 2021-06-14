package org.ikasan.metrics.dao;

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
import org.ikasan.metrics.model.ComponentInvocationMetricImpl;
import org.ikasan.metrics.model.CustomMetric;
import org.ikasan.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.metrics.model.MetricEventImpl;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.wiretap.dao.SolrWiretapDao;
import org.ikasan.wiretap.model.SolrWiretapEvent;
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
public class SolrMetricsDaoTest extends SolrTestCaseJ4
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

        SolrResourceLoader loader = new SolrResourceLoader(tmppath);
        config = new NodeConfig.NodeConfigBuilder("testnode", loader)
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

        SolrMetricsDao dao = new SolrMetricsDao();
        dao.setSolrClient(server);
        dao.setDaysToKeep(0);

        ComponentInvocationMetricImpl componentInvocationMetric = new ComponentInvocationMetricImpl();
        componentInvocationMetric.setComponentName("my-component");
        componentInvocationMetric.setStartTimeMillis(1000l);
        componentInvocationMetric.setEndTimeMillis(2000l);
        MetricEventImpl metricEvent = new MetricEventImpl();
        metricEvent.setComponentName("my-component");
        metricEvent.setFlowName("my-flow");
        metricEvent.setEvent("event payload");
        metricEvent.setTimestamp(1001l);
        componentInvocationMetric.setWiretapFlowEvent(metricEvent);
        CustomMetric customMetric = new CustomMetric();
        customMetric.setName("name");
        customMetric.setValue("value");
        componentInvocationMetric.setMetrics(Set.of(customMetric));

        FlowInvocationMetricImpl event = new FlowInvocationMetricImpl();
        event.setModuleName("module-name");
        event.setFlowName("my-flow");
        event.setInvocationStartTime(1000l);
        event.setInvocationEndTime(2000l);
        event.setFlowInvocationEvents(Set.of(componentInvocationMetric));

        dao.save(event);
    }

    @Test
    public void test_convert_entity_to_solr_input_document() throws IOException, JSONException {
        SolrMetricsDao dao = new SolrMetricsDao();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<FlowInvocationMetricImpl> event = objectMapper.readValue(loadDataFile("/data/flowInvocationMetrics.json")
            , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

        SolrInputDocument solrInputDocument = dao.convertEntityToSolrInputDocument(1L, event.get(0));

        Assert.assertEquals("My Module", solrInputDocument.getFieldValue(SolrDaoBase.MODULE_NAME));
        Assert.assertEquals("metric", solrInputDocument.getFieldValue(SolrDaoBase.TYPE));
        Assert.assertEquals("Trade Consumer Flow", solrInputDocument.getFieldValue(SolrDaoBase.FLOW_NAME));
        JSONAssert.assertEquals("{\"id\":24271,\"moduleName\":\"My Module\",\"flowName\":\"Trade Consumer Flow\",\"invocationStartTime\":1623325830486,\"invocationEndTime\":1623325830496,\"finalAction\":\"PUBLISH\",\"componentInvocationMetricImpls\":[{\"componentName\":\"JMS Producer\",\"beforeEventIdentifier\":\"-1996324143\",\"beforeRelatedEventIdentifier\":null,\"afterEventIdentifier\":\"-1996324143\",\"afterRelatedEventIdentifier\":null,\"startTimeMillis\":1623325830491,\"endTimeMillis\":1623325830496,\"id\":561,\"metrics\":[],\"wiretapFlowEvent\":{\"identifier\":562,\"event\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><bdm><trade><trade-id>88634252</trade-id><asset><isin>XS2201078503</isin></asset><trade-details><nominal>895715</nominal><price>103.554</price><amount>7.7193923646E7</amount><currency>USD</currency><trade-date-time>2021-06-10T12:50:30</trade-date-time></trade-details><book>GBP_GOVT</book><party-details><counterparty>ACME Corporation</counterparty></party-details></trade></bdm>\",\"moduleName\":\"My Module\",\"flowName\":\"Trade Consumer Flow\",\"componentName\":\"JMS Producer\",\"timestamp\":1623325830491,\"expiry\":1623325830491,\"eventId\":\"-1996324143\",\"relatedEventId\":null}},{\"componentName\":\"JMS Consumer\",\"beforeEventIdentifier\":\"-1996324143\",\"beforeRelatedEventIdentifier\":null,\"afterEventIdentifier\":\"-1996324143\",\"afterRelatedEventIdentifier\":null,\"startTimeMillis\":1623325830486,\"endTimeMillis\":1623325830487,\"id\":562,\"metrics\":[],\"wiretapFlowEvent\":{\"identifier\":561,\"event\":\"ActiveMQTextMessage {commandId = 5, responseRequired = false, messageId = ID:Michaels-MacBook-Pro.local-55556-1623243068313-7:24490:1:1:1, originalDestination = null, originalTransactionId = null, producerId = ID:Michaels-MacBook-Pro.local-55556-1623243068313-7:24490:1:1, destination = queue://com.caixa.bank.murex.out, transactionId = null, expiration = 0, timestamp = 1623325830485, arrival = 0, brokerInTime = 1623325830486, brokerOutTime = 1623325830486, correlationId = null, replyTo = null, persistent = true, type = null, priority = 4, groupID = null, groupSequence = 0, targetConsumerId = null, compressed = false, userID = null, content = org.apache.activemq.util.ByteSequence@5e5e1ee4, marshalledProperties = org.apache.activemq.util.ByteSequence@60f3cba8, dataStructure = null, redeliveryCounter = 0, size = 0, properties = {IkasanEventLifeIdentifier=-1996324143}, readOnlyProperties = true, readOnlyBody = true, droppable = false, jmsXGroupFirstForConsumer = false, text = <?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalo...rade></mxml>}\",\"moduleName\":\"My Module\",\"flowName\":\"Trade Consumer Flow\",\"componentName\":\"JMS Consumer\",\"timestamp\":1623325830486,\"expiry\":1623325830486,\"eventId\":\"-1996324143\",\"relatedEventId\":null}}],\"harvested\":true,\"expiry\":1623930630497,\"errorUri\":null,\"harvestedDateTime\":1623325840029,\"flowInvocationEvents\":[{\"componentName\":\"JMS Producer\",\"beforeEventIdentifier\":\"-1996324143\",\"beforeRelatedEventIdentifier\":null,\"afterEventIdentifier\":\"-1996324143\",\"afterRelatedEventIdentifier\":null,\"startTimeMillis\":1623325830491,\"endTimeMillis\":1623325830496,\"id\":561,\"metrics\":[],\"wiretapFlowEvent\":{\"identifier\":562,\"event\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><bdm><trade><trade-id>88634252</trade-id><asset><isin>XS2201078503</isin></asset><trade-details><nominal>895715</nominal><price>103.554</price><amount>7.7193923646E7</amount><currency>USD</currency><trade-date-time>2021-06-10T12:50:30</trade-date-time></trade-details><book>GBP_GOVT</book><party-details><counterparty>ACME Corporation</counterparty></party-details></trade></bdm>\",\"moduleName\":\"My Module\",\"flowName\":\"Trade Consumer Flow\",\"componentName\":\"JMS Producer\",\"timestamp\":1623325830491,\"expiry\":1623325830491,\"eventId\":\"-1996324143\",\"relatedEventId\":null}},{\"componentName\":\"JMS Consumer\",\"beforeEventIdentifier\":\"-1996324143\",\"beforeRelatedEventIdentifier\":null,\"afterEventIdentifier\":\"-1996324143\",\"afterRelatedEventIdentifier\":null,\"startTimeMillis\":1623325830486,\"endTimeMillis\":1623325830487,\"id\":562,\"metrics\":[],\"wiretapFlowEvent\":{\"identifier\":561,\"event\":\"ActiveMQTextMessage {commandId = 5, responseRequired = false, messageId = ID:Michaels-MacBook-Pro.local-55556-1623243068313-7:24490:1:1:1, originalDestination = null, originalTransactionId = null, producerId = ID:Michaels-MacBook-Pro.local-55556-1623243068313-7:24490:1:1, destination = queue://com.caixa.bank.murex.out, transactionId = null, expiration = 0, timestamp = 1623325830485, arrival = 0, brokerInTime = 1623325830486, brokerOutTime = 1623325830486, correlationId = null, replyTo = null, persistent = true, type = null, priority = 4, groupID = null, groupSequence = 0, targetConsumerId = null, compressed = false, userID = null, content = org.apache.activemq.util.ByteSequence@5e5e1ee4, marshalledProperties = org.apache.activemq.util.ByteSequence@60f3cba8, dataStructure = null, redeliveryCounter = 0, size = 0, properties = {IkasanEventLifeIdentifier=-1996324143}, readOnlyProperties = true, readOnlyBody = true, droppable = false, jmsXGroupFirstForConsumer = false, text = <?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" standalo...rade></mxml>}\",\"moduleName\":\"My Module\",\"flowName\":\"Trade Consumer Flow\",\"componentName\":\"JMS Consumer\",\"timestamp\":1623325830486,\"expiry\":1623325830486,\"eventId\":\"-1996324143\",\"relatedEventId\":null}}]}", (String)solrInputDocument.getFieldValue(SolrDaoBase.PAYLOAD_CONTENT), false);
        Assert.assertEquals(1L, solrInputDocument.getFieldValue(SolrDaoBase.EXPIRY));
    }

    @Test
    @DirtiesContext
    public void test_get_results_within_timeframe() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            dao = new SolrMetricsDao();
            dao.setSolrClient(server);

            dao.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = dao.getMetrics(10000000L
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

            dao.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = dao.getMetrics("My Module", 10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(5, results.size());

            results = dao.getMetrics("Bad module name", 10000000L
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

            dao.save((List<FlowInvocationMetric>)mapper.readValue(this.loadDataFile("/data/flowInvocationMetrics.json")
                , mapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class)));

            List<FlowInvocationMetric> results = dao.getMetrics("My Module", "Trade Consumer Flow",10000000L
                , System.currentTimeMillis() + 10000000L);

            assertEquals(5, results.size());

            results = dao.getMetrics("Bad module name", "Trade Consumer Flow",10000000L
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
