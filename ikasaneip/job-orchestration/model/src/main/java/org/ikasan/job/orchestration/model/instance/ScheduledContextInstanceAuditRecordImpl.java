package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;

import java.util.UUID;

public class ScheduledContextInstanceAuditRecordImpl extends ScheduledContextInstanceRecordImpl {

    public ScheduledContextInstanceAuditRecordImpl() {
        super.id = ScheduledContextInstanceAuditDao.SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID + "_" + UUID.randomUUID();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
