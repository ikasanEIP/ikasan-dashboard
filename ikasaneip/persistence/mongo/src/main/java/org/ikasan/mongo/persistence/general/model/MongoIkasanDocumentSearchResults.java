package org.ikasan.mongo.persistence.general.model;

import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;

import java.util.List;

/**
 * MongoDB implementation for Ikasan document search results.
 * This class encapsulates search results from MongoDB queries.
 */
public class MongoIkasanDocumentSearchResults implements IkasanDocumentSearchResults {

    private List<IkasanESBDocument> resultList;
    private long totalNumberOfResults;
    private long queryResponseTime;

    /**
     * Constructor
     *
     * @param resultList the list of documents in this result set
     * @param totalNumberOfResults the total number of results matching the query
     * @param queryResponseTime the time taken to execute the query in milliseconds
     */
    public MongoIkasanDocumentSearchResults(List<IkasanESBDocument> resultList, long totalNumberOfResults, long queryResponseTime) {
        this.resultList = resultList;
        this.totalNumberOfResults = totalNumberOfResults;
        this.queryResponseTime = queryResponseTime;
    }

    public List<IkasanESBDocument> getResultList() {
        return resultList;
    }

    public long getTotalNumberOfResults() {
        return totalNumberOfResults;
    }

    public long getQueryResponseTime() {
        return queryResponseTime;
    }
}
