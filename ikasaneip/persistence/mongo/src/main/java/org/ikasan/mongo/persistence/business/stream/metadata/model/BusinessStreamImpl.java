package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete business stream containing flows, destinations, systems, and their connections.
 * This is the root model object that gets serialized to/from JSON.
 */
public class BusinessStreamImpl implements BusinessStream {
    private List<Flowimpl> flows = new ArrayList<>();
    private List<DestinationImpl> destinations = new ArrayList<>();
    private List<IntegratedSystem> integratedSystems = new ArrayList<>();
    private List<EdgeImpl> edges = new ArrayList<>();
    private List<BoundaryImpl> boundaries = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public List<Flow> getFlows() {
        return (List<Flow>) (List<?>) flows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setFlows(List<Flow> flows) {
        this.flows = (List<Flowimpl>) (List<?>) flows;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Destination> getDestinations() {
        return (List<Destination>) (List<?>) destinations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setDestinations(List<Destination> destinations) {
        this.destinations = (List<DestinationImpl>) (List<?>) destinations;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IntegratedSystem> getIntegratedSystems() {
        return integratedSystems;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setIntegratedSystems(List<IntegratedSystem> integratedSystems) {
        this.integratedSystems = integratedSystems;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Edge> getEdges() {
        return (List<Edge>) (List<?>) edges;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setEdges(List<Edge> edges) {
        this.edges = (List<EdgeImpl>) (List<?>) edges;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Boundary> getBoundaries() {
        return (List<Boundary>) (List<?>) boundaries;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setBoundaries(List<Boundary> boundaries) {
        this.boundaries = (List<BoundaryImpl>) (List<?>) boundaries;
    }
}
