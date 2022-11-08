package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class GenericNotificationDetails {

    private String childContextName;
    private String jobName;
    private String contextInstanceId;
    private MonitorType monitorType;
    private InstanceStatus status;
    private String message;

    public GenericNotificationDetails(String childContextName, String jobName, String contextInstanceId, MonitorType monitorType, InstanceStatus status) {
        this.childContextName = childContextName;
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.status = status;
        this.contextInstanceId = contextInstanceId;
    }

    public String getChildContextName() {
        return childContextName;
    }

    public MonitorType getMonitorType() {
        return monitorType;
    }

    public String getJobName() {
        return jobName;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public String getContextInstanceId() {
        return contextInstanceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
