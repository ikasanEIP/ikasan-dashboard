package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * MongoDB implementation of BusinessStreamMetaData.
 * Provides JSON serialization/deserialization of BusinessStreamImpl objects.
 */
public class BusinessStreamMetaDataImpl implements BusinessStreamMetaData<BusinessStreamImpl> {

    private static final JsonMapper mapper;

    static {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(Boundary.class, BoundaryImpl.class)
            .addAbstractTypeMapping(BusinessStream.class, BusinessStreamImpl.class)
            .addAbstractTypeMapping(Correlator.class, CorrelatorImpl.class)
            .addAbstractTypeMapping(Destination.class, DestinationImpl.class)
            .addAbstractTypeMapping(Edge.class, EdgeImpl.class)
            .addAbstractTypeMapping(Flow.class, Flowimpl.class)
            .addAbstractTypeMapping(IntegratedSystem.class, IntegratedSystemImpl.class);

        mapper = JsonMapper.builder().addModule(simpleModule).build();
    }

    private String id;
    private String name;
    private String description;
    private String json;
    private BusinessStreamImpl businessStream;

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
    public BusinessStreamImpl getBusinessStream() {
        if (this.businessStream == null && this.json != null) {
            try {
                this.businessStream = mapper.readValue(this.json, BusinessStreamImpl.class);
            } catch (JacksonException e) {
                throw new RuntimeException("Could not map business stream from JSON", e);
            }
        }

        return this.businessStream;
    }
}
