package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

public class ContextParameterBuilder {
    protected String name;
    protected String defaultValue;

    public ContextParameterBuilder() {
    }

    public ContextParameterBuilder withName(String name) {
        this.name = name;

        return this;
    }

    public ContextParameterBuilder withDefaultValue(String value) {
        this.defaultValue = value;

        return this;
    }

    public ContextParameter build() {
        ContextParameter contextParameter = new ContextParameterImpl();
        contextParameter.setName(this.name);
        contextParameter.setDefaultValue(this.defaultValue);

        return contextParameter;
    }
}
