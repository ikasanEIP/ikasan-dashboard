package org.ikasan.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.*;

import java.util.ArrayList;
import java.util.List;

public class BusinessStreamImpl implements BusinessStream
{
    private List<Flow> flows = new ArrayList<>();
    private List<Destination> destinations = new ArrayList<>();
    private List<IntegratedSystem> integratedSystems = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private List<Boundary> boundaries = new ArrayList<>();

    @Override
    public List<Flow> getFlows() {
        return flows;
    }

    @Override
    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    @Override
    public List<Destination> getDestinations() {
        return destinations;
    }

    @Override
    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }

    @Override
    public List<IntegratedSystem> getIntegratedSystems() {
        return integratedSystems;
    }

    @Override
    public void setIntegratedSystems(List<IntegratedSystem> integratedSystems) {
        this.integratedSystems = integratedSystems;
    }

    @Override
    public List<Edge> getEdges() {
        return edges;
    }

    @Override
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public List<Boundary> getBoundaries() {
        return boundaries;
    }

    @Override
    public void setBoundaries(List<Boundary> boundaries) {
        this.boundaries = boundaries;
    }
}
