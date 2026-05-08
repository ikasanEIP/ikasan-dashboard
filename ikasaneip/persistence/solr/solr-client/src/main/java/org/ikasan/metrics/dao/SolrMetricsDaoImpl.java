package org.ikasan.metrics.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.metrics.model.SolrFlowInvocationMetric;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Ikasan Development Team.
 */
public class SolrMetricsDaoImpl extends SolrDaoBase<FlowInvocationMetric> implements MetricsDao {
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrMetricsDaoImpl.class);

    /**
     * We need to give this document it's context.
     */
    public static final String METRIC_ENTITY_TYPE = "metric";

    private int solrMetricsQueryLimit;

    private ObjectMapper mapper;

    public SolrMetricsDaoImpl(int solrMetricsQueryLimit) {
        this.solrMetricsQueryLimit = solrMetricsQueryLimit;
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, FlowInvocationMetric flowInvocationMetric) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, flowInvocationMetric.getModuleName() + "-" + METRIC_ENTITY_TYPE
            + UUID.randomUUID());
        document.addField(TYPE, METRIC_ENTITY_TYPE);
        document.addField(MODULE_NAME, flowInvocationMetric.getModuleName());
        document.addField(FLOW_NAME, flowInvocationMetric.getFlowName());
        flowInvocationMetric.setHarvestedDateTime(System.currentTimeMillis());

        try {
            document.addField(PAYLOAD_CONTENT, this.mapper.writeValueAsString(flowInvocationMetric));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.warn(String.format("Could not set metric payload content[%s]", flowInvocationMetric), e);
        }

        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, expiry);

        return document;
    }

    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, 0, this.solrMetricsQueryLimit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }

    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime, int offset, int limit) {
        try {
            String query = super.buildQuery(Set.of(), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, offset, limit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }

    public long count(long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            return this.count(query);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics count query!", e);
        }
    }

    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, 0, this.solrMetricsQueryLimit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }

    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime, int offset, int limit) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, offset, limit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }

    public long count(String moduleName, long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            return this.count(query);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics count query!", e);
        }
    }

    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(flowName), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, 0, this.solrMetricsQueryLimit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }


    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime, int offset, int limit) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(flowName), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            List<FlowInvocationMetric> results = this.findByQuery(query, offset, limit)
                .stream()
                .map(bean -> convert(bean.getFlowInvocationMetric()))
                .collect(Collectors.toList());

            return results;
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics query!", e);
        }
    }

    public long count(String moduleName, String flowName, long startTime, long endTime) {
        try {
            String query = super.buildQuery(Set.of(moduleName), Set.of(flowName), new Date(startTime), new Date(endTime), null
                , null, METRIC_ENTITY_TYPE, false);

            return this.count(query);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not execute solr metrics count query!", e);
        }
    }

    /**
     * Helper method to find by query.
     *
     * @param query
     */
    private List<SolrFlowInvocationMetric> findByQuery(String query, int offset, int limit)
    {
        logger.debug("queryString: " + query);

        if(limit > this.solrMetricsQueryLimit) {
            throw new RuntimeException(String.format("Error resolving solr flow invocation metric by query [" + query + "] " +
                "from the ikasan solr index! The limit on the query[%s] exceeds the maximum limit configured [%s]. The limit " +
                "can be increased by setting configuration property 'solr.metrics.query.limit'.", limit, this.solrMetricsQueryLimit ));
        }

        try
        {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setStart(offset);
            solrQuery.setRows(limit);

            QueryRequest req = new QueryRequest(solrQuery);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrFlowInvocationMetric.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr flow invocation metric by query [" + query + "] " +
                "from the ikasan solr index!", e);
        }
    }

    private long count(String query)
    {
        logger.debug("queryString: " + query);

        try
        {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);

            QueryRequest req = new QueryRequest(solrQuery);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getResults().getNumFound();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr flow invocation metric count by query [" + query + "] " +
                "from the ikasan solr index!", e);
        }
    }

    /**
     * Helper method to convert raw module metadata.
     *
     rawFlowInvocationMetric     * @return
     */
    private FlowInvocationMetric convert(String rawFlowInvocationMetric)
    {
        try
        {
            FlowInvocationMetric solrModuleMetaData
                = mapper.readValue(rawFlowInvocationMetric, FlowInvocationMetricImpl.class);

            return solrModuleMetaData;
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Unable to deserialise FlowInvocationMetric [%s]"
                , rawFlowInvocationMetric), e);
        }
    }
}
