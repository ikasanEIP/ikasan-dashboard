package org.ikasan.dashboard.ui.scheduler.model;

import java.util.List;

public interface SearchResults<DATA> {

    List<DATA> getResultList();

    long getTotalNumberOfResults();

    long getQueryResponseTime();
}
