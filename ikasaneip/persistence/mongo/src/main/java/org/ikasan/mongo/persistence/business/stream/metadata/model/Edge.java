package org.ikasan.mongo.persistence.business.stream.metadata.model;

/**
 * Represents an edge connecting two nodes in a business stream.
 * Defines the flow of data from one component to another.
 */
public class Edge {
    private String from;
    private String to;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
