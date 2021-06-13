package org.ikasan.metrics.service;

import org.ikasan.metrics.dao.SolrMetricsDao;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.solr.SolrService;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.List;

public class SolrMetricsServiceImpl extends SolrServiceBase implements SolrService<FlowInvocationMetric>, BatchInsert<FlowInvocationMetric>, MetricsService<FlowInvocationMetric> {

    private SolrMetricsDao solrMetricsDao;

    public SolrMetricsServiceImpl(SolrMetricsDao solrMetricsDao) {
        this.solrMetricsDao = solrMetricsDao;
        if(this.solrMetricsDao == null) {
            throw new IllegalArgumentException("solrMetricsDao cannot be null!");
        }
    }

    @Override
    public void insert(List<FlowInvocationMetric> entities) {
        this.solrMetricsDao.setSolrUsername(this.solrUsername);
        this.solrMetricsDao.setSolrPassword(this.solrPassword);
        this.solrMetricsDao.save(entities);
    }

    @Override
    public void save(FlowInvocationMetric save) {
        this.solrMetricsDao.setSolrUsername(this.solrUsername);
        this.solrMetricsDao.setSolrPassword(this.solrPassword);
        this.solrMetricsDao.save(save);
    }

    @Override
    public void save(List<FlowInvocationMetric> save) {
        this.solrMetricsDao.setSolrUsername(this.solrUsername);
        this.solrMetricsDao.setSolrPassword(this.solrPassword);
        this.insert(save);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime) {
        return this.solrMetricsDao.getMetrics(startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime) {
        return this.solrMetricsDao.getMetrics(moduleName, startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime) {
        return this.solrMetricsDao.getMetrics(moduleName, flowName, startTime, endTime);
    }
}
