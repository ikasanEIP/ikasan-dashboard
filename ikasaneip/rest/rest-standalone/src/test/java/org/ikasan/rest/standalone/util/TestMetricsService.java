package org.ikasan.rest.standalone.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.rest.standalone.model.metrics.FlowInvocationMetricImpl;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestMetricsService implements MetricsService<FlowInvocationMetric> {
    public static final String METRICS_JSON = "/data/metrics.json";
    public static final String METRICS_JSON_LARGE = "/data/flowInvocationMetricsLarge.json";

    private ObjectMapper objectMapper;

    public TestMetricsService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime) {
        try {
            return objectMapper.readValue(loadDataFile(METRICS_JSON)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime) {
        try {
            return objectMapper.readValue(loadDataFile(METRICS_JSON)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime) {
        try {
            return objectMapper.readValue(loadDataFile(METRICS_JSON)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(long startTime, long endTime, int offset, int limit) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            if(offset >= 0 && limit > 0 && offset + limit >= metrics.size()) {
                metrics = metrics.subList(offset, metrics.size());
            }
            else if(offset >= 0 && limit > 0 && offset + limit < metrics.size()) {
                metrics = metrics.subList(offset, offset + limit);
            }

            return metrics;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long count(long startTime, long endTime) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            return metrics.size();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, long startTime, long endTime, int offset, int limit) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            if(offset >= 0 && limit > 0 && offset + limit >= metrics.size()) {
                metrics = metrics.subList(offset, metrics.size());
            }
            else if(offset >= 0 && limit > 0 && offset + limit < metrics.size()) {
                metrics = metrics.subList(offset, offset + limit);
            }

            return metrics;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long count(String moduleName, long startTime, long endTime) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            return metrics.size();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<FlowInvocationMetric> getMetrics(String moduleName, String flowName, long startTime, long endTime, int offset, int limit) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            if(offset >= 0 && limit > 0 && offset + limit >= metrics.size()) {
                metrics = metrics.subList(offset, metrics.size());
            }
            else if(offset >= 0 && limit > 0 && offset + limit < metrics.size()) {
                metrics = metrics.subList(offset, offset + limit);
            }

            return metrics;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long count(String moduleName, String flowName, long startTime, long endTime) {
        try {
            List<FlowInvocationMetric> metrics = objectMapper.readValue(loadDataFile(METRICS_JSON_LARGE)
                , objectMapper.getTypeFactory().constructCollectionType(List.class, FlowInvocationMetricImpl.class));

            return metrics.size();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
