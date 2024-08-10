package org.ikasan.dashboard.ui.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapter;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

public class CanvasJsonToContextTemplateAdapterTest extends AbstractTest {

    ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Test
    public void test_sample_context_with_multiple_and_roots() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/context-multiple-and-roots.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-multiple-and-roots.json");

        JSONAssert.assertEquals(result, objectMapper.writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_context_and_single_job_with_nested_or() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/context-and-single-job-with-nested-or.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT--1209755884", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-context-and-single-job-with-nested-or.json");

        JSONAssert.assertEquals(result, objectMapper.writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_sample_context_with_parallel_jobs() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/sample-context-parallel-jobs.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-sample-context-parallel-jobs.json");

        JSONAssert.assertEquals(result, objectMapper.writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_sample_context_with_repeating_jobs() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/context-with-repeating-jobs.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-context-with-repeating-jobs.json");

        JSONAssert.assertEquals(result, objectMapper.writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_sample_context_with_start_and_end_jobs() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/draw2d-json-with-start-and-end-jobs.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-context-with-repeating-jobs.json");

        JSONAssert.assertEquals(result, objectMapper.writeValueAsString(contextTemplate), false);
    }
}
