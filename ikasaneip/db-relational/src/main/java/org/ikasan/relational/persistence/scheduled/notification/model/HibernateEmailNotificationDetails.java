package org.ikasan.relational.persistence.scheduled.notification.model;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of EmailNotificationDetails.
 *
 * Represents detailed email notification information for a specific job and monitor type,
 * including recipients, templates, template parameters, and content.
 */
public class HibernateEmailNotificationDetails implements EmailNotificationDetails {

    private String jobName;
    private String contextName;
    private String childContextName;
    private String monitorType;
    private Map<String, String> emailNotificationTemplateParameters;
    private List<String> emailSendTo;
    private List<String> emailSendCc;
    private List<String> emailSendBcc;
    private String emailSubject;
    private String emailBody;
    private String emailSubjectTemplate;
    private String emailBodyTemplate;
    private String attachment;
    private boolean isHtml;

    public HibernateEmailNotificationDetails() {
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getChildContextName() {
        return childContextName;
    }

    @Override
    public void setChildContextName(String childContextName) {
        this.childContextName = childContextName;
    }

    @Override
    public String getMonitorType() {
        return monitorType;
    }

    @Override
    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    @Override
    public Map<String, String> getEmailNotificationTemplateParameters() {
        if (emailNotificationTemplateParameters == null) {
            emailNotificationTemplateParameters = new HashMap<>();
        }
        return emailNotificationTemplateParameters;
    }

    @Override
    public void setEmailNotificationTemplateParameters(Map<String, String> emailNotificationTemplateParameters) {
        this.emailNotificationTemplateParameters = emailNotificationTemplateParameters;
    }

    @Override
    public List<String> getEmailSendTo() {
        return emailSendTo;
    }

    @Override
    public void setEmailSendTo(List<String> emailSendTo) {
        this.emailSendTo = emailSendTo;
    }

    @Override
    public List<String> getEmailSendCc() {
        return emailSendCc;
    }

    @Override
    public void setEmailSendCc(List<String> emailSendCc) {
        this.emailSendCc = emailSendCc;
    }

    @Override
    public List<String> getEmailSendBcc() {
        return emailSendBcc;
    }

    @Override
    public void setEmailSendBcc(List<String> emailSendBcc) {
        this.emailSendBcc = emailSendBcc;
    }

    @Override
    public String getEmailSubject() {
        return emailSubject;
    }

    @Override
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    @Override
    public String getEmailBody() {
        return emailBody;
    }

    @Override
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    @Override
    public String getEmailSubjectTemplate() {
        return emailSubjectTemplate;
    }

    @Override
    public void setEmailSubjectTemplate(String emailSubjectTemplate) {
        this.emailSubjectTemplate = emailSubjectTemplate;
    }

    @Override
    public String getEmailBodyTemplate() {
        return emailBodyTemplate;
    }

    @Override
    public void setEmailBodyTemplate(String emailBodyTemplate) {
        this.emailBodyTemplate = emailBodyTemplate;
    }

    @Override
    public String getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public boolean isHtml() {
        return isHtml;
    }

    @Override
    public void setHtml(boolean html) {
        isHtml = html;
    }
}
