package org.ikasan.orchestration.service.context.reset;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.reset.ContextResetService;

public class ContextResetServiceImpl implements ContextResetService {

    @Override
    public void resetContext(String contextName, boolean holdCommandExecutionJob) {

        for(ContextMachine contextMachine : ContextMachineCache.instance().getAllByContextName(contextName)) {
            if (contextMachine == null) {
                throw new ContextResetException(String.format("Could not find context for context name %s to reset", contextName));
            }
            try {
                contextMachine.resetContextInstance(holdCommandExecutionJob);
            } catch (Exception e) {
                throw new ContextResetException("Failed to reset context " + e.getMessage());
            }
        }
    }

    @Override
    public void resetContextInstance(String contextInstanceId, boolean holdCommandExecutionJob) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            throw new ContextResetException(String.format("Could not find context for context instance ID %s to reset", contextInstanceId));
        }
        try {
            contextMachine.resetContextInstance(holdCommandExecutionJob);
        } catch (Exception e) {
            throw new ContextResetException("Failed to reset context " + e.getMessage());
        }
    }
}
