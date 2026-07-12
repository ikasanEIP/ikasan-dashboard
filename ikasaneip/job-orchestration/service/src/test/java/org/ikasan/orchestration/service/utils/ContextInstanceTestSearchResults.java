package org.ikasan.orchestration.service.utils;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ikasan.spec.scheduled.instance.model.InstanceStatus.RUNNING;
import static org.ikasan.spec.scheduled.instance.model.InstanceStatus.WAITING;

public class ContextInstanceTestSearchResults implements SearchResults<ScheduledContextInstanceRecord> {
    private final int number;
    private final boolean insideOperatingWindow;
    private static final JsonMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    public ContextInstanceTestSearchResults(int number, boolean insideOperatingWindow) {
        this.number = number;
        this.insideOperatingWindow = insideOperatingWindow;
    }

    @Override
    public List<ScheduledContextInstanceRecord> getResultList() {
        List<ScheduledContextInstanceRecord> results = new ArrayList<>();
        for (int i = 1; i < number + 1; i++) {
            String jsonContext;
            try {
                jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException("could not find or parse file data/context.json");
            }
            jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + ("ContextName" + i) + "\"");
            if (insideOperatingWindow) {
                // make operating window 24 hours
                jsonContext = jsonContext.replaceAll("\"timeWindowStart\".*", "\"timeWindowStart\" : \"" + "* * 0 ? * * *" + "\"" + ",");
                jsonContext = jsonContext.replaceAll("\"projectedEndTime\".*", "\"projectedEndTime\" : " + (System.currentTimeMillis() + 86400000) + ",");
            } else {
                jsonContext = jsonContext.replaceAll("\"timeWindowStart\".*", "\"timeWindowStart\" : \"" + "59 59 23 ? * * *" + "\"" + ",");
                jsonContext = jsonContext.replaceAll("\"projectedEndTime\".*", "\"projectedEndTime\" : " + System.currentTimeMillis() + ",");
            }

            ContextInstanceImpl contextInstance;
            try {
                contextInstance = OBJECT_MAPPER.readValue(jsonContext, ContextInstanceImpl.class);
            } catch (JacksonException e) {
                throw new RuntimeException(e.getMessage());

            }
            ScheduledContextInstanceRecordImpl record = createRecord(contextInstance, System.currentTimeMillis(), RUNNING.name());
            results.add(record);
            // add multiple instances of the same context name - should have different ids
            for (int j = 1; j < number + 1; j++) {
                ScheduledContextInstanceRecordImpl newRecord = createRecord(contextInstance, System.currentTimeMillis() - (number * 1000L), WAITING.name());
                results.add(newRecord);
            }
        }
        return results;
    }

    private ScheduledContextInstanceRecordImpl createRecord(ContextInstanceImpl contextInstance, long timestamp, String status) {
        ScheduledContextInstanceRecordImpl record = new ScheduledContextInstanceRecordImpl();
        record.setContextName(contextInstance.getName());
        record.setStatus(status);
        record.setContextInstance(contextInstance);
        record.setTimestamp(timestamp);
        return record;
    }

    @Override
    public long getTotalNumberOfResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getQueryResponseTime() {
        throw new UnsupportedOperationException();
    }
}
