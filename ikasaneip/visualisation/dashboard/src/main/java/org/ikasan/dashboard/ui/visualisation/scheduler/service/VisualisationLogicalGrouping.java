package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.ArrayList;
import java.util.List;

public class VisualisationLogicalGrouping {
    private String type;
    private List<String> jobIdentifiers = new ArrayList<>();
    private List<VisualisationLogicalGrouping> nestedVisualisationLogicalGrouping = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getJobIdentifiers() {
        return jobIdentifiers;
    }

    public void setJobIdentifiers(List<String> jobIdentifiers) {
        this.jobIdentifiers = jobIdentifiers;
    }

    public List<VisualisationLogicalGrouping> getNestedGrouping() {
        return nestedVisualisationLogicalGrouping;
    }

    public void setNestedGrouping(List<VisualisationLogicalGrouping> nestedVisualisationLogicalGrouping) {
        this.nestedVisualisationLogicalGrouping = nestedVisualisationLogicalGrouping;
    }
}
