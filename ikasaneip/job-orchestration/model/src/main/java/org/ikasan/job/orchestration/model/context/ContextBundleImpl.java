package org.ikasan.job.orchestration.model.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.List;

public class ContextBundleImpl implements ContextBundle {

    private ContextTemplate contextTemplate;
    private List<SchedulerJob> schedulerJobs;
    private List<ContextProfileRecord> contextProfiles;

    @JsonCreator
    public ContextBundleImpl(@JsonProperty("contextTemplate") ContextTemplate contextTemplate
        , @JsonProperty("schedulerJobs") List<SchedulerJob> schedulerJobs, @JsonProperty("contextProfiles") List<ContextProfileRecord> contextProfiles) {
        this.contextTemplate = contextTemplate;
        if(this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.schedulerJobs = schedulerJobs;
        if(this.schedulerJobs == null) {
            throw new IllegalArgumentException("schedulerJobs cannot be null!");
        }
        this.contextProfiles = contextProfiles;
        if(this.contextProfiles == null) {
            throw new IllegalArgumentException("contextProfiles cannot be null!");
        }
    }

    @Override
    public ContextTemplate getContextTemplate() {
        return this.contextTemplate;
    }

    @Override
    public List<SchedulerJob> getSchedulerJobs() {
        return this.schedulerJobs;
    }

    @Override
    public List<ContextProfileRecord> getContextProfiles() {
        return this.contextProfiles;
    }
}
