package org.ikasan.designer.builder;

import org.ikasan.designer.model.UserData;

public class UserDataBuilder {
    private String jobName;
    private String agentName;
    private String identifier;
    private String contextName;
    private String itemType;

    public UserDataBuilder withJobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public UserDataBuilder withAgentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public UserDataBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public UserDataBuilder withContextName(String contextName) {
        this.contextName = contextName;
        return this;
    }

    public UserDataBuilder withItemType(String itemType) {
        this.itemType = itemType;
        return this;
    }

    public UserData build() {
        return new UserData(this.jobName, this.agentName, this.identifier, this.contextName, this.itemType);
    }
}
