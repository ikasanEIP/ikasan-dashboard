package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

public class ContextParameterBuilder {
    protected String name;
    protected String type;

    protected ContextParameterBuilder() {
    }

    public ContextParameterBuilder withName(String name) {
        this.name = name;

        return this;
    }

    public ContextParameterBuilder withType(String type) {
        this.type = type;

        return this;
    }

    public ContextParameter build() {
        ContextParameter contextParameter = new ContextParameterImpl();
        contextParameter.setName(this.name);
        contextParameter.setType(this.type);

        return contextParameter;
    }
}
