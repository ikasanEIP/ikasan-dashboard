package org.ikasan.spec.solr;

import java.util.List;

public interface SearchResults<DATA> {

    List<DATA> getResultList();

    long getTotalNumberOfResults();

    long getQueryResponseTime();
}
