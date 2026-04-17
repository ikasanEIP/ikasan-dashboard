package org.ikasan.scheduled.notification.model;

import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;

public class SolrNotificationSendAudit implements NotificationSendAudit {

    private String jobName;

    private String contextInstanceId;

    private String contextName;

    private String monitorType;

    private String notifierType;

    private boolean isNotificationSend;

    public SolrNotificationSendAudit() {
    }

    public SolrNotificationSendAudit(String jobName, String contextInstanceId, String contextName, String monitorType, String notifierType, boolean isNotificationSend) {
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.contextInstanceId = contextInstanceId;
        this.contextName = contextName;
        this.notifierType = notifierType;
        this.isNotificationSend = isNotificationSend;
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
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
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
    public String getNotifierType() {
        return notifierType;
    }

    @Override
    public void setNotifierType(String notifierType) {
        this.notifierType = notifierType;
    }

    @Override
    public boolean isNotificationSend() {
        return isNotificationSend;
    }

    @Override
    public void setNotificationSend(boolean notificationSend) {
        isNotificationSend = notificationSend;
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
    public String toString() {
        return "SolrNotificationSendAudit{" +
                "jobName='" + jobName + '\'' +
                ", contextInstanceId='" + contextInstanceId + '\'' +
                ", contextName='" + contextName + '\'' +
                ", monitorType='" + monitorType + '\'' +
                ", notifierType='" + notifierType + '\'' +
                ", isNotificationSend=" + isNotificationSend +
                '}';
    }
}
