package org.ikasan.job.orchestration.context.recovery;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.mockito.ArgumentMatcher;

public class CustomerBackFillerMatcher implements ArgumentMatcher<ContextInstance> {

    private ContextInstance entry;
    private String contextName;

    public CustomerBackFillerMatcher(ContextInstance entry, String contextName) {
        this.entry = entry;
        this.contextName = contextName;
    }

    @Override
    public boolean matches(ContextInstance instance) {
            return instance.getName().equals(contextName)
                && entry.getContextParameters().equals(instance.getContextParameters());
    }
}
