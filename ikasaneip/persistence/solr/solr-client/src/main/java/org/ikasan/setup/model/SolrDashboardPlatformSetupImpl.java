package org.ikasan.setup.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.setup.util.SolrSetupObjectMapperFactory;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Solr implementation of DashboardPlatformSetup.
 */
public class SolrDashboardPlatformSetupImpl implements DashboardPlatformSetup {
    private final JsonMapper objectMapper = SolrSetupObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.TYPE)
    private String type;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String platformSetupItems;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<DashboardSetupItem> getPlatformSetupItems() {
        if(this.platformSetupItems == null || this.platformSetupItems.isEmpty()) {
            return new ArrayList<>();
        }

        return this.objectMapper.readValue(platformSetupItems, new TypeReference<>() {});
    }

    @Override
    public void setPlatformSetupItems(List<DashboardSetupItem> dashboardSetupItems) {
        this.platformSetupItems = this.objectMapper.writeValueAsString(dashboardSetupItems);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String toString() {
        return "SolrDashboardPlatformSetupImpl{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", platformSetupItems=" + platformSetupItems +
            ", timestamp=" + timestamp +
            ", modifiedTimestamp=" + modifiedTimestamp +
            '}';
    }
}
