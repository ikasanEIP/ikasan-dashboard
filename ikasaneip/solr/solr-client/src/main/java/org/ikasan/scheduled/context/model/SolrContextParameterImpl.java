package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.ContextParameter;

public class SolrContextParameterImpl implements ContextParameter {
    private String name;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
