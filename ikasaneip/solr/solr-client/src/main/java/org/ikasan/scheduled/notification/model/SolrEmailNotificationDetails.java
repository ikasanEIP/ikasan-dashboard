package org.ikasan.scheduled.notification.model;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrEmailNotificationDetails implements EmailNotificationDetails {

    private String jobName;

    private String contextName;

    private String monitorType;

    private Map<String,String> emailNotificationTemplateParameters;

    private List<String> emailSendTo;

    private List<String> emailSendCc;

    private List<String> emailSendBcc;

    private String emailSubject;

    private String emailBody;

    private String emailSubjectTemplate;

    private String emailBodyTemplate;

    private String attachment;

    private boolean isHtml;

    public SolrEmailNotificationDetails() {
    }

    public SolrEmailNotificationDetails(String jobName, String contextName, String monitorType, Map<String,String> emailNotificationTemplateParameters, List<String> emailSendTo,
                                        List<String> emailSendCc, List<String> emailSendBcc, String emailSubject, String emailBody, String emailSubjectTemplate,
                                        String emailBodyTemplate, String attachment, boolean isHtml) {
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.emailNotificationTemplateParameters = emailNotificationTemplateParameters;
        this.emailSendTo = emailSendTo;
        this.emailSendCc = emailSendCc;
        this.emailSendBcc = emailSendBcc;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.emailSubjectTemplate = emailSubjectTemplate;
        this.emailBodyTemplate = emailBodyTemplate;
        this.attachment = attachment;
        this.isHtml = isHtml;
        this.contextName = contextName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public List<String> getEmailSendTo() {
        return emailSendTo;
    }

    public void setEmailSendTo(List<String> emailSendTo) {
        this.emailSendTo = emailSendTo;
    }

    public List<String> getEmailSendCc() {
        return emailSendCc;
    }

    public void setEmailSendCc(List<String> emailSendCc) {
        this.emailSendCc = emailSendCc;
    }

    public List<String> getEmailSendBcc() {
        return emailSendBcc;
    }

    public void setEmailSendBcc(List<String> emailSendBcc) {
        this.emailSendBcc = emailSendBcc;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public String getEmailSubjectTemplate() {
        return emailSubjectTemplate;
    }

    public void setEmailSubjectTemplate(String emailSubjectTemplate) {
        this.emailSubjectTemplate = emailSubjectTemplate;
    }

    public String getEmailBodyTemplate() {
        return emailBodyTemplate;
    }

    public void setEmailBodyTemplate(String emailBodyTemplate) {
        this.emailBodyTemplate = emailBodyTemplate;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setHtml(boolean html) {
        isHtml = html;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public Map<String,String> getEmailNotificationTemplateParameters() {
        if (emailNotificationTemplateParameters == null) {
            emailNotificationTemplateParameters = new HashMap<>();
        }
        return emailNotificationTemplateParameters;
    }

    @Override
    public void setEmailNotificationTemplateParameters(Map<String,String> emailNotificationTemplateParameters) {
        this.emailNotificationTemplateParameters = emailNotificationTemplateParameters;
    }

    @Override
    public String toString() {
        return "SolrEmailNotificationDetails{" +
            "jobName='" + jobName + '\'' +
            ", contextName=" + contextName +
            ", monitorType=" + monitorType +
            ", emailNotificationTemplateParameters=" + emailNotificationTemplateParameters +
            ", emailSendTo=" + emailSendTo +
            ", emailSendCc=" + emailSendCc +
            ", emailSendBcc=" + emailSendBcc +
            ", emailSubject='" + emailSubject + '\'' +
            ", emailBody='" + emailBody + '\'' +
            ", emailSubjectTemplate='" + emailSubjectTemplate + '\'' +
            ", emailBodyTemplate='" + emailBodyTemplate + '\'' +
            ", attachment='" + attachment + '\'' +
            ", isHtml=" + isHtml +
            '}';
    }
}
