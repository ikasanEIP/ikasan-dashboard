package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
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
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.util.List;
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
            schedulerJobs.stream().collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (key1, key2)-> key2)));

        JSONAssert.assertEquals(loadDataFile("/data/contexts/results/context-with-2-jobs-in-or-at-start-result.json"), result, new CustomComparator(JSONCompareMode.STRICT,
            new Customization("[*].id", (o1, o2) -> true), new Customization("[*].ports[*].id", (o1, o2) -> true),new Customization("[*].composite", (o1, o2) -> true)));
    }

}
