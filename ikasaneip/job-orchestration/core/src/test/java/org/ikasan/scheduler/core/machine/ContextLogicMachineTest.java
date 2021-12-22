package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ContextLogicMachineTest extends AbstractTest {

    private ContextService contextService = new ContextService();
    private ContextLogicMachine contextLogicMachine = new ContextLogicMachine();

    @Test
        public void test_context_machine() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        Assert.assertFalse(contextLogicMachine.contextLogicSatisfied(context.getContextsMap(), context.getContextDependencies()));
    }

    @Test
    public void test_context_machine_context_all_satisfied() throws IOException {
        ContextInstance context1 = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        // Get a handle to Context2 which has a dependency on Context4.
        ContextInstance context2 = context1.getContexts().get(0);

        Assert.assertFalse(contextLogicMachine.contextLogicSatisfied(context2.getContextsMap(), context2.getContextDependencies()));

        // Get a handle to Context3 which Context4 has a dependency on and set it to complete.
        ContextInstance context3 = context2.getContexts().get(0);
        context3.setStatus(InstanceStatus.COMPLETE);

        // Context2 is still not complete as it has a dependency on Context4 which is not yet complete.
        Assert.assertFalse(contextLogicMachine.contextLogicSatisfied(context2.getContextsMap(), context2.getContextDependencies()));

        // Get a handle to Context4 which Context2 has a dependency on and set it to complete.
        ContextInstance context4 = context2.getContexts().get(1);
        context4.setStatus(InstanceStatus.COMPLETE);

        // Context2 is now complete as its dependencies on Context3 and Context 4 have been satisfied.
        Assert.assertTrue(contextLogicMachine.contextLogicSatisfied(context2.getContextsMap(), context2.getContextDependencies()));
        context2.setStatus(InstanceStatus.COMPLETE);

        // Context1 is still not complete as it has a dependency on Context5 which is not yet complete.
        Assert.assertFalse(contextLogicMachine.contextLogicSatisfied(context1.getContextsMap(), context1.getContextDependencies()));

        // Get a handle to Context5 which has a dependency on Context2.
        ContextInstance context5 = context1.getContexts().get(1);
        // Context5 has all of its dependencies satisfied.
        Assert.assertTrue(contextLogicMachine.contextLogicSatisfied(context5.getContextsMap(), context5.getContextDependencies()));
        context5.setStatus(InstanceStatus.COMPLETE);

        // Context1 is now complete as Context5 is complete.
        Assert.assertTrue(contextLogicMachine.contextLogicSatisfied(context1.getContextsMap(), context1.getContextDependencies()));
    }
}
