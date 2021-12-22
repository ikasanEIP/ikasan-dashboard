package org.ikasan.scheduler.core.model.context;

import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;

public class ScheduledContextRecordImpl implements ScheduledContextRecord {
    private String id;
    private String contextName;
    private String context;
    private long timestamp;

    public ScheduledContextRecordImpl(String id, String contextName, String context, long timestamp) {
        this.id = id;
        this.contextName = contextName;
        this.context = context;
        this.timestamp = timestamp;
    }

    public ScheduledContextRecordImpl() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public String getContext() {
        return this.context;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
}
