package org.ikasan.dashboard.ui.scheduler.model;

import org.ikasan.spec.metadata.BusinessStreamMetaData;

import java.time.ZonedDateTime;
import java.util.List;

public class JobExecution {

    private String schedulerName;
    private String jobName;
    private String description;
    private List<BusinessStreamMetaData> relatedBusinessStreams;
    private ZonedDateTime nextExecution;
    private ZonedDateTime executionTime;
    private String schedulerStatus;
    private String executionStatus;

    public JobExecution(String schedulerName, String jobName, String description, List<BusinessStreamMetaData> relatedBusinessStreams
        , ZonedDateTime nextExecution, ZonedDateTime executionTime, String schedulerStatus, String executionStatus) {
        this.schedulerName = schedulerName;
        this.jobName = jobName;
        this.description = description;
        this.relatedBusinessStreams = relatedBusinessStreams;
        this.nextExecution = nextExecution;
        this.executionTime = executionTime;
        this.schedulerStatus = schedulerStatus;
        this.executionStatus = executionStatus;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getDescription() {
        return description;
    }

    public List<BusinessStreamMetaData> getRelatedBusinessStreams() {
        return relatedBusinessStreams;
    }

    public ZonedDateTime getNextExecution() {
        return nextExecution;
    }

    public String getSchedulerStatus() {
        return schedulerStatus;
    }

    public ZonedDateTime getExecutionTime() {
        return executionTime;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }
}
