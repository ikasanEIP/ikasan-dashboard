package org.ikasan.job.orchestration.model.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.List;

public class ContextBundleImpl implements ContextBundle {

    private ContextTemplate contextTemplate;
    private List<SchedulerJob> schedulerJobs;
    private List<ContextProfileRecord> contextProfiles;
    private List<EmailNotificationDetails> emailNotificationDetails;
    private EmailNotificationContext emailNotificationContexts;

    @JsonCreator
    public ContextBundleImpl(@JsonProperty("contextTemplate") ContextTemplate contextTemplate,
                             @JsonProperty("schedulerJobs") List<SchedulerJob> schedulerJobs,
                             @JsonProperty("contextProfiles") List<ContextProfileRecord> contextProfiles,
                             @JsonProperty("emailNotificationDetails") List<EmailNotificationDetails> emailNotificationDetails,
                             @JsonProperty("emailNotificationContext") EmailNotificationContext emailNotificationContexts) {
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
        this.emailNotificationDetails = emailNotificationDetails;
        if(this.emailNotificationDetails == null) {
            throw new IllegalArgumentException("emailNotificationDetails cannot by null!");
        }
        // this can be null
        this.emailNotificationContexts = emailNotificationContexts;
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

    @Override
    public List<EmailNotificationDetails> getEmailNotificationDetails() {
        return this.emailNotificationDetails;
    }

    @Override
    public EmailNotificationContext getEmailNotificationContext() {
        return this.emailNotificationContexts;
    }
}
