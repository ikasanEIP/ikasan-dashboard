package org.ikasan.dashboard.ui.visualisation.model.general;

import java.util.List;

public class Rectangle extends PositionedItem {
    private List<Port> ports;
    private String bgColor;
    private String color;
    private int stroke;
    private int radius;
    private String dasharray;

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getStroke() {
        return stroke;
    }

    public void setStroke(int stroke) {
        this.stroke = stroke;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getDasharray() {
        return dasharray;
    }

    public void setDasharray(String dasharray) {
        this.dasharray = dasharray;
    }
}
