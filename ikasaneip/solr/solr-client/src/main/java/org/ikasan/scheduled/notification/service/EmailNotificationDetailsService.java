package org.ikasan.scheduled.notification.service;

import org.ikasan.job.orchestration.model.notification.EmailNotificationDetails;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

public interface EmailNotificationDetailsService<T extends EmailNotificationDetails> {

    SearchResults<? extends T> findAll(int limit, int offset);

    T findByJobNameAndMonitorType(String jobName, String monitorType);

    void save(T var1);

    void save(List<T> var1);
}