package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.Correlator;

/**
 * Represents a correlator in a business stream flow.
 * Used to define correlation logic for flows.
 */
public class CorrelatorImpl implements Correlator {
    private String type;
    private String query;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
