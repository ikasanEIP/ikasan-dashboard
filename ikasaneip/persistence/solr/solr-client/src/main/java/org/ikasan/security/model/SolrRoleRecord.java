package org.ikasan.security.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.solr.SolrDaoBase;

import java.util.List;

public class SolrRoleRecord {
    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.TYPE)
    private String type;

    @Field(SolrDaoBase.NAME)
    private String name;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String role;

    @Field(SolrDaoBase.ROLE_POLICY_RELATED_ENTITY_COLLECTION)
    private List<String> relatedPolicies;

    @Field(SolrDaoBase.ROLE_JOB_PLAN_RELATED_ENTITY_COLLECTION)
    private List<String> relatedJobPlans;

    @Field(SolrDaoBase.ROLE_MODULE_RELATED_ENTITY_COLLECTION)
    private List<String> relatedModules;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getRelatedPolicies() {
        return relatedPolicies;
    }

    public void setRelatedPolicies(List<String> relatedPolicies) {
        this.relatedPolicies = relatedPolicies;
    }

    public List<String> getRelatedJobPlans() {
        return relatedJobPlans;
    }

    public void setRelatedJobPlans(List<String> relatedJobPlans) {
        this.relatedJobPlans = relatedJobPlans;
    }

    public List<String> getRelatedModules() {
        return relatedModules;
    }

    public void setRelatedModules(List<String> relatedModules) {
        this.relatedModules = relatedModules;
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
