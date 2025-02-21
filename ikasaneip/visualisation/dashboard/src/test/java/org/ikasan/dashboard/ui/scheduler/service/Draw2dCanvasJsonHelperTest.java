package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.Draw2dCanvasJsonHelper;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.Rectangle;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class Draw2dCanvasJsonHelperTest extends AbstractTest {
    private ContextService contextService = new ContextService();

    @Test
    public void test_get_image_map_success() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/contexts/test-plan-with-layout.json"));

        Map<String, Image> imageMap = Draw2dCanvasJsonHelper
            .getSchedulerJobImagesFromCanvasJson(contextTemplate.getUserGeneratedLayout());

        Assert.assertFalse(imageMap.isEmpty());
        Assert.assertEquals(7, imageMap.size());
        Assert.assertEquals(2410.5, imageMap.get("CONTEXT_TERMINAL_JOB-test-bug_TERMINAL").getX(), 0);
        Assert.assertEquals(805.0, imageMap.get("CONTEXT_TERMINAL_JOB-test-bug_TERMINAL").getY(), 0);
        Assert.assertEquals("CONTEXT_TERMINAL_JOB-test-bug_TERMINAL"
            , imageMap.get("CONTEXT_TERMINAL_JOB-test-bug_TERMINAL").getId());
        Assert.assertEquals(1956.5, imageMap.get("scheduler-agent-blah-blah").getX(), 0);
        Assert.assertEquals(780.0, imageMap.get("scheduler-agent-blah-blah").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah-blah"
            , imageMap.get("scheduler-agent-blah-blah").getId());
        Assert.assertEquals(1514.2725, imageMap.get("BRIDGING_JOB-test-bug_TERMINAL_1730694063496").getX(), 0);
        Assert.assertEquals(941.5725, imageMap.get("BRIDGING_JOB-test-bug_TERMINAL_1730694063496").getY(), 0);
        Assert.assertEquals("BRIDGING_JOB-test-bug_TERMINAL_1730694063496"
            , imageMap.get("BRIDGING_JOB-test-bug_TERMINAL_1730694063496").getId());
        Assert.assertEquals(600.0, imageMap.get("scheduler-agent-my-file-watcher").getX(), 0);
        Assert.assertEquals(690.0, imageMap.get("scheduler-agent-my-file-watcher").getY(), 0);
        Assert.assertEquals("LOCAL_EVENT_JOB-test"
            , imageMap.get("LOCAL_EVENT_JOB-test").getId());
        Assert.assertEquals(1956.5, imageMap.get("LOCAL_EVENT_JOB-test").getX(), 0);
        Assert.assertEquals(600.0, imageMap.get("LOCAL_EVENT_JOB-test").getY(), 0);
        Assert.assertEquals("LOCAL_EVENT_JOB-test"
            , imageMap.get("LOCAL_EVENT_JOB-test").getId());
        Assert.assertEquals(980.3600000000001, imageMap.get("scheduler-agent-cmd1").getX(), 0);
        Assert.assertEquals(734.76, imageMap.get("scheduler-agent-cmd1").getY(), 0);
        Assert.assertEquals("scheduler-agent-cmd1"
            , imageMap.get("scheduler-agent-cmd1").getId());
    }

    @Test
    public void test_get_logical_groupings_success() throws IOException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/contexts/test-plan-with-layout-and-logical-grouping.json"));

        Map<String, Rectangle> rectangleMap = Draw2dCanvasJsonHelper
            .getLogicalBoundariesFromCanvasJson(contextTemplate.getUserGeneratedLayout());

        Assert.assertEquals(1, rectangleMap.size());
    }

    @Test
    public void test_bad_layout_returns_empty_map_success() {
        Map<String, Image> imageMap = Draw2dCanvasJsonHelper
            .getSchedulerJobImagesFromCanvasJson("bad generated layout");

        Assert.assertTrue(imageMap.isEmpty());
    }

    @Test
    public void test_null_layout_returns_empty_map_success() {
        Map<String, Image> imageMap = Draw2dCanvasJsonHelper
            .getSchedulerJobImagesFromCanvasJson(null);

        Assert.assertTrue(imageMap.isEmpty());
    }

    @Test
    public void test_empty_layout_returns_empty_map_success() {
        Map<String, Image> imageMap = Draw2dCanvasJsonHelper
            .getSchedulerJobImagesFromCanvasJson(null);

        Assert.assertTrue(imageMap.isEmpty());
    }
}
