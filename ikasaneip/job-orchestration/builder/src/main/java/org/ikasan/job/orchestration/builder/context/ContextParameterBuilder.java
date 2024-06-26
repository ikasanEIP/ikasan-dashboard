package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;

public class ContextParameterBuilder {
    protected String name;
    protected String defaultValue;

    /**
     * The ContextParameterBuilder class is used to build ContextParameter objects.
     */
    public ContextParameterBuilder() {
    }

    /**
     * Sets the name of the context parameter.
     *
     * @param name the name of the context parameter
     * @return the updated ContextParameterBuilder object
     */
    public ContextParameterBuilder withName(String name) {
        this.name = name;

        return this;
    }

    /**
     * Sets the default value for a context parameter.
     *
     * @param value the default value for the context parameter
     * @return the updated ContextParameterBuilder object
     */
    public ContextParameterBuilder withDefaultValue(String value) {
        this.defaultValue = value;

        return this;
    }

    /**
     * Builds a ContextParameter object with the provided name and default value.
     *
     * @return the built ContextParameter object
     */
    public ContextParameter build() {
        ContextParameter contextParameter = new ContextParameterImpl();
        contextParameter.setName(this.name);
        contextParameter.setDefaultValue(this.defaultValue);

        return contextParameter;
    }
}
