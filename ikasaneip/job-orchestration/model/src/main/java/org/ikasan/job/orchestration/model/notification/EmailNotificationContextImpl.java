package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailNotificationContextImpl implements EmailNotificationContext {

    private String contextName;
    private List<String> monitorTypes;
    private List<String> emailSendTo;
    private Map<String, List<String>> emailSendToByMonitorType;
    private List<String> emailSendCc;
    private Map<String, List<String>> emailSendCcByMonitorType;
    private List<String> emailSendBcc;
    private Map<String, List<String>> emailSendBccByMonitorType;
    private Map<String, String> emailSubjectNotificationTemplate;
    private Map<String, String> emailBodyNotificationTemplate;
    private String attachment;
    private boolean isHtml;

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public List<String> getMonitorTypes() {
        return monitorTypes;
    }

    @Override
    public void setMonitorTypes(List<String> monitorTypes) {
        this.monitorTypes = monitorTypes;
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
    public Map<String, List<String>> getEmailSendToByMonitorType() {
        if (emailSendToByMonitorType == null) {
            emailSendToByMonitorType = new HashMap<>();
        }
        return emailSendToByMonitorType;
    }

    @Override
    public void setEmailSendToByMonitorType(Map<String, List<String>> emailSendToByMonitorType) {
        this.emailSendToByMonitorType = emailSendToByMonitorType;
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
    public Map<String, List<String>> getEmailSendCcByMonitorType() {
        if (emailSendCcByMonitorType == null) {
            emailSendCcByMonitorType = new HashMap<>();
        }
        return emailSendCcByMonitorType;
    }

    @Override
    public void setEmailSendCcByMonitorType(Map<String, List<String>> emailSendCcByMonitorType) {
        this.emailSendCcByMonitorType = emailSendCcByMonitorType;
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
    public Map<String, List<String>> getEmailSendBccByMonitorType() {
        if (emailSendBccByMonitorType == null) {
            emailSendBccByMonitorType = new HashMap<>();
        }
        return emailSendBccByMonitorType;
    }

    @Override
    public void setEmailSendBccByMonitorType(Map<String, List<String>> emailSendBccByMonitorType) {
        this.emailSendBccByMonitorType = emailSendBccByMonitorType;
    }

    @Override
    public Map<String, String> getEmailSubjectNotificationTemplate() {
        return emailSubjectNotificationTemplate;
    }

    @Override
    public void setEmailSubjectNotificationTemplate(Map<String, String> emailSubjectNotificationTemplate) {
        this.emailSubjectNotificationTemplate = emailSubjectNotificationTemplate;
    }

    @Override
    public Map<String, String> getEmailBodyNotificationTemplate() {
        return emailBodyNotificationTemplate;
    }

    @Override
    public void setEmailBodyNotificationTemplate(Map<String, String> emailBodyNotificationTemplate) {
        this.emailBodyNotificationTemplate = emailBodyNotificationTemplate;
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
