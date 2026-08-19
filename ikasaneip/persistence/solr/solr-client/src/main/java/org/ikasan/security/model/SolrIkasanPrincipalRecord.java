package org.ikasan.security.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;

import java.util.List;

/**
 * Solr record for storing IkasanPrincipal data in the Solr index.
 *
 * @author Ikasan Development Team
 */
public class SolrIkasanPrincipalRecord {
    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.IKASAN_PRINCIPAL_TYPE)
    private String principalType;

    @Field(EntityFields.NAME)
    private String name;

    @Field(EntityFields.DESCRIPTION)
    private String description;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String principal;

    @Field(EntityFields.ROLES_RELATED_ENTITY_COLLECTION)
    private List<String> relatedRoleIdentifiers;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public List<String> getRelatedRoleIdentifiers() {
        return relatedRoleIdentifiers;
    }

    public void setRelatedRoleIdentifiers(List<String> relatedRoleIdentifiers) {
        this.relatedRoleIdentifiers = relatedRoleIdentifiers;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }
}
