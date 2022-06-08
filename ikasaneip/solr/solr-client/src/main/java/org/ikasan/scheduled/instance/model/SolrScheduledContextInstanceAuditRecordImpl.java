package org.ikasan.scheduled.instance.model;

import java.util.UUID;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl.SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID;

public class SolrScheduledContextInstanceAuditRecordImpl extends SolrScheduledContextInstanceRecordImpl {

    public SolrScheduledContextInstanceAuditRecordImpl() {
        super.id = SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID + "_" + UUID.randomUUID();
    }
}
