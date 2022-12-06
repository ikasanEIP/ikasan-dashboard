package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.spec.scheduled.instance.model.JobLockInstance;

public class JobLockInstanceImpl extends JobLockImpl implements JobLockInstance {
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
