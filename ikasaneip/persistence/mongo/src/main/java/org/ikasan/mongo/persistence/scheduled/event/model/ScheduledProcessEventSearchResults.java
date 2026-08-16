package org.ikasan.mongo.persistence.scheduled.event.model;

import org.ikasan.spec.search.SearchResults;

import java.util.List;

/**
 * Search results wrapper for ScheduledProcessEvent queries.
 *
 * @author Ikasan Development Team
 */
public class ScheduledProcessEventSearchResults<T> implements SearchResults<T> {

    private List<T> results;
    private long totalCount;
    private long queryTime;

    /**
     * Constructor
     *
     * @param results the list of results
     * @param totalCount the total count of results
     * @param queryTime the query execution time in milliseconds
     */
    public ScheduledProcessEventSearchResults(List<T> results, long totalCount, long queryTime) {
        this.results = results;
        this.totalCount = totalCount;
        this.queryTime = queryTime;
    }

    @Override
    public List<T> getResultList() {
        return this.results;
    }

    @Override
    public long getTotalNumberOfResults() {
        return this.totalCount;
    }

    @Override
    public long getQueryResponseTime() {
        return this.queryTime;
    }
}
