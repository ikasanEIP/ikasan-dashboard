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

    private List<String> previousJobIdentifiers = new ArrayList<>();
    private List<String> subsequentJobIdentifiers = new ArrayList<>();

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

    public UserDataBuilder addPreviousJobIdentifiers(String identifier) {
        this.previousJobIdentifiers.add(identifier);
        return this;
    }

    public UserDataBuilder addSubsequentJobIdentifiers(String identifier) {
        subsequentJobIdentifiers.add(identifier);
        return this;
    }

    public UserData build() {
        return new UserData(this.jobName, this.agentName, this.identifier, this.contextName
            , this.itemType, this.previousJobIdentifiers, this.subsequentJobIdentifiers);
    }
}
