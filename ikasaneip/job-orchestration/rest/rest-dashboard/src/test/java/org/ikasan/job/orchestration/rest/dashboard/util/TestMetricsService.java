package org.ikasan.job.orchestration.rest.dashboard.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.rest.dashboard.model.metrics.FlowInvocationMetricImpl;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestMetricsService implements MetricsService<FlowInvocationMetric> {
    public static final String METRICS_JSON = "/data/metrics.json";

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
