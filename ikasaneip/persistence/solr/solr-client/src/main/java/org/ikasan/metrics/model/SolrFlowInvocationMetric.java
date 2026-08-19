package org.ikasan.metrics.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrFlowInvocationMetric
{
    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String rawFlowInvocationMetric;


    public String getId()
    {
        return this.id;
    }

    public String getFlowInvocationMetric()
    {
        return this.rawFlowInvocationMetric;
    }
}
