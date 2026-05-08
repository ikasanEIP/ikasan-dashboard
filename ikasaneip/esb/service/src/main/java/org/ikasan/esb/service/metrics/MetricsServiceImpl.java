package org.ikasan.esb.service.metrics;

import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.persistence.BatchInsert;

import java.util.List;

public class MetricsServiceImpl implements BatchInsert<FlowInvocationMetric>, MetricsService<FlowInvocationMetric> {

    private MetricsDao solrMetricsDao;

    public MetricsServiceImpl(MetricsDao solrMetricsDao) {
        this.solrMetricsDao = solrMetricsDao;
        if(this.solrMetricsDao == null) {
            throw new IllegalArgumentException("solrMetricsDao cannot be null!");
        }
    }

    @Override
    public void insert(List<FlowInvocationMetric> entities) {
        this.solrMetricsDao.save(entities);
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

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime, int offset, int limit) {
        return this.solrMetricsDao.getMetrics(startTime, endTime, offset, limit);
    }

    @Override
    public long count(long startTime, long endTime) {
        return this.solrMetricsDao.count(startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime, int offset, int limit) {
        return this.solrMetricsDao.getMetrics(moduleName, startTime, endTime, offset, limit);
    }

    @Override
    public long count(String moduleName, long startTime, long endTime) {
        return this.solrMetricsDao.count(moduleName, startTime, endTime);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime, int offset, int limit) {
        return this.solrMetricsDao.getMetrics(moduleName, flowName, startTime, endTime, offset, limit);
    }

    @Override
    public long count(String moduleName, String flowName, long startTime, long endTime) {
        return this.solrMetricsDao.count(moduleName, flowName, startTime, endTime);
    }
}
