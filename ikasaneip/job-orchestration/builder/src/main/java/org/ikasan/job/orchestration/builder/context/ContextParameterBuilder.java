package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

public class ContextParameterBuilder {
    protected String name;
    protected String type;

    public ContextParameterBuilder() {
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
