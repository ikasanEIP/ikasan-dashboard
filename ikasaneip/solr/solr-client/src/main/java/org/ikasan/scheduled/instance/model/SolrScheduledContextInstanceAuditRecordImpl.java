package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.UUID;

import static org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl.SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID;

public class SolrScheduledContextInstanceAuditRecordImpl extends SolrScheduledContextInstanceRecordImpl {

    public SolrScheduledContextInstanceAuditRecordImpl() {
        super.id = SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID + "_" + UUID.randomUUID();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
