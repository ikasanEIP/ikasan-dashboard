package org.ikasan.relational.persistence.module.metadata.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.metadata.model.FlowMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hibernate/PostgreSQL implementation of ModuleMetaData.
 *
 * Uses PostgreSQL JSONB type to store the flows as JSON, providing efficient
 * storage and querying capabilities while maintaining full metadata structure.
 */
@Entity
@Table(name = "module_metadata")
public class HibernateModuleMetaDataImpl implements ModuleMetaData {

    private static final Logger logger = LoggerFactory.getLogger(HibernateModuleMetaDataImpl.class);
    private static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", length = 50)
    private ModuleType moduleType;

    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "context", length = 512)
    private String context;

    @Column(name = "protocol", length = 10)
    private String protocol;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "ikasan_version", length = 50)
    private String ikasanVersion;

    @Column(name = "configured_resource_id", length = 512)
    private String configuredResourceId;

    @Column(name = "flows", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String flowsJson;

    @Transient
    private List<FlowMetaData> flows;

    /**
     * Default constructor for JPA
     */
    public HibernateModuleMetaDataImpl() {
    }

    /**
     * Constructor with name
     */
    public HibernateModuleMetaDataImpl(String name) {
        this.name = name;
    }

    @PrePersist
    @PreUpdate
    protected void serializeFlows() {
        if (flows != null && !flows.isEmpty()) {
            try {
                this.flowsJson = objectMapper.writeValueAsString(flows);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize flows to JSON", e);
                throw new RuntimeException("Failed to serialize flows to JSON", e);
            }
        } else {
            this.flowsJson = null;
        }
    }

    @PostLoad
    protected void deserializeFlows() {
        if (flowsJson != null && !flowsJson.isEmpty()) {
            try {
                List<HibernateFlowMetaDataImpl> flowsList = objectMapper.readValue(flowsJson,
                    new TypeReference<List<HibernateFlowMetaDataImpl>>() {});
                this.flows = new ArrayList<>(flowsList);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize flows from JSON: {}", flowsJson, e);
                throw new RuntimeException("Failed to deserialize flows from JSON", e);
            }
        } else {
            this.flows = new ArrayList<>();
        }
    }

    public void setType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }

    public ModuleType getType() {
        return this.moduleType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public String getIkasanVersion() {
        return ikasanVersion;
    }

    public void setIkasanVersion(String ikasanVersion) {
        this.ikasanVersion = ikasanVersion;
    }

    public void setFlows(List<FlowMetaData> flows) {
        this.flows = flows;
    }

    public List<FlowMetaData> getFlows() {
        if (flows == null) {
            flows = new ArrayList<>();
        }
        return flows;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getConfiguredResourceId() {
        return this.configuredResourceId;
    }

    public void setConfiguredResourceId(String id) {
        this.configuredResourceId = id;
    }

    /**
     * Get the raw JSON string (for debugging or direct access)
     */
    public String getFlowsJson() {
        return flowsJson;
    }

    /**
     * Set the raw JSON string (mainly for testing or direct manipulation)
     */
    public void setFlowsJson(String flowsJson) {
        this.flowsJson = flowsJson;
        this.flows = null; // Force re-deserialization
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateModuleMetaDataImpl that = (HibernateModuleMetaDataImpl) o;
        return moduleType == that.moduleType &&
            Objects.equals(url, that.url) &&
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(version, that.version) &&
            Objects.equals(configuredResourceId, that.configuredResourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleType, url, name, description, version, configuredResourceId);
    }

    @Override
    public String toString() {
        return "HibernateModuleMetaDataImpl{" +
                "name='" + name + '\'' +
                ", moduleType=" + moduleType +
                ", url='" + url + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", context='" + context + '\'' +
                ", protocol='" + protocol + '\'' +
                ", description='" + description + '\'' +
                ", version='" + version + '\'' +
                ", ikasanVersion='" + ikasanVersion + '\'' +
                ", configuredResourceId='" + configuredResourceId + '\'' +
                ", flows=" + (flows != null ? flows.size() + " flows" : "null") +
                '}';
    }
}
