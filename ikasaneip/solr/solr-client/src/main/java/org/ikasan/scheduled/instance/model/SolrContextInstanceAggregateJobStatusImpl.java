package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.Map;
import java.util.Objects;

public class SolrContextInstanceAggregateJobStatusImpl implements ContextInstanceAggregateJobStatus {
    private String contextInstanceId;
    private String contextInstanceName;
    private Map<String, Integer> statusCounts;

    /**
     * Constructor
     *
     * @param contextInstanceId
     * @param contextInstanceName
     * @param statusCounts
     */
    public SolrContextInstanceAggregateJobStatusImpl(String contextInstanceId, String contextInstanceName
        , Map<String, Integer> statusCounts) {
        this.contextInstanceId = contextInstanceId;
        this.contextInstanceName = contextInstanceName;
        this.statusCounts = statusCounts;
    }

    @Override
    public String getContextInstanceId() {
        return this.contextInstanceId;
    }

    @Override
    public String getContextInstanceName() {
        return this.contextInstanceName;
    }

    @Override
    public int getStatusCount(InstanceStatus instanceStatus) {
        if(!statusCounts.containsKey(instanceStatus.name())) {
            return 0;
        }

        return statusCounts.get(instanceStatus.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolrContextInstanceAggregateJobStatusImpl that = (SolrContextInstanceAggregateJobStatusImpl) o;
        return Objects.equals(contextInstanceId, that.contextInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextInstanceId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
