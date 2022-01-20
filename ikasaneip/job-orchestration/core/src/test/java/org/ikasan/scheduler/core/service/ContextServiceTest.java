package org.ikasan.scheduler.core.service;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.context.ContextTemplateImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;

import java.io.IOException;

public class ContextServiceTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_load_context() throws IOException {
        ContextTemplate context = this.contextService.getContext(loadDataFile("/data/context.json"));

        ContextTemplateImpl contextTemplate = new ContextTemplateImpl();

        System.out.println(contextService.getContextString(contextTemplate));
    }

    @Test
    public void test_load_context_instance() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        String contextString = this.contextService.getContextInstanceString(context);

    }
}
