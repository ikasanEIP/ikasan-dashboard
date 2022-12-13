package org.ikasan.job.orchestration.model.context;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContextTransition {
    private SchedulerJob proceedingJob;
    private SchedulerJob subsequentJob;
    private List<String> contexts = new ArrayList<>();;

    public SchedulerJob getPrecedingJob() {
        return proceedingJob;
    }

    public void setProceedingJob(SchedulerJob proceedingJob) {
        this.proceedingJob = proceedingJob;
    }

    public SchedulerJob getSubsequentJob() {
        return subsequentJob;
    }

    public void setSubsequentJob(SchedulerJob subsequentJob) {
        this.subsequentJob = subsequentJob;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public void setContexts(List<String> contexts) {
        this.contexts = contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextTransition)) return false;
        ContextTransition that = (ContextTransition) o;
        return Objects.equals(proceedingJob, that.proceedingJob)
            && Objects.equals(subsequentJob, that.subsequentJob)
            && Objects.equals(contexts, that.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proceedingJob, subsequentJob, contexts);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
