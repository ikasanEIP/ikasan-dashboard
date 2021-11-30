package org.ikasan.scheduler.core.model.instance;

import org.ikasan.spec.scheduled.context.model.ScheduledContextInstanceRecord;

public class ScheduledContextInstanceRecordImpl implements ScheduledContextInstanceRecord {
    private String id;
    private String contextName;
    private String contextInstance;
    private String status;
    private long timestamp;

    public ScheduledContextInstanceRecordImpl(String id, String contextName, String contextInstance, long timestamp) {
        this.id = id;
        this.contextName = contextName;
        this.contextInstance = contextInstance;
        this.timestamp = timestamp;
    }

    public ScheduledContextInstanceRecordImpl() {
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
    public String getContextInstance() {
        return this.contextInstance;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }
}
