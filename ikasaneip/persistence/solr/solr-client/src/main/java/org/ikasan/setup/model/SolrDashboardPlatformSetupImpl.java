package org.ikasan.setup.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.setup.util.SolrSetupObjectMapperFactory;
import org.ikasan.spec.solr.SolrDaoBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Solr implementation of DashboardPlatformSetup.
 */
public class SolrDashboardPlatformSetupImpl implements DashboardPlatformSetup {
    private final ObjectMapper objectMapper = SolrSetupObjectMapperFactory.newInstance();

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

        try {
            return this.objectMapper.readValue(platformSetupItems, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPlatformSetupItems(List<DashboardSetupItem> dashboardSetupItems) {
        try {
            this.platformSetupItems = this.objectMapper.writeValueAsString(dashboardSetupItems);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
