package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

public class FileEventDrivenJobBuilder extends QuartzScheduleDrivenJobBuilder {

    protected String cronExpression;
    protected String jobGroup;
    protected String timeZone;
    private String filePath;

    public FileEventDrivenJobBuilder withFilePath(String filePath) {
        this.filePath = filePath;

        return this;
    }

    public FileEventDrivenJob build() {
        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setFilePath(this.filePath);
        fileEventDrivenJob.setAgentName(super.agentName);
        fileEventDrivenJob.setIdentifier(super.agentName+"-"+super.jobName);
        fileEventDrivenJob.setJobDescription(super.description);
        fileEventDrivenJob.setJobName(super.jobName);
        fileEventDrivenJob.setContextId(super.contextId);
        fileEventDrivenJob.setCronExpression(super.cronExpression);
        fileEventDrivenJob.setTimeZone(super.timeZone);
        fileEventDrivenJob.setJobGroup(super.jobGroup);

        return fileEventDrivenJob;
    }
}
