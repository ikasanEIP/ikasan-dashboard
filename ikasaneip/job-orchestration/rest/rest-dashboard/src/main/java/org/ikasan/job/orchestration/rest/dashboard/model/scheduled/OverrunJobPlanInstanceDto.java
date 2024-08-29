package org.ikasan.job.orchestration.rest.dashboard.model.scheduled;

public class OverrunJobPlanInstanceDto {
    private String jobPlanInstanceId;
    private String jobPlanName;
    private long jobPlanStartTimestamp;
    private long jobPlanProjectedEndTimestamp;
    private String timezone;

    public OverrunJobPlanInstanceDto(String jobPlanInstanceId, String jobPlanName, long jobPlanStartTimestamp
        , long jobPlanProjectedEndTimestamp, String timezone) {
        this.jobPlanInstanceId = jobPlanInstanceId;
        this.jobPlanName = jobPlanName;
        this.jobPlanStartTimestamp = jobPlanStartTimestamp;
        this.jobPlanProjectedEndTimestamp = jobPlanProjectedEndTimestamp;
        this.timezone = timezone;
    }

    public String getJobPlanInstanceId() {
        return jobPlanInstanceId;
    }

    public String getJobPlanName() {
        return jobPlanName;
    }

    public long getJobPlanStartTimestamp() {
        return jobPlanStartTimestamp;
    }

    public long getJobPlanProjectedEndTimestamp() {
        return jobPlanProjectedEndTimestamp;
    }

    public String getTimezone() {
        return timezone;
    }
}
