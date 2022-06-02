package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class GenericNotificationDetails {

    private String contextInstanceId;
    private String jobName;
    private MonitorType monitorType;
    private InstanceStatus status;

    public GenericNotificationDetails(String contextInstanceId, String jobName, MonitorType monitorType, InstanceStatus status) {
        this.contextInstanceId = contextInstanceId;
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.status = status;
    }

    public String getContextInstanceId() {
        return contextInstanceId;
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
}
