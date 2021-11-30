package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.ContextParameter;
import org.ikasan.spec.scheduled.ContextParameterInstance;

public class ContextParameterInstanceImpl extends ContextParameter implements ContextParameterInstance {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
