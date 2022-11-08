package org.ikasan.job.orchestration.rest.dashboard.util;

import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.EmailNotificationDetailsRecordRestImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;

import java.util.*;

public class TestEmailNotificationDetailsService implements EmailNotificationDetailsService {

    private Map<String, EmailNotificationDetailsRecord> cache = new HashMap<>();

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        List<EmailNotificationDetailsRecord> recordList = new ArrayList<>(cache.values());
        return new SearchResultsImpl<>(recordList, recordList.size(), 1L);
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        // ignore limit and offset for test implementation
        List<EmailNotificationDetailsRecord> recordList = new ArrayList<>();
        for(EmailNotificationDetailsRecord record : cache.values()) {
            if(contextName.equals(record.getContextName())) {
                recordList.add(record);
            }
        }
        return new SearchResultsImpl<>(recordList, recordList.size(), 1L);
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        return cache.get(generateId(jobName, childContextName, monitorType));
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
        Set<String> keys = new HashSet<>();
        cache.forEach((k, v) -> {
            if (contextName.equals(v.getContextName())) {
                keys.add(k);
            }
        });
        for (String k : keys) {
            cache.remove(k);
        }
    }

    @Override
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        cache.remove(generateId(jobName, childContextName, monitorType));
    }

    public String createKey(EmailNotificationDetailsRecord emailNotificationDetailsRecord){
        return generateId(emailNotificationDetailsRecord.getEmailNotificationDetails().getJobName(),
                          emailNotificationDetailsRecord.getEmailNotificationDetails().getChildContextName(),
                          emailNotificationDetailsRecord.getEmailNotificationDetails().getMonitorType());
    }

    /**
     * Generates ID so that the format is correct
     * @param jobName jobName
     * @param childContextName childContextName
     * @param monitorType of Type org.ikasan.job.orchestration.model.notification.MonitorType
     * @return format = jobName_childContextName_monitorType
     */
    private static String generateId(String jobName, String childContextName, String monitorType) {
        return jobName+"_"+childContextName+"_"+monitorType;
    }
}
