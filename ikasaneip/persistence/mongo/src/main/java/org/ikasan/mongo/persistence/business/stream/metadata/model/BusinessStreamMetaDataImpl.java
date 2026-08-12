package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * MongoDB implementation of BusinessStreamMetaData.
 * Provides JSON serialization/deserialization of BusinessStream objects.
 */
public class BusinessStreamMetaDataImpl implements BusinessStreamMetaData<BusinessStream> {

    private static final JsonMapper mapper = JsonMapper.builder().build();

    private String id;
    private String name;
    private String description;
    private String json;
    private BusinessStream businessStream;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getJson() {
        return this.json;
    }

    @Override
    public void setJson(String json) {
        this.json = json;
    }

    @Override
    public BusinessStream getBusinessStream() {
        if (this.businessStream == null && this.json != null) {
            try {
                this.businessStream = mapper.readValue(this.json, BusinessStream.class);
            } catch (JacksonException e) {
                throw new RuntimeException("Could not map business stream from JSON", e);
            }
        }

        return this.businessStream;
    }
}
