package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameterInstance;

public class ContextParameterInstanceImpl extends ContextParameterImpl implements ContextParameterInstance {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
