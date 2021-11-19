package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.ContextParameter;

public class ContextParameterInstance extends ContextParameter {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
