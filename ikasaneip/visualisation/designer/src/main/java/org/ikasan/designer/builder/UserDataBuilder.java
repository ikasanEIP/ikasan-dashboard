package org.ikasan.designer.builder;

import org.ikasan.designer.model.UserData;

import java.util.ArrayList;
import java.util.List;

public class UserDataBuilder {
    private String jobName;
    private String agentName;
    private String identifier;
    private String contextName;
    private String itemType;
    private String componentName;

    private List<String> previousJobIdentifiers = new ArrayList<>();
    private List<String> subsequentJobIdentifiers = new ArrayList<>();

    /**
     * Sets the job name for the user data being built.
     *
     * @param jobName the name of the job to set
     * @return the current instance of {@code UserDataBuilder} for method chaining
     */
    public UserDataBuilder withJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * Sets the agent name for the user data being built.
     *
     * @param agentName the name of the agent to be set
     * @return the current instance of {@code UserDataBuilder} for method chaining
     */
    public UserDataBuilder withAgentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    /**
     * Sets the identifier for the user data being built.
     *
     * @param identifier the unique identifier to be assigned
     * @return the current instance of {@code UserDataBuilder} for method chaining
     */
    public UserDataBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Sets the context name for the user data being built.
     *
     * @param contextName the name of the context to be set
     * @return the current instance of {@code UserDataBuilder} for method chaining
     */
    public UserDataBuilder withContextName(String contextName) {
        this.contextName = contextName;
        return this;
    }

    /**
     * Sets the item type for the {@code UserDataBuilder} instance.
     *
     * @param itemType the type of the item to be set
     * @return the current {@code UserDataBuilder} instance for method chaining
     */
    public UserDataBuilder withItemType(String itemType) {
        this.itemType = itemType;
        return this;
    }

    /**
     * Sets the component name for the UserDataBuilder and returns the updated builder instance.
     *
     * @param componentName the name of the component to set
     * @return the updated UserDataBuilder instance
     */
    public UserDataBuilder withComponentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    /**
     * Adds a previous job identifier to the list of previous job identifiers.
     *
     * @param identifier the identifier of the previous job to be added
     * @return the current instance of {@code UserDataBuilder} for method chaining
     */
    public UserDataBuilder addPreviousJobIdentifiers(String identifier) {
        this.previousJobIdentifiers.add(identifier);
        return this;
    }

    /**
     * Adds the specified identifier to the list of subsequent job identifiers.
     *
     * @param identifier the identifier of the subsequent job to add
     * @return the {@code UserDataBuilder} instance for method chaining
     */
    public UserDataBuilder addSubsequentJobIdentifiers(String identifier) {
        subsequentJobIdentifiers.add(identifier);
        return this;
    }

    /**
     * Builds a new instance of {@code UserData} using the current state of
     * the {@code UserDataBuilder}.
     *
     * @return an instance of {@code UserData} populated with the values
     *         set in the builder.
     */
    public UserData build() {
        return new UserData(this.jobName, this.agentName, this.identifier, this.contextName
            , this.itemType, this.componentName, this.previousJobIdentifiers, this.subsequentJobIdentifiers);
    }
}
