package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;

import java.io.IOException;

public class ScheduledContextDraw2dAdapterTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_context_machine() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-1394887810.json"));

        ScheduledContextDraw2dAdapter adapter = new ScheduledContextDraw2dAdapter();
        adapter.adapt(context);
    }
}
