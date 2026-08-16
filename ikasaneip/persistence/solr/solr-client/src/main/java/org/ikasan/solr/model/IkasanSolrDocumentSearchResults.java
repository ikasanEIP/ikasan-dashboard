package org.ikasan.solr.model;

import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;

import java.util.List;

/**
 * Solr implementation of IkasanDocumentSearchResults.
 * This class encapsulates search results from Solr queries.
 *
 * Created by Ikasan Development Team on 05/08/2017.
 */
public class IkasanSolrDocumentSearchResults implements IkasanDocumentSearchResults
{
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
    public IkasanSolrDocumentSearchResults(List<IkasanESBDocument> resultList, long totalNumberOfResults, long queryResponseTime)
    {
        this.resultList = resultList;
        this.totalNumberOfResults = totalNumberOfResults;
        this.queryResponseTime = queryResponseTime;
    }

    @Override
    public List<IkasanESBDocument> getResultList()
    {
        return resultList;
    }

    @Override
    public long getTotalNumberOfResults()
    {
        return totalNumberOfResults;
    }

    @Override
    public long getQueryResponseTime()
    {
        return queryResponseTime;
    }
}
