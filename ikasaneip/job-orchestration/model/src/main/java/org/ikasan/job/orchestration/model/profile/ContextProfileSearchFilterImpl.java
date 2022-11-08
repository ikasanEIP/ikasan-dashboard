package org.ikasan.job.orchestration.model.profile;

import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;

import java.util.List;

public class ContextProfileSearchFilterImpl implements ContextProfileSearchFilter {

    private String profileName;
    private String contextName;
    private String owner;
    private List<String> accessRoles;
    private String user;

    @Override
    public String getProfileName() {
        return this.profileName;
    }

    @Override
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public List<String> getAccessRoles() {
        return this.accessRoles;
    }

    @Override
    public void setAccessRoles(List<String> accessRoles) {
        this.accessRoles = accessRoles;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

}
