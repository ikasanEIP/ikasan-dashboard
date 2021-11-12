package org.ikasan.dashboard.notification.scheduler.model;

import java.util.List;

public class SchedulerNotification {
    private String jobName;
    private String emailBodyTemplate;
    private String emailSubjectTemplate;
    private String schedulerAgentName;
    private List<String> recipientList;
    private String cronExpression;
    private boolean isHtml;
    private Integer resultSize;
    private Long lastRunTimestamp = -1L;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getEmailBodyTemplate() {
        return emailBodyTemplate;
    }

    public void setEmailBodyTemplate(String emailBodyTemplate) {
        this.emailBodyTemplate = emailBodyTemplate;
    }

    public String getEmailSubjectTemplate() {
        return emailSubjectTemplate;
    }

    public void setEmailSubjectTemplate(String emailSubjectTemplate) {
        this.emailSubjectTemplate = emailSubjectTemplate;
    }

    public String getSchedulerAgentName() {
        return schedulerAgentName;
    }

    public void setSchedulerAgentName(String schedulerAgentName) {
        this.schedulerAgentName = schedulerAgentName;
    }

    public List<String> getRecipientList() {
        return recipientList;
    }

    public void setRecipientList(List<String> recipientList) {
        this.recipientList = recipientList;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setIsHtml(boolean html) {
        isHtml = html;
    }

    public Integer getResultSize() {
        return resultSize;
    }

    public void setResultSize(Integer resultSize) {
        this.resultSize = resultSize;
    }

    public Long getLastRunTimestamp() {
        return lastRunTimestamp;
    }

    public void setLastRunTimestamp(Long lastRunTimestamp) {
        this.lastRunTimestamp = lastRunTimestamp;
    }
}
