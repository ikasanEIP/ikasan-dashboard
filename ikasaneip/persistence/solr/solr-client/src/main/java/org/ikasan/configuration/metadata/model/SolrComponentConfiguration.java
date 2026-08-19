package org.ikasan.configuration.metadata.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrComponentConfiguration
{
    private static Logger logger = LoggerFactory.getLogger(SolrComponentConfiguration.class);

    @Field(EntityFields.ID)
    private String configurationId;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String rawConfigurationMetadata;

    public String getConfigurationId()
    {
        return configurationId;
    }

    public String getRawConfigurationMetadata()
    {
        return rawConfigurationMetadata;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("SolrComponentConfiguration{");
        sb.append("configurationId='").append(configurationId).append('\'');
        sb.append(", rawConfigurationMetadata='").append(rawConfigurationMetadata).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
