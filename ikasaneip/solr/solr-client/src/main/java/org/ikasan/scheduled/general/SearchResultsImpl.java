package org.ikasan.scheduled.general;


import org.ikasan.spec.search.SearchResults;

import java.util.List;

public class SearchResultsImpl<T> implements SearchResults<T> {
    private List<T> results;
    private long totalNumberOfResults;
    private long queryResponseTime;

    public SearchResultsImpl(List<T> results, long totalNumberOfResults, long queryResponseTime) {
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
