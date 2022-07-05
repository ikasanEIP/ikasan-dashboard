package org.ikasan.job.orchestration.rest.dashboard.util;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestEmailNotificationDetailsService implements EmailNotificationDetailsService<EmailNotificationDetailsRecord> {

    private Map<String, EmailNotificationDetailsRecord> cache = new HashMap<>();

    @Override
    public SearchResults findAll(int limit, int offset) {
        return null;
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String contextName, String monitorType) {
        return cache.get(jobName+"-"+contextName+"-"+monitorType);
    }

    @Override
    public void save(EmailNotificationDetailsRecord emailNotificationDetailsRecord) {
        cache.put(createKey(emailNotificationDetailsRecord), emailNotificationDetailsRecord);
    }

    @Override
    public void save(List<EmailNotificationDetailsRecord> list) {
        list.forEach(e -> cache.put(createKey(e), e));
    }

    private String createKey(EmailNotificationDetailsRecord emailNotificationDetailsRecord){
        return emailNotificationDetailsRecord.getEmailNotificationDetails().getJobName()+"-"+
               emailNotificationDetailsRecord.getEmailNotificationDetails().getContextName()+"-"+
               emailNotificationDetailsRecord.getEmailNotificationDetails().getMonitorType();
    }
}
