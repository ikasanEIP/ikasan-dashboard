package org.ikasan.scheduled.event.model;

import org.ikasan.spec.solr.SearchResults;

import java.util.List;

public class ScheduledProcessEventSearchResults<T> implements SearchResults<T> {
    private List<T> results;
    private long totalNumberOfResults;
    private long queryResponseTime;

    public ScheduledProcessEventSearchResults(List<T> results, long totalNumberOfResults, long queryResponseTime) {
        this.results = results;
        this.totalNumberOfResults = totalNumberOfResults;
        this.queryResponseTime = queryResponseTime;
    }

    @Override
    public List<T> getResultList() {
        return this.results;
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
