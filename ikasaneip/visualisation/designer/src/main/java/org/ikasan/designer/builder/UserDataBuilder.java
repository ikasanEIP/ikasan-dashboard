package org.ikasan.designer.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ikasan.designer.model.UserData;

public class UserDataBuilder {
    private String jobName;
    private String agentName;
    private String identifier;

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

    public UserData build() {
        return new UserData(this.jobName, this.agentName, this.identifier);
    }
}
