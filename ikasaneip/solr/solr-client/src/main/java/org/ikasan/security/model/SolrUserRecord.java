package org.ikasan.security.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.solr.SolrDaoBase;

import java.util.List;

public class SolrUserRecord {

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.TYPE)
    private String type;

    @Field(SolrDaoBase.NAME)
    private String username;

    @Field(SolrDaoBase.EMAIL)
    private String email;

    @Field(SolrDaoBase.FIRST_NAME)
    private String firstName;

    @Field(SolrDaoBase.SURNAME)
    private String surname;

    @Field(SolrDaoBase.DEPARTMENT)
    private String department;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String user;

    @Field(SolrDaoBase.PRINCIPAL_RELATED_ENTITY_COLLECTION)
    private List<String> relatedPrincipalIdentifiers;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<String> getRelatedPrincipalIdentifiers() {
        return relatedPrincipalIdentifiers;
    }

    public void setRelatedPrincipalIdentifiers(List<String> relatedPrincipalIdentifiers) {
        this.relatedPrincipalIdentifiers = relatedPrincipalIdentifiers;
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
