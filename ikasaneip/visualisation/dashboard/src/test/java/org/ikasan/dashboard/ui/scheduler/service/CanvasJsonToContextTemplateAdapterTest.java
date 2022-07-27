package org.ikasan.dashboard.ui.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;

public class CanvasJsonToContextTemplateAdapterTest extends AbstractTest {

    @Test
    public void test() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/sample-context.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        adapter.adapt("CONTEXT-1892741766", canvasJson);
    }

    @Test
    public void test_2() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/nested-sample-context.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        adapter.adapt("CONTEXT-1892741766", canvasJson);
    }

    @Test
    public void test_sample_context_with_multiple_and_roots() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/context-multiple-and-roots.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-multiple-and-roots.json");

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate));
        JSONAssert.assertEquals(result, new ObjectMapper().writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_4() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/context-and-single-job-with-nested-or.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT--1209755884", canvasJson);

        String result = loadDataFile("/data/contexts/results/results-nested-sample-context.json");

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate));
        JSONAssert.assertEquals(result, new ObjectMapper().writeValueAsString(contextTemplate), false);
    }

    @Test
    public void test_sample_context_with_parallel_jobs() throws IOException {
        String canvasJson = loadDataFile("/data/contexts/sample-context-parallel-jobs.json");
        CanvasJsonToContextTemplateAdapter adapter = new CanvasJsonToContextTemplateAdapter();
        ContextTemplate contextTemplate = adapter.adapt("CONTEXT-1892741766", canvasJson);

//        String result = loadDataFile("/data/contexts/results/results-multiple-and-roots.json");
//
//        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate));
//        JSONAssert.assertEquals(result, new ObjectMapperapper().writeValueAsString(contextTemplate), false);
    }
}
