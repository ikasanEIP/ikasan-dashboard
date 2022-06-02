package org.ikasan.scheduled.notification.dao;

import org.ikasan.job.orchestration.model.notification.EmailNotificationDetails;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

public interface EmailNotificationDetailsDao<T extends EmailNotificationDetails> {

    SearchResults<? extends T> findAll(int limit, int offset);

    T findByJobNameAndMonitorType(String jobName, String monitorType);

    void save(T var1);

    void save(List<T> var1);
}