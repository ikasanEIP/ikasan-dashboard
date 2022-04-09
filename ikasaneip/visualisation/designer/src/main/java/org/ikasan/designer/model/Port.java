package org.ikasan.designer.model;

public class Port extends Item {
    private int width;
    private int height;
    private String bgColor;
    private String color;
    private int stroke;
    private long maxFanOut;
    private String name;
    private String semanticGroup;
    private String port;
    private String locator;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
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

    public long getMaxFanOut() {
        return maxFanOut;
    }

    public void setMaxFanOut(long maxFanOut) {
        this.maxFanOut = maxFanOut;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSemanticGroup() {
        return semanticGroup;
    }

    public void setSemanticGroup(String semanticGroup) {
        this.semanticGroup = semanticGroup;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

}
