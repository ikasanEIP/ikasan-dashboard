package org.ikasan.scheduled.instance.model;

import org.ikasan.scheduled.context.model.SolrContextParameterImpl;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;


public class SolrContextParameterInstanceImpl extends SolrContextParameterImpl implements ContextParameterInstance {
    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
