package org.ikasan.orchestration.service.utils;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.mockito.ArgumentMatcher;

public class CustomBackFillerMatcher implements ArgumentMatcher<ContextInstance> {

    private final ContextInstance entry;
    private final String contextName;

    public CustomBackFillerMatcher(ContextInstance entry, String contextName) {
        this.entry = entry;
        this.contextName = contextName;
    }

    @Override
    public boolean matches(ContextInstance instance) {
            return instance.getName().equals(contextName)
                && (entry.getContextParameters() == null && instance.getContextParameters() == null
                || entry.getContextParameters().equals(instance.getContextParameters()));
    }
}
