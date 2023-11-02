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

public class ContextDraw2DAdapterTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_adapt_success() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/contexts/-1793100514.json"));

        ContextInstanceDraw2dAdapter adapter = new ContextInstanceDraw2dAdapter();
        String json = adapter.adaptContext(context);

        JSONAssert.assertEquals(loadDataFile("/data/contexts/results/-1793100514-results.json"), json
            , new CustomComparator(JSONCompareMode.LENIENT
                , new Customization("***", (o1, o2) -> true)));
    }
}
