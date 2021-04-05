package org.ikasan.dashboard.ui.scheduler.model;

import java.util.List;

public class UpcomingJobExecutionSearchResults implements SearchResults<JobExecution> {
    private List<JobExecution> jobExecutions;
    private long totalNumberOfResults;
    private long queryResponseTime;

    /**
     * Consrtuctor
     *
     * @param jobExecutions
     * @param totalNumberOfResults
     * @param queryResponseTime
     */
    public UpcomingJobExecutionSearchResults(List<JobExecution> jobExecutions, long totalNumberOfResults, long queryResponseTime) {
        this.jobExecutions = jobExecutions;
        this.totalNumberOfResults = totalNumberOfResults;
        this.queryResponseTime = queryResponseTime;
    }

    @Override
    public List<JobExecution> getResultList() {
        return this.jobExecutions;
    }

    @Override
    public long getTotalNumberOfResults() {
        return this.totalNumberOfResults;
    }

    @Override
    public long getQueryResponseTime() {
        return this.queryResponseTime;
    }
}
