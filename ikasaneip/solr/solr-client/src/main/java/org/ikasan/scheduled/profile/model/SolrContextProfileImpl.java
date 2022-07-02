package org.ikasan.scheduled.profile.model;

import org.ikasan.spec.scheduled.profile.model.ContextProfile;

import java.util.ArrayList;
import java.util.List;

public class SolrContextProfileImpl implements ContextProfile {
    private List<String> subContexts = new ArrayList<>();
    private String defaultContext;

    @Override
    public String getDefaultContext() {
        return defaultContext;
    }

    @Override
    public void setDefaultContext(String defaultContext) {
        this.defaultContext = defaultContext;
    }

    @Override
    public List<String> getSubContexts() {
        return this.subContexts;
    }

    @Override
    public void setSubContexts(List<String> subContexts) {
        this.subContexts = subContexts;
    }
}
