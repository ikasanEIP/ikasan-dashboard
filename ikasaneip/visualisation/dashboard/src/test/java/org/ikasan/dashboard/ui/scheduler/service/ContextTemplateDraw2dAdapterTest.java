package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Test;

import java.io.IOException;

public class ContextTemplateDraw2dAdapterTest extends AbstractTest {
    private ContextService contextService = new ContextService();
    private ContextTemplateDraw2dAdapter adapter = new ContextTemplateDraw2dAdapter();

    // todo build some tests
    @Test
    public void test() throws IOException {
//        ContextTemplate contextTemplate = this.contextService.getContextTemplate(loadDataFile("/data/contexts/-1793100514.json"));
//        contextTemplate = ContextHelper.getChildContextTemplate("CONTEXT--1209755884", contextTemplate);
//
//        String result = adapter.adaptJobs(contextTemplate);
//
//        System.out.println(result);
//
//        contextTemplate = this.contextService.getContextTemplate(loadDataFile("/data/contexts/-1793100514.json"));
//        contextTemplate = ContextHelper.getChildContextTemplate("CONTEXT-774294372", contextTemplate);
//
//        result = adapter.adaptJobs(contextTemplate);
//
//        System.out.println(result);
    }

}
