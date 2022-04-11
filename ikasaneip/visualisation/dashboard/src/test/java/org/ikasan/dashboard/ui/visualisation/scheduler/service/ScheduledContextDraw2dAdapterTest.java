package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.dashboard.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;

public class ScheduledContextDraw2dAdapterTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_context_machine() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/contexts/CONTEXT-1436221681.json"));

        ScheduledContextDraw2dAdapter adapter = new ScheduledContextDraw2dAdapter();
        String json = adapter.adaptContext(context);

        // todo sort out assertion
//        JSONAssert.assertEquals(loadDataFile("/data/contexts/results/CONTEXT-1436221681-results.json"), json
//            , new CustomComparator(JSONCompareMode.LENIENT
//                , new Customization("./id", (o1, o2) -> true)
//                , new Customization("./ports.id", (o1, o2) -> true)));
    }
}
