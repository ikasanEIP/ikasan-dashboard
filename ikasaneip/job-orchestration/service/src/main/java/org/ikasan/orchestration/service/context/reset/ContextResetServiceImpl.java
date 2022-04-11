package org.ikasan.orchestration.service.context.reset;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.reset.ContextResetService;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ContextResetServiceImpl implements ContextResetService {

    @Override
    public void resetContext(String contextName) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
        if (contextMachine == null) {
            throw new ContextResetException(String.format("Could not find context for %s to reset", contextName));
        }
        try {
            contextMachine.resetContextInstance();
        } catch (JsonProcessingException e) {
            throw new ContextResetException("Failed to reset context " + e.getMessage());
        }
    }
}
