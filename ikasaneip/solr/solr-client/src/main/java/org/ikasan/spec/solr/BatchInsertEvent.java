package org.ikasan.spec.solr;

import java.util.List;

public class BatchInsertEvent<BATCH_EVENT> {
    private List<BATCH_EVENT> events;

    public BatchInsertEvent(List<BATCH_EVENT> events) {
        this.events = events;
    }

    public List<BATCH_EVENT> getEvents() {
        return events;
    }
}
