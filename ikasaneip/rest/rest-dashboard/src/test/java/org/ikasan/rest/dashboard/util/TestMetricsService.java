package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsService;

import java.util.List;

public class TestMetricsService implements MetricsService<FlowInvocationMetric> {
    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime) {
        return null;
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime) {
        return null;
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime) {
        return null;
    }
}
