package org.ikasan.dashboard.visualisation.model;

import java.util.List;

public class Connection extends Item {
    private int stroke;
    private String color;
    private int outlineStroke;
    private String outlineColor;
    private String policy;
    private List<Vertex> vertex;
    private String router;
    private int radius;
    private ConnectionDetails source;
    private ConnectionDetails target;

    public int getStroke() {
        return stroke;
    }

    public void setStroke(int stroke) {
        this.stroke = stroke;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getOutlineStroke() {
        return outlineStroke;
    }

    public void setOutlineStroke(int outlineStroke) {
        this.outlineStroke = outlineStroke;
    }

    public String getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(String outlineColor) {
        this.outlineColor = outlineColor;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public List<Vertex> getVertex() {
        return vertex;
    }

    public void setVertex(List<Vertex> vertex) {
        this.vertex = vertex;
    }

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public ConnectionDetails getSource() {
        return source;
    }

    public void setSource(ConnectionDetails source) {
        this.source = source;
    }

    public ConnectionDetails getTarget() {
        return target;
    }

    public void setTarget(ConnectionDetails target) {
        this.target = target;
    }
}
