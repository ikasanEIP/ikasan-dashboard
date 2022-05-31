package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class GenericNotificationDetails {

    private Long id;
    private String contextId;
    private String jobName;
    private MonitorType monitorType;
    private InstanceStatus status;

    public GenericNotificationDetails(Long id, String contextId, String jobName, MonitorType monitorType, InstanceStatus status) {
        this.id = id;
        this.contextId = contextId;
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public String getJobName() {
        return jobName;
    }

    public MonitorType getNotificationType() {
        return monitorType;
    }

    public InstanceStatus getStatus() {
        return status;
    }
}
