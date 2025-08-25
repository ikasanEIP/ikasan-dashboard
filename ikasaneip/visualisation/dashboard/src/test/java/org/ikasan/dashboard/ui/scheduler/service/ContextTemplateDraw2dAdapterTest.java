package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.Draw2dCanvasJsonHelper;
import org.ikasan.designer.model.Image;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextTemplateDraw2dAdapterTest extends AbstractTest {
    private ContextService contextService = new ContextService();
    private ContextTemplateDraw2dAdapter adapter = new ContextTemplateDraw2dAdapter();

    @Test
    public void test_context_with_2_jobs_in_leading_or() throws IOException {
        ContextTemplate contextTemplate = this.contextService.getContextTemplate(loadDataFile("/data/contexts/context-with-2-jobs-in-or-at-start.json"));
        ContextTemplate child = ContextHelper.getChildContextTemplate("DEMO-WITH_UPPER_CASE", contextTemplate);

        List<SchedulerJob> schedulerJobs = ContextHelper.getAllJobs(contextTemplate);

        schedulerJobs = schedulerJobs.stream()
            .map(schedulerJob -> {
                if(schedulerJob.getJobName().startsWith("sc")) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                    quartzScheduleDrivenJob.setJobName(schedulerJob.getJobName());
                    quartzScheduleDrivenJob.setAgentName(schedulerJob.getAgentName());
                    quartzScheduleDrivenJob.setIdentifier(schedulerJob.getIdentifier());

                    return quartzScheduleDrivenJob;
                }
                else if(schedulerJob.getJobName().startsWith("fw")) {
                    FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
                    fileEventDrivenJob.setJobName(schedulerJob.getJobName());
                    fileEventDrivenJob.setAgentName(schedulerJob.getAgentName());
                    fileEventDrivenJob.setIdentifier(schedulerJob.getIdentifier());

                    return fileEventDrivenJob;
                }
                else if(schedulerJob.getJobName().startsWith("ch")) {
                    InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
                    internalEventDrivenJob.setJobName(schedulerJob.getJobName());
                    internalEventDrivenJob.setAgentName(schedulerJob.getAgentName());
                    internalEventDrivenJob.setIdentifier(schedulerJob.getIdentifier());

                    return internalEventDrivenJob;
                }

                return schedulerJob;
            }).collect(Collectors.toList());

        String result = adapter.adaptJobs(contextTemplate, child,
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)),
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (key1, key2)-> key2))
            , new HashMap<>(), null);

        Map<String, Image> imageMap  = Draw2dCanvasJsonHelper.getSchedulerJobImagesFromCanvasJson(result);
        Assert.assertFalse(imageMap.isEmpty());
        Assert.assertEquals(2102.5, imageMap.get("scheduler-agent-ch7").getX(), 5);
        Assert.assertEquals(735, imageMap.get("scheduler-agent-ch7").getY(), 5);
        Assert.assertEquals("scheduler-agent-ch7", imageMap.get("scheduler-agent-ch7").getId());

        Assert.assertEquals(1601.5, imageMap.get("scheduler-agent-ch3").getX(), 5);
        Assert.assertEquals(600, imageMap.get("scheduler-agent-ch3").getY(), 5);
        Assert.assertEquals("scheduler-agent-ch3", imageMap.get("scheduler-agent-ch3").getId());

        Assert.assertEquals(600, imageMap.get("scheduler-agent-sc1").getX(), 5);
        Assert.assertEquals(600, imageMap.get("scheduler-agent-sc1").getY(), 5);
        Assert.assertEquals("scheduler-agent-sc1", imageMap.get("scheduler-agent-sc1").getId());

        Assert.assertEquals(1601.5, imageMap.get("scheduler-agent-ch5").getX(), 5);
        Assert.assertEquals(870, imageMap.get("scheduler-agent-ch5").getY(), 5);
        Assert.assertEquals("scheduler-agent-ch5", imageMap.get("scheduler-agent-ch5").getId());

        Assert.assertEquals(1100.5, imageMap.get("scheduler-agent-ch2").getX(), 5);
        Assert.assertEquals(600, imageMap.get("scheduler-agent-ch2").getY(), 5);
        Assert.assertEquals("scheduler-agent-ch2", imageMap.get("scheduler-agent-ch2").getId());

        Assert.assertEquals(1100.5, imageMap.get("scheduler-agent-ch1").getX(), 5);
        Assert.assertEquals(870, imageMap.get("scheduler-agent-ch1").getY(), 5);
        Assert.assertEquals("scheduler-agent-ch1", imageMap.get("scheduler-agent-ch1").getId());
    }

    @Test
    public void test_render_context_with_layout() throws IOException {
        ContextTemplate contextTemplate = this.contextService.getContextTemplate(loadDataFile("/data/contexts/test-plan-with-layout.json"));
        ContextTemplate child = ContextHelper.getChildContextTemplate("test1", contextTemplate);

        List<SchedulerJob> schedulerJobs = ContextHelper.getAllJobs(contextTemplate);

        schedulerJobs = schedulerJobs.stream()
            .map(schedulerJob -> {
                if(!schedulerJob.getIdentifier().contains("BRIDGING_JOB") &&
                    !schedulerJob.getIdentifier().contains("LOCAL_EVENT") &&
                    !schedulerJob.getIdentifier().contains("TERMINAL") &&
                    !schedulerJob.getIdentifier().contains("START")) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                    quartzScheduleDrivenJob.setJobName(schedulerJob.getJobName());
                    quartzScheduleDrivenJob.setAgentName(schedulerJob.getAgentName());
                    quartzScheduleDrivenJob.setIdentifier(schedulerJob.getIdentifier());

                    return quartzScheduleDrivenJob;
                }

                return schedulerJob;
            }).collect(Collectors.toList());

        Map<String, Image> imageMap = Draw2dCanvasJsonHelper.getSchedulerJobImagesFromCanvasJson(child.getUserGeneratedLayout());
        Assert.assertFalse(imageMap.isEmpty());

        String result = adapter.adaptJobs(contextTemplate, child,
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)),
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (key1, key2)-> key2))
            , imageMap, null);

        imageMap = Draw2dCanvasJsonHelper.getSchedulerJobImagesFromCanvasJson(result);

        Assert.assertFalse(imageMap.isEmpty());
        Assert.assertEquals(2888.206, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getX(), 0);
        Assert.assertEquals(1338.9479999999999, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getY(), 0);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730711810204", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getId());
        Assert.assertEquals(1504.0, imageMap.get("LOCAL_EVENT_JOB-test-local").getX(), 0);
        Assert.assertEquals(600.0, imageMap.get("LOCAL_EVENT_JOB-test-local").getY(), 0);
        Assert.assertEquals("LOCAL_EVENT_JOB-test-local", imageMap.get("LOCAL_EVENT_JOB-test-local").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah2").getX(), 0);
        Assert.assertEquals(780.0, imageMap.get("scheduler-agent-blah2").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah2", imageMap.get("scheduler-agent-blah2").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah1").getX(), 0);
        Assert.assertEquals(960.0, imageMap.get("scheduler-agent-blah1").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah1", imageMap.get("scheduler-agent-blah1").getId());
        Assert.assertEquals(2858.0, imageMap.get("LOCAL_EVENT_JOB-jjj").getX(), 0);
        Assert.assertEquals(780.0, imageMap.get("LOCAL_EVENT_JOB-jjj").getY(), 0);
        Assert.assertEquals("LOCAL_EVENT_JOB-jjj", imageMap.get("LOCAL_EVENT_JOB-jjj").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah4").getX(), 0);
        Assert.assertEquals(1050.0, imageMap.get("scheduler-agent-blah4").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah4", imageMap.get("scheduler-agent-blah4").getId());
        Assert.assertEquals(4264.928, imageMap.get("LOCAL_EVENT_JOB-test").getX(), 0);
        Assert.assertEquals(1978.766, imageMap.get("LOCAL_EVENT_JOB-test").getY(), 0);
        Assert.assertEquals("LOCAL_EVENT_JOB-test", imageMap.get("LOCAL_EVENT_JOB-test").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah3").getX(), 0);
        Assert.assertEquals(690.0, imageMap.get("scheduler-agent-blah3").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah3", imageMap.get("scheduler-agent-blah3").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah5").getX(), 0);
        Assert.assertEquals(870.0, imageMap.get("scheduler-agent-blah5").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah5", imageMap.get("scheduler-agent-blah5").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah-blah").getX(), 0);
        Assert.assertEquals(1140.0, imageMap.get("scheduler-agent-blah-blah").getY(), 0);
        Assert.assertEquals("scheduler-agent-blah-blah", imageMap.get("scheduler-agent-blah-blah").getId());
        Assert.assertEquals(3358.428, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getX(), 0);
        Assert.assertEquals(422.40200000000004, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getY(), 0);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730720779184", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getId());
        Assert.assertEquals(4669.5, imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getX(), 0);
        Assert.assertEquals(805.0, imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getY(), 0);
        Assert.assertEquals("CONTEXT_TERMINAL_JOB-test1_TERMINAL", imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getId());
        Assert.assertEquals(4215.5, imageMap.get("scheduler-agent-visaul").getX(), 0);
        Assert.assertEquals(780.0, imageMap.get("scheduler-agent-visaul").getY(), 0);
        Assert.assertEquals("scheduler-agent-visaul", imageMap.get("scheduler-agent-visaul").getId());
        Assert.assertEquals(600.5, imageMap.get("scheduler-agent-test").getX(), 0);
        Assert.assertEquals(958.0, imageMap.get("scheduler-agent-test").getY(), 0);
        Assert.assertEquals("scheduler-agent-test", imageMap.get("scheduler-agent-test").getId());
        Assert.assertEquals(1956.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getX(), 0);
        Assert.assertEquals(870.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getY(), 0);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730699885554", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getId());
        Assert.assertEquals(1051.5, imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getX(), 0);
        Assert.assertEquals(958.0, imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getY(), 0);
        Assert.assertEquals("BRIDGING_JOB-test1_TERMINAL_1730694933465", imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getId());
        Assert.assertEquals(3761.5, imageMap.get("scheduler-agent-new-job").getX(), 0);
        Assert.assertEquals(870.0, imageMap.get("scheduler-agent-new-job").getY(), 0);
        Assert.assertEquals("scheduler-agent-new-job", imageMap.get("scheduler-agent-new-job").getId());
        Assert.assertEquals(1051.5, imageMap.get("test-bug_in").getX(), 5);
        Assert.assertEquals(1140.0, imageMap.get("test-bug_in").getY(), 5);
        Assert.assertEquals("test-bug_in", imageMap.get("test-bug_in").getId());
    }

    @Test
    public void test_render_context_without_layout() throws IOException {
        ContextTemplate contextTemplate = this.contextService.getContextTemplate(loadDataFile("/data/contexts/test-plan-with-layout.json"));
        ContextTemplate child = ContextHelper.getChildContextTemplate("test1", contextTemplate);

        List<SchedulerJob> schedulerJobs = ContextHelper.getAllJobs(contextTemplate);

        schedulerJobs = schedulerJobs.stream()
            .map(schedulerJob -> {
                if(!schedulerJob.getIdentifier().contains("BRIDGING_JOB") &&
                    !schedulerJob.getIdentifier().contains("LOCAL_EVENT") &&
                    !schedulerJob.getIdentifier().contains("TERMINAL") &&
                    !schedulerJob.getIdentifier().contains("START")) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                    quartzScheduleDrivenJob.setJobName(schedulerJob.getJobName());
                    quartzScheduleDrivenJob.setAgentName(schedulerJob.getAgentName());
                    quartzScheduleDrivenJob.setIdentifier(schedulerJob.getIdentifier());

                    return quartzScheduleDrivenJob;
                }

                return schedulerJob;
            }).collect(Collectors.toList());


        String result = adapter.adaptJobs(contextTemplate, child,
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)),
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (key1, key2)-> key2))
            , new HashMap<>(), null);

        Map<String, Image> imageMap = Draw2dCanvasJsonHelper.getSchedulerJobImagesFromCanvasJson(result);

        Assert.assertEquals(2858.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getX(), 5);
        Assert.assertEquals(780.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getY(), 5);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730711810204", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730711810204").getId());
        Assert.assertEquals(1504.0, imageMap.get("LOCAL_EVENT_JOB-test-local").getX(), 5);
        Assert.assertEquals(600.0, imageMap.get("LOCAL_EVENT_JOB-test-local").getY(), 5);
        Assert.assertEquals("LOCAL_EVENT_JOB-test-local", imageMap.get("LOCAL_EVENT_JOB-test-local").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah2").getX(), 5);
        Assert.assertEquals(960.0, imageMap.get("scheduler-agent-blah2").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah2", imageMap.get("scheduler-agent-blah2").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah1").getX(), 5);
        Assert.assertEquals(780.0, imageMap.get("scheduler-agent-blah1").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah1", imageMap.get("scheduler-agent-blah1").getId());
        Assert.assertEquals(2858.0, imageMap.get("LOCAL_EVENT_JOB-jjj").getX(), 5);
        Assert.assertEquals(960.0, imageMap.get("LOCAL_EVENT_JOB-jjj").getY(), 5);
        Assert.assertEquals("LOCAL_EVENT_JOB-jjj", imageMap.get("LOCAL_EVENT_JOB-jjj").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah4").getX(), 5);
        Assert.assertEquals(690.0, imageMap.get("scheduler-agent-blah4").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah4", imageMap.get("scheduler-agent-blah4").getId());
        Assert.assertEquals(4215.5, imageMap.get("LOCAL_EVENT_JOB-test").getX(), 5);
        Assert.assertEquals(960.0, imageMap.get("LOCAL_EVENT_JOB-test").getY(), 5);
        Assert.assertEquals("LOCAL_EVENT_JOB-test", imageMap.get("LOCAL_EVENT_JOB-test").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah3").getX(), 5);
        Assert.assertEquals(1050.0, imageMap.get("scheduler-agent-blah3").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah3", imageMap.get("scheduler-agent-blah3").getId());
        Assert.assertEquals(2406.5, imageMap.get("scheduler-agent-blah5").getX(), 5);
        Assert.assertEquals(870.0, imageMap.get("scheduler-agent-blah5").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah5", imageMap.get("scheduler-agent-blah5").getId());
        Assert.assertEquals(1504.0, imageMap.get("scheduler-agent-blah-blah").getX(), 5);
        Assert.assertEquals(1140.0, imageMap.get("scheduler-agent-blah-blah").getY(), 5);
        Assert.assertEquals("scheduler-agent-blah-blah", imageMap.get("scheduler-agent-blah-blah").getId());
        Assert.assertEquals(3309.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getX(), 5);
        Assert.assertEquals(870.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getY(), 5);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730720779184", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730720779184").getId());
        Assert.assertEquals(4669.5, imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getX(), 5);
        Assert.assertEquals(805.0, imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getY(), 5);
        Assert.assertEquals("CONTEXT_TERMINAL_JOB-test1_TERMINAL", imageMap.get("CONTEXT_TERMINAL_JOB-test1_TERMINAL").getId());
        Assert.assertEquals(4215.5, imageMap.get("scheduler-agent-visaul").getX(), 5);
        Assert.assertEquals(780.0, imageMap.get("scheduler-agent-visaul").getY(), 5);
        Assert.assertEquals("scheduler-agent-visaul", imageMap.get("scheduler-agent-visaul").getId());
        Assert.assertEquals(600.5, imageMap.get("scheduler-agent-test").getX(), 5);
        Assert.assertEquals(959.0, imageMap.get("scheduler-agent-test").getY(), 5);
        Assert.assertEquals("scheduler-agent-test", imageMap.get("scheduler-agent-test").getId());
        Assert.assertEquals(1956.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getX(), 5);
        Assert.assertEquals(870.0, imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getY(), 5);
        Assert.assertEquals("BRIDGING_JOB-test1_BRIDGING_1730699885554", imageMap.get("BRIDGING_JOB-test1_BRIDGING_1730699885554").getId());
        Assert.assertEquals(1051.5, imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getX(), 5);
        Assert.assertEquals(959.0, imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getY(), 5);
        Assert.assertEquals("BRIDGING_JOB-test1_TERMINAL_1730694933465", imageMap.get("BRIDGING_JOB-test1_TERMINAL_1730694933465").getId());
        Assert.assertEquals(3761.5, imageMap.get("scheduler-agent-new-job").getX(), 5);
        Assert.assertEquals(870.0, imageMap.get("scheduler-agent-new-job").getY(), 5);
        Assert.assertEquals("scheduler-agent-new-job", imageMap.get("scheduler-agent-new-job").getId());
        Assert.assertEquals(1051.5, imageMap.get("test-bug_in").getX(), 5);
        Assert.assertEquals(1140.0, imageMap.get("test-bug_in").getY(), 5);
        Assert.assertEquals("test-bug_in", imageMap.get("test-bug_in").getId());
    }

}
