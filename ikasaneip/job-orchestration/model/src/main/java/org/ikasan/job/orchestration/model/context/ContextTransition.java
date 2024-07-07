package org.ikasan.job.orchestration.model.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContextTransition {
    private SchedulerJob precedingJob;
    private SchedulerJob subsequentJob;
    private boolean transitionDueToEventDependency = false;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> contexts = new ArrayList<>();;

    public SchedulerJob getPrecedingJob() {
        return precedingJob;
    }

    public void setPrecedingJob(SchedulerJob precedingJob) {
        this.precedingJob = precedingJob;
    }

    public SchedulerJob getSubsequentJob() {
        return subsequentJob;
    }

    public void setSubsequentJob(SchedulerJob subsequentJob) {
        this.subsequentJob = subsequentJob;
    }

    public boolean isTransitionDueToEventDependency() {
        return transitionDueToEventDependency;
    }

    public void setTransitionDueToEventDependency(boolean transitionDueToEventDependency) {
        this.transitionDueToEventDependency = transitionDueToEventDependency;
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
        return Objects.equals(precedingJob, that.precedingJob)
            && Objects.equals(subsequentJob, that.subsequentJob)
            && Objects.equals(contexts, that.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(precedingJob, subsequentJob, contexts);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
