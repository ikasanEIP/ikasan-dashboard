package org.ikasan.job.orchestration.core.service;

import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ContextServiceTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_load_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));

        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();

        System.out.println(contextService.getContextTemplateString(contextTemplate));
    }

    @Test
    public void test_load_context_instance() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        List<SchedulerJobInstance> schedulerJobInstances = context.getAllSchedulerJobInstances();

        String contextString = this.contextService.getContextInstanceString(context);

    }
}
