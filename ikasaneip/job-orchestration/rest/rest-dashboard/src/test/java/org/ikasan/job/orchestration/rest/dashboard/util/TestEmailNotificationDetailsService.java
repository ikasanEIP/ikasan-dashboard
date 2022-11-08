package org.ikasan.job.orchestration.rest.dashboard.util;

import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.EmailNotificationDetailsRecordRestImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestEmailNotificationDetailsService implements EmailNotificationDetailsService {

    private Map<String, EmailNotificationDetailsRecord> cache = new HashMap<>();

    @Override
    public SearchResults findAll(int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
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

    @Override
    public void saveEmailNotificationDetails(List<EmailNotificationDetails> emailNotificationDetails) {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();
        emailNotificationDetails.forEach(n -> {
            EmailNotificationDetailsRecord r = new EmailNotificationDetailsRecordRestImpl();
            r.setEmailNotificationDetails(n);
            records.add(r);
        });
        this.save(records);
    }

    @Override
    public void deleteByContextName(String contextName) {
        cache.forEach((k, v) -> {
            if (v.getContextName().equals(contextName)) {
                cache.remove(k);
            }
        });
    }

    private String createKey(EmailNotificationDetailsRecord emailNotificationDetailsRecord){
        return emailNotificationDetailsRecord.getEmailNotificationDetails().getJobName()+"-"+
               emailNotificationDetailsRecord.getEmailNotificationDetails().getChildContextName()+"-"+
               emailNotificationDetailsRecord.getEmailNotificationDetails().getMonitorType();
    }
}
