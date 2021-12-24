package org.ikasan.scheduler.core.service;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.context.ContextTemplateImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.junit.Test;

import java.io.IOException;

public class ContextServiceTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_load_context() throws IOException {
        ContextTemplateImpl context = this.contextService.getContext(loadDataFile("/data/context.json"));
    }

    @Test
    public void test_load_context_instance() throws IOException {
        ContextInstanceImpl context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        String contextString = this.contextService.getContextInstanceString(context);

    }
}
