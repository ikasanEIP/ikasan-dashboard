package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.ArrayList;
import java.util.List;

public class Grouping {
    private String type;
    private List<String> jobIdentifiers = new ArrayList<>();
    private List<Grouping> nestedGrouping = new ArrayList<>();

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

    public List<Grouping> getNestedGrouping() {
        return nestedGrouping;
    }

    public void setNestedGrouping(List<Grouping> nestedGrouping) {
        this.nestedGrouping = nestedGrouping;
    }
}
