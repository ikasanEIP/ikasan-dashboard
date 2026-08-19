package org.ikasan.module.metadata.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrModule
{
    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String rawConfigurationMetadata;


    public String getId()
    {
        return this.id;
    }

    public String getModuleMetaData()
    {
        return this.rawConfigurationMetadata;
    }
}
