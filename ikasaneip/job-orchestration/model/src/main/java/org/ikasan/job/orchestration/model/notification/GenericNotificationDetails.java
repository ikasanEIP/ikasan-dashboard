package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class GenericNotificationDetails {

    private String agentName;
    private String contextName;
    private String childContextName;
    private String jobName;
    private String contextInstanceId;
    private MonitorType monitorType;
    private InstanceStatus status;
    private String message;
    private String fileName;
    private String filePath;
    private long firedTime;
    private long completedTime;

    public GenericNotificationDetails(String agentName, String contextName, String childContextName, String jobName, String contextInstanceId, MonitorType monitorType, InstanceStatus status) {
        this.agentName = agentName;
        this.contextName = contextName;
        this.childContextName = childContextName;
        this.jobName = jobName;
        this.monitorType = monitorType;
        this.status = status;
        this.contextInstanceId = contextInstanceId;
    }

    public String getAgentName() { return agentName; }

    public String getContextName() { return contextName; }

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFiredTime() {
        return firedTime;
    }

    public void setFiredTime(long firedTime) {
        this.firedTime = firedTime;
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }
}
