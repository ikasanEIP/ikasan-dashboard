package org.ikasan.relational.persistence.scheduled.notification.model;

import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;

/**
 * Hibernate implementation of NotificationSendAudit.
 *
 * Represents an audit record of notification send attempts, tracking whether
 * a notification was successfully sent for a specific job, context instance,
 * monitor type, and notifier type combination.
 */
public class HibernateNotificationSendAudit implements NotificationSendAudit {

    private String jobName;
    private String contextInstanceId;
    private String contextName;
    private String monitorType;
    private String notifierType;
    private boolean isNotificationSend;

    public HibernateNotificationSendAudit() {
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
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
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
}
