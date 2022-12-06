package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.spec.scheduled.instance.model.JobLockInstance;

public class SolrJobLockInstanceImpl extends SolrJobLockImpl implements JobLockInstance {
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
