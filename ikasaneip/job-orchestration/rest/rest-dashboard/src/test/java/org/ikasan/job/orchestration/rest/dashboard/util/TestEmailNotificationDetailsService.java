package org.ikasan.job.orchestration.rest.dashboard.util;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestEmailNotificationDetailsService implements EmailNotificationDetailsService<EmailNotificationDetails> {

    private Map<String, EmailNotificationDetails> cache = new HashMap<>();

    @Override
    public SearchResults findAll(int limit, int offset) {
        return null;
    }

    @Override
    public EmailNotificationDetails findByJobNameAndMonitorType(String jobName, String contextName, String monitorType) {
        return cache.get(jobName+"-"+contextName+"-"+monitorType);
    }

    @Override
    public void save(EmailNotificationDetails emailNotificationDetails) {
        cache.put(createKey(emailNotificationDetails), emailNotificationDetails);
    }

    @Override
    public void save(List<EmailNotificationDetails> list) {
        list.forEach(e -> cache.put(createKey(e), e));
    }

    private String createKey(EmailNotificationDetails emailNotificationDetails){
        return emailNotificationDetails.getJobName()+"-"+emailNotificationDetails.getContextName()+"-"+emailNotificationDetails.getMonitorType();
    }
}
