package org.ikasan.designer.model;

import java.util.List;

public class Circle extends PositionedItem {
    private List<Port> ports;
    private String bgColor;
    private String color;
    private int stroke;
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

    public String getDasharray() {
        return dasharray;
    }

    public void setDasharray(String dasharray) {
        this.dasharray = dasharray;
    }
}
